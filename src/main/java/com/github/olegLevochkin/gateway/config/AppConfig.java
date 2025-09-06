package com.github.olegLevochkin.gateway.config;

import java.util.Objects;

public record AppConfig(
        int httpPort,
        boolean tlsEnabled,
        String pkcs12Path,
        String pkcs12Password,
        String targetRpcUrl,
        int requestTimeoutMs,
        int maxBodyBytes,
        boolean circuitBreakerEnabled,
        int circuitBreakerFailuresThreshold,
        int circuitBreakerResetTimeoutMs
) {
    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65_535;

    public AppConfig {
        requireInRange(httpPort);
        requirePositive(requestTimeoutMs, "requestTimeoutMs");
        requirePositive(maxBodyBytes, "maxBodyBytes");
        requireNonBlank(targetRpcUrl, "targetRpcUrl");

        if (tlsEnabled) {
            requireNonBlank(pkcs12Path, "pkcs12Path");
            requireNonBlank(pkcs12Password, "pkcs12Password");
        }

        requireAtLeast(circuitBreakerFailuresThreshold, 1, "circuitBreakerFailuresThreshold");
        requireAtLeast(circuitBreakerResetTimeoutMs, 0, "circuitBreakerResetTimeoutMs");
    }

    private static void requireInRange(int value) {
        if (value < AppConfig.PORT_MIN || value > AppConfig.PORT_MAX) {
            throw new IllegalArgumentException("httpPort" + " must be in range " + AppConfig.PORT_MIN + ".." + AppConfig.PORT_MAX);
        }
    }

    private static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
    }

    private static void requireAtLeast(int value, int min, String field) {
        if (value < min) {
            throw new IllegalArgumentException(field + " must be >= " + min);
        }
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
