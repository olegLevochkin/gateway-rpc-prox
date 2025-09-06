package com.github.olegLevochkin.gateway.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.util.Properties;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigLoader {

    private static final String RESOURCE_FILE = "application.properties";

    private static final class Keys {
        static final String HTTP_PORT = "http.port";
        static final String TLS_ENABLED = "tls.enabled";
        static final String TLS_PKCS12_PATH = "tls.pkcs12.path";
        static final String TLS_PKCS12_PASSWORD = "tls.pkcs12.password";
        static final String TARGET_RPC_URL = "target.rpc.url";
        static final String REQUEST_TIMEOUT_MS = "request.timeout.ms";
        static final String MAX_BODY_BYTES = "max.body.bytes";
        static final String CB_ENABLED = "circuit.breaker.enabled";
        static final String CB_FAILURES_THRESHOLD = "circuit.breaker.failures.threshold";
        static final String CB_RESET_TIMEOUT_MS = "circuit.breaker.reset.timeout.ms";

        private Keys() {
        }
    }

    private static final class Defaults {
        static final int HTTP_PORT = 8443;
        static final boolean TLS_ENABLED = true;
        static final String TLS_PKCS12_PATH = "/server.p12";
        static final String TLS_PKCS12_PASSWORD = "changeit";
        static final String TARGET_RPC_URL = "https://cloudflare-eth.com";
        static final int REQUEST_TIMEOUT_MS = 10_000;
        static final int MAX_BODY_BYTES = 10_485_760; // 10 MiB
        static final boolean CB_ENABLED = true;
        static final int CB_FAILURES_THRESHOLD = 5;
        static final int CB_RESET_TIMEOUT_MS = 30_000;

        private Defaults() {
        }
    }

    private record PropertyEntry<T>(String key, T defaultValue, Function<String, T> parser) {
    }

    private static <T> PropertyEntry<T> entry(String key, T defaultValue, Function<String, T> parser) {
        return new PropertyEntry<>(key, defaultValue, parser);
    }

    private static final PropertyEntry<Integer> HTTP_PORT =
            entry(Keys.HTTP_PORT, Defaults.HTTP_PORT, Integer::parseInt);
    private static final PropertyEntry<Boolean> TLS_ENABLED =
            entry(Keys.TLS_ENABLED, Defaults.TLS_ENABLED, Boolean::parseBoolean);
    private static final PropertyEntry<String> TLS_PKCS12_PATH =
            entry(Keys.TLS_PKCS12_PATH, Defaults.TLS_PKCS12_PATH, String::trim);
    private static final PropertyEntry<String> TLS_PKCS12_PASSWORD =
            entry(Keys.TLS_PKCS12_PASSWORD, Defaults.TLS_PKCS12_PASSWORD, String::trim);
    private static final PropertyEntry<String> TARGET_RPC_URL =
            entry(Keys.TARGET_RPC_URL, Defaults.TARGET_RPC_URL, String::trim);
    private static final PropertyEntry<Integer> REQUEST_TIMEOUT_MS =
            entry(Keys.REQUEST_TIMEOUT_MS, Defaults.REQUEST_TIMEOUT_MS, Integer::parseInt);
    private static final PropertyEntry<Integer> MAX_BODY_BYTES =
            entry(Keys.MAX_BODY_BYTES, Defaults.MAX_BODY_BYTES, Integer::parseInt);
    private static final PropertyEntry<Boolean> CB_ENABLED =
            entry(Keys.CB_ENABLED, Defaults.CB_ENABLED, Boolean::parseBoolean);
    private static final PropertyEntry<Integer> CB_FAILURES_THRESHOLD =
            entry(Keys.CB_FAILURES_THRESHOLD, Defaults.CB_FAILURES_THRESHOLD, Integer::parseInt);
    private static final PropertyEntry<Integer> CB_RESET_TIMEOUT_MS =
            entry(Keys.CB_RESET_TIMEOUT_MS, Defaults.CB_RESET_TIMEOUT_MS, Integer::parseInt);

    public static AppConfig load() {
        Properties properties = loadProperties();

        return new AppConfig(
                get(properties, HTTP_PORT),
                get(properties, TLS_ENABLED),
                get(properties, TLS_PKCS12_PATH),
                get(properties, TLS_PKCS12_PASSWORD),
                get(properties, TARGET_RPC_URL),
                get(properties, REQUEST_TIMEOUT_MS),
                get(properties, MAX_BODY_BYTES),
                get(properties, CB_ENABLED),
                get(properties, CB_FAILURES_THRESHOLD),
                get(properties, CB_RESET_TIMEOUT_MS)
        );
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(RESOURCE_FILE)) {
            if (in != null) {
                properties.load(in);
            }
            return properties;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load " + RESOURCE_FILE, e);
        }
    }

    private static <T> T get(Properties properties, PropertyEntry<T> entry) {
        String raw = properties.getProperty(entry.key());
        if (raw == null || raw.isBlank()) {
            return entry.defaultValue();
        }
        try {
            return entry.parser().apply(raw.trim());
        } catch (Exception ignored) {
            return entry.defaultValue();
        }
    }
}
