package org.aitu.vulnerabilitiesmvp.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotNull
    private final Security security = new Security();

    @NotNull
    private final Payments payments = new Payments();

    @NotNull
    private final Fraud fraud = new Fraud();

    @NotNull
    private final Exports exports = new Exports();

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Returning the live security holder preserves Spring binding semantics and avoids stale copies."
    )
    public Security getSecurity() {
        return security;
    }

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Returning the live payments holder preserves Spring binding semantics and avoids stale copies."
    )
    public Payments getPayments() {
        return payments;
    }

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Returning the live fraud holder preserves Spring binding semantics and avoids stale copies."
    )
    public Fraud getFraud() {
        return fraud;
    }

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Returning the live exports holder preserves Spring binding semantics and avoids stale copies."
    )
    public Exports getExports() {
        return exports;
    }

    public static class Security {

        @NotNull
        private final Jwt jwt = new Jwt();

        @Min(1024)
        private long maxRequestSizeBytes = 16_384;

        @Min(1024)
        private long maxFileSizeBytes = 16_384;

        @Min(64)
        private int maxJsonStringLength = 4_096;

        @Min(8)
        private int maxJsonNestingDepth = 40;

        @Min(8)
        private int maxJsonNumberLength = 64;

        @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Nested JWT settings are a live configuration holder managed inside the application "
                + "context. Returning a copy would disconnect validation/binding semantics."
        )
        public Jwt getJwt() {
            return jwt;
        }

        public long getMaxRequestSizeBytes() {
            return maxRequestSizeBytes;
        }

        public void setMaxRequestSizeBytes(long maxRequestSizeBytes) {
            this.maxRequestSizeBytes = maxRequestSizeBytes;
        }

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public int getMaxJsonStringLength() {
            return maxJsonStringLength;
        }

        public void setMaxJsonStringLength(int maxJsonStringLength) {
            this.maxJsonStringLength = maxJsonStringLength;
        }

        public int getMaxJsonNestingDepth() {
            return maxJsonNestingDepth;
        }

        public void setMaxJsonNestingDepth(int maxJsonNestingDepth) {
            this.maxJsonNestingDepth = maxJsonNestingDepth;
        }

        public int getMaxJsonNumberLength() {
            return maxJsonNumberLength;
        }

        public void setMaxJsonNumberLength(int maxJsonNumberLength) {
            this.maxJsonNumberLength = maxJsonNumberLength;
        }
    }

    public static class Jwt {

        @NotBlank
        private String secret;

        @NotBlank
        private String issuer = "vulnerabilities-mvp";

        @Min(1)
        private long expirationMinutes = 15;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }

    public static class Payments {

        @Min(1)
        @Max(100)
        private int defaultPageSize = 20;

        @Min(1)
        @Max(100)
        private int maxPageSize = 100;

        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }
    }

    public static class Fraud {

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal largeAmountThreshold = new BigDecimal("5000.00");

        @Min(1)
        private int frequencyWindowMinutes = 10;

        @Min(1)
        private int frequencyThreshold = 3;

        public BigDecimal getLargeAmountThreshold() {
            return largeAmountThreshold;
        }

        public void setLargeAmountThreshold(BigDecimal largeAmountThreshold) {
            this.largeAmountThreshold = largeAmountThreshold;
        }

        public int getFrequencyWindowMinutes() {
            return frequencyWindowMinutes;
        }

        public void setFrequencyWindowMinutes(int frequencyWindowMinutes) {
            this.frequencyWindowMinutes = frequencyWindowMinutes;
        }

        public int getFrequencyThreshold() {
            return frequencyThreshold;
        }

        public void setFrequencyThreshold(int frequencyThreshold) {
            this.frequencyThreshold = frequencyThreshold;
        }
    }

    public static class Exports {

        @NotBlank
        private String baseDirectory;

        @Min(1)
        @Max(100)
        private int maxRows = 100;

        public String getBaseDirectory() {
            return baseDirectory;
        }

        public void setBaseDirectory(String baseDirectory) {
            this.baseDirectory = baseDirectory;
        }

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }
    }
}
