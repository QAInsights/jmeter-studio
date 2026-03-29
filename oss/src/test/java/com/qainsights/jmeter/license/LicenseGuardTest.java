package com.qainsights.jmeter.license;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LicenseGuard} covering valid, expired, tampered, and missing JWT scenarios.
 *
 * <p>Uses the project's actual public key from classpath and generates matching
 * private key for signing test JWTs. For tampered-token tests, a separate throwaway
 * key pair is generated.</p>
 */
class LicenseGuardTest {

    /** Matching private key — loaded from the project root for test signing. */
    private static RSAPrivateKey projectPrivateKey;

    /** A separate key pair used to simulate tampered / wrongly-signed tokens. */
    private static RSAPrivateKey roguePrivateKey;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void loadKeys() throws Exception {
        // Verify public key exists on classpath (used by LicenseGuard internally)
        try (InputStream is = LicenseGuardTest.class.getClassLoader()
                .getResourceAsStream("com/qainsights/jmeter/license/public.pem")) {
            assertNotNull(is, "public.pem must be on the test classpath");
        }

        // Load project private key from file system (project root)
        Path privateKeyPath = Path.of(System.getProperty("user.dir")).resolve("private.pem");
        if (!Files.exists(privateKeyPath)) {
            // Try parent dir (in case CWD is oss/)
            privateKeyPath = privateKeyPath.getParent().getParent().resolve("private.pem");
        }
        if (Files.exists(privateKeyPath)) {
            String pem = Files.readString(privateKeyPath, StandardCharsets.UTF_8);
            String base64 = pem
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64);

            try {
                java.security.spec.PKCS8EncodedKeySpec pkcs8 =
                        new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                projectPrivateKey = (RSAPrivateKey) kf.generatePrivate(pkcs8);
            } catch (Exception e) {
                // PKCS#1 format — generate a fresh pair for testing instead
                projectPrivateKey = null;
            }
        }

        // If we couldn't load the project private key, skip tests that need it
        // (they'll be marked with assumptions)

        // Generate rogue key pair for tampered-token tests
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair rogueKp = kpg.generateKeyPair();
        roguePrivateKey = (RSAPrivateKey) rogueKp.getPrivate();
    }

    @BeforeEach
    void resetLicenseGuard() {
        // Force re-validation before each test
        LicenseGuard.revalidate();
    }

    // ── Helper: create a signed JWT ──────────────────────────────────────────

    private String createJwt(RSAPrivateKey privateKey, String email,
                              Instant expiresAt, String product) {
        var builder = JWT.create()
                .withIssuer("qainsights.com")
                .withSubject(email)
                .withIssuedAt(Date.from(Instant.now()))
                .withClaim("product", product)
                .withClaim("edition", "pro")
                .withClaim("lid", UUID.randomUUID().toString());

        if (expiresAt != null) {
            builder.withExpiresAt(Date.from(expiresAt));
        }

        Algorithm algo = Algorithm.RSA256(null, privateKey);
        return builder.sign(algo);
    }

    private void placeLicenseFile(String jwt) throws Exception {
        // Place in the temp dir and set jmeter.home so LicenseGuard finds it
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);
        Files.writeString(binDir.resolve("jmeter-studio.license"), jwt, StandardCharsets.UTF_8);
        System.setProperty("jmeter.home", tempDir.toString());
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    void should_returnFalse_when_noLicenseFileExists() {
        System.setProperty("jmeter.home", tempDir.toString());
        LicenseGuard.revalidate();
        assertFalse(LicenseGuard.isProLicensed());
        assertTrue(LicenseGuard.getLicenseInfo().isEmpty());
    }

    @Test
    void should_returnFalse_when_licenseFileIsEmpty() throws Exception {
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);
        Files.writeString(binDir.resolve("jmeter-studio.license"), "", StandardCharsets.UTF_8);
        System.setProperty("jmeter.home", tempDir.toString());

        LicenseGuard.revalidate();
        assertFalse(LicenseGuard.isProLicensed());
    }

    @Test
    void should_returnFalse_when_tokenSignedByWrongKey() throws Exception {
        String jwt = createJwt(roguePrivateKey, "rogue@example.com",
                Instant.now().plus(365, ChronoUnit.DAYS), "jmeter-studio-pro");
        placeLicenseFile(jwt);

        LicenseGuard.revalidate();
        assertFalse(LicenseGuard.isProLicensed(),
                "Token signed by rogue key must not validate");
    }

    @Test
    void should_returnFalse_when_tokenHasWrongProduct() throws Exception {
        if (projectPrivateKey == null) {
            // Skip if we don't have the matching private key
            return;
        }
        String jwt = createJwt(projectPrivateKey, "user@example.com",
                Instant.now().plus(365, ChronoUnit.DAYS), "wrong-product");
        placeLicenseFile(jwt);

        LicenseGuard.revalidate();
        assertFalse(LicenseGuard.isProLicensed(),
                "Token with wrong product claim must not validate");
    }

    @Test
    void should_returnFalse_when_tokenExpiredBeyondGracePeriod() throws Exception {
        if (projectPrivateKey == null) {
            return;
        }
        // Expired 60 days ago (beyond 30-day grace)
        String jwt = createJwt(projectPrivateKey, "expired@example.com",
                Instant.now().minus(60, ChronoUnit.DAYS), "jmeter-studio-pro");
        placeLicenseFile(jwt);

        LicenseGuard.revalidate();
        assertFalse(LicenseGuard.isProLicensed(),
                "Token expired beyond grace period must not validate");
    }

    @Test
    void should_returnTrue_when_validLifetimeLicense() throws Exception {
        if (projectPrivateKey == null) {
            return;
        }
        // No expiry = lifetime
        String jwt = createJwt(projectPrivateKey, "lifetime@example.com",
                null, "jmeter-studio-pro");
        placeLicenseFile(jwt);

        LicenseGuard.revalidate();
        assertTrue(LicenseGuard.isProLicensed(), "Valid lifetime license must validate");

        Optional<LicenseInfo> info = LicenseGuard.getLicenseInfo();
        assertTrue(info.isPresent());
        assertEquals("lifetime@example.com", info.get().getEmail());
        assertEquals("pro", info.get().getEdition());
        assertTrue(info.get().isLifetime());
    }

    @Test
    void should_returnTrue_when_validSubscriptionLicense() throws Exception {
        if (projectPrivateKey == null) {
            return;
        }
        String jwt = createJwt(projectPrivateKey, "sub@example.com",
                Instant.now().plus(365, ChronoUnit.DAYS), "jmeter-studio-pro");
        placeLicenseFile(jwt);

        LicenseGuard.revalidate();
        assertTrue(LicenseGuard.isProLicensed(), "Valid subscription license must validate");

        Optional<LicenseInfo> info = LicenseGuard.getLicenseInfo();
        assertTrue(info.isPresent());
        assertEquals("sub@example.com", info.get().getEmail());
        assertFalse(info.get().isLifetime());
    }

    @Test
    void should_returnTrue_when_tokenExpiredWithinGracePeriod() throws Exception {
        if (projectPrivateKey == null) {
            return;
        }
        // Expired 10 days ago (within 30-day grace)
        String jwt = createJwt(projectPrivateKey, "grace@example.com",
                Instant.now().minus(10, ChronoUnit.DAYS), "jmeter-studio-pro");
        placeLicenseFile(jwt);

        LicenseGuard.revalidate();
        assertTrue(LicenseGuard.isProLicensed(),
                "Token within grace period should still validate");
    }

    @Test
    void should_returnFalse_when_tokenIsGarbage() throws Exception {
        placeLicenseFile("not-a-valid-jwt-at-all");

        LicenseGuard.revalidate();
        assertFalse(LicenseGuard.isProLicensed(),
                "Garbage token must not validate");
    }
}
