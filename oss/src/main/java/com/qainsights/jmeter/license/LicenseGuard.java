package com.qainsights.jmeter.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Single point of truth for all JMeter Studio Pro license validation.
 *
 * <p>Validates a signed JWT license file offline using an RSA-256 public key
 * embedded on the classpath. The private key is never distributed — it stays
 * on the build/sales server.</p>
 *
 * <h3>License file discovery (first match wins):</h3>
 * <ol>
 *   <li>{@code JMETER_HOME/bin/jmeter-studio.license}</li>
 *   <li>{@code JMETER_HOME/jmeter-studio.license}</li>
 *   <li>{@code user.home/.jmeter-studio/jmeter-studio.license}</li>
 * </ol>
 *
 * <p>Thread-safe: validation runs once and the result is cached for the JVM session.</p>
 */
public final class LicenseGuard {

    private static final Logger logger = LoggerFactory.getLogger(LicenseGuard.class);

    private static final String PREFIX = "JMeter Studio";
    private static final String LICENSE_FILE_NAME = "jmeter-studio.license";
    private static final String PUBLIC_KEY_RESOURCE = "com/qainsights/jmeter/license/public.pem";
    private static final String EXPECTED_ISSUER = "qainsights.com";
    private static final String EXPECTED_PRODUCT = "jmeter-studio-pro";

    /** Grace period after expiry before the license is considered invalid. */
    private static final Duration EXPIRY_GRACE = Duration.ofDays(30);

    // ── Cached state (set once) ──────────────────────────────────────────────

    private static volatile boolean validated = false;
    private static volatile boolean proLicensed = false;
    private static volatile LicenseInfo cachedInfo = null;

    private LicenseGuard() {
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if a valid Pro license has been detected.
     * Safe to call from any thread; triggers validation on first call.
     */
    public static boolean isProLicensed() {
        ensureValidated();
        return proLicensed;
    }

    /**
     * Returns license details if a valid Pro license exists.
     */
    public static Optional<LicenseInfo> getLicenseInfo() {
        ensureValidated();
        return Optional.ofNullable(cachedInfo);
    }

    /**
     * Forces re-validation (e.g. after the user places a new license file).
     */
    public static synchronized void revalidate() {
        validated = false;
        proLicensed = false;
        cachedInfo = null;
        ensureValidated();
    }

    // ── Validation logic ─────────────────────────────────────────────────────

    private static void ensureValidated() {
        if (validated) {
            return;
        }
        synchronized (LicenseGuard.class) {
            if (validated) {
                return;
            }
            doValidate();
            validated = true;
        }
    }

    private static void doValidate() {
        String jwt = readLicenseFile();
        if (jwt == null) {
            logger.info("{} No license file found — running in free mode.", PREFIX);
            return;
        }

        RSAPublicKey publicKey = loadPublicKey();
        if (publicKey == null) {
            logger.warn("{} Public key not found on classpath — cannot validate license.", PREFIX);
            return;
        }

        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(EXPECTED_ISSUER)
                    .withClaim("product", EXPECTED_PRODUCT)
                    .acceptExpiresAt(EXPIRY_GRACE.getSeconds())
                    .build();

            DecodedJWT decoded = verifier.verify(jwt);

            // Check expiry with grace period
            Date expiresAtDate = decoded.getExpiresAt();
            Instant expiresAt = null;
            if (expiresAtDate != null) {
                expiresAt = expiresAtDate.toInstant();
                Instant gracedExpiry = expiresAt.plus(EXPIRY_GRACE);
                if (Instant.now().isAfter(gracedExpiry)) {
                    logger.warn("{} License expired on {} (grace period ended {}). Running in free mode.",
                            PREFIX, expiresAt, gracedExpiry);
                    return;
                }
                if (Instant.now().isAfter(expiresAt)) {
                    logger.info("{} License expired on {} but within grace period. Please renew soon.",
                            PREFIX, expiresAt);
                }
            }

            String email = decoded.getSubject();
            String edition = decoded.getClaim("edition").asString();
            String licenseId = decoded.getClaim("lid").asString();
            Instant issuedAt = decoded.getIssuedAt() != null
                    ? decoded.getIssuedAt().toInstant()
                    : Instant.now();

            if (email == null || edition == null || licenseId == null) {
                logger.warn("{} License JWT missing required claims (sub, edition, lid).", PREFIX);
                return;
            }

            cachedInfo = new LicenseInfo(email, edition, licenseId, issuedAt, expiresAt);
            proLicensed = true;
            logger.info("{} Pro license validated for {} (edition={}, expires={}).",
                    PREFIX, email, edition,
                    expiresAt != null ? expiresAt : "lifetime");

        } catch (JWTVerificationException e) {
            logger.warn("{} License validation failed: {}. Running in free mode.",
                    PREFIX, e.getMessage());
        }
    }

    // ── License file discovery ───────────────────────────────────────────────

    private static String readLicenseFile() {
        Path[] candidates = buildCandidatePaths();
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                try {
                    String content = Files.readString(candidate, StandardCharsets.UTF_8).trim();
                    if (!content.isEmpty()) {
                        logger.info("{} Found license file: {}", PREFIX, candidate);
                        return content;
                    }
                } catch (IOException e) {
                    logger.warn("{} Error reading license file {}: {}",
                            PREFIX, candidate, e.getMessage());
                }
            }
        }
        return null;
    }

    private static Path[] buildCandidatePaths() {
        String jmeterHome = System.getProperty("jmeter.home",
                System.getenv("JMETER_HOME"));

        Path userHome = Paths.get(System.getProperty("user.home"),
                ".jmeter-studio", LICENSE_FILE_NAME);

        if (jmeterHome != null && !jmeterHome.isEmpty()) {
            return new Path[]{
                    Paths.get(jmeterHome, "bin", LICENSE_FILE_NAME),
                    Paths.get(jmeterHome, LICENSE_FILE_NAME),
                    userHome
            };
        }
        return new Path[]{ userHome };
    }

    // ── Public key loading ───────────────────────────────────────────────────

    private static RSAPublicKey loadPublicKey() {
        try (InputStream is = LicenseGuard.class.getClassLoader()
                .getResourceAsStream(PUBLIC_KEY_RESOURCE)) {
            if (is == null) {
                return null;
            }
            String pem;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                pem = reader.lines().collect(Collectors.joining("\n"));
            }

            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
            logger.error("{} Failed to load public key: {}", PREFIX, e.getMessage());
            return null;
        }
    }
}
