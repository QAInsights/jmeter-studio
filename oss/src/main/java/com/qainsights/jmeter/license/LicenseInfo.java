package com.qainsights.jmeter.license;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable data carrier for a validated JMeter Studio Pro license.
 *
 * <p>Instances are created exclusively by {@link LicenseGuard} after successful
 * JWT verification. A {@code null} {@code expiresAt} indicates a lifetime license.</p>
 */
public final class LicenseInfo {

    private final String email;
    private final String edition;
    private final String licenseId;
    private final Instant issuedAt;
    private final Instant expiresAt; // null → lifetime

    public LicenseInfo(String email, String edition, String licenseId,
                       Instant issuedAt, Instant expiresAt) {
        this.email = Objects.requireNonNull(email, "email");
        this.edition = Objects.requireNonNull(edition, "edition");
        this.licenseId = Objects.requireNonNull(licenseId, "licenseId");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = expiresAt; // nullable — lifetime license
    }

    public String getEmail() {
        return email;
    }

    public String getEdition() {
        return edition;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    /**
     * Returns the expiry instant, or empty for lifetime licenses.
     */
    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    /**
     * Returns {@code true} if this is a lifetime (non-expiring) license.
     */
    public boolean isLifetime() {
        return expiresAt == null;
    }

    @Override
    public String toString() {
        return "LicenseInfo{email='" + email + "', edition='" + edition
                + "', licenseId='" + licenseId + "', expiresAt="
                + (expiresAt != null ? expiresAt : "lifetime") + '}';
    }
}
