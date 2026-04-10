package org.aitu.vulnerabilitiesmvp.config;

import jakarta.validation.constraints.DecimalMin;
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

    public Security getSecurity() {
        return security;
    }

    public Payments getPayments() {
        return payments;
    }

    public Fraud getFraud() {
        return fraud;
    }

    public static class Security {

        @NotNull
        private final Jwt jwt = new Jwt();

        @Min(1024)
        private long maxRequestSizeBytes = 16_384;

        public Jwt getJwt() {
            return jwt;
        }

        public long getMaxRequestSizeBytes() {
            return maxRequestSizeBytes;
        }

        public void setMaxRequestSizeBytes(long maxRequestSizeBytes) {
            this.maxRequestSizeBytes = maxRequestSizeBytes;
        }
    }

    public static class Jwt {

        @NotBlank
        private String secret;

        @Min(1)
        private long expirationMinutes = 15;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
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
        private int defaultPageSize = 20;

        @Min(1)
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
}
