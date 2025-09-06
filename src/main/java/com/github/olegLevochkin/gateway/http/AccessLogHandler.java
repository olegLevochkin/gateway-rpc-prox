package com.github.olegLevochkin.gateway.http;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AccessLogHandler implements Handler<RoutingContext> {

    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String UNKNOWN = "-";

    @Override
    public void handle(RoutingContext context) {
        final long startedAtNanos = System.nanoTime();

        context.addBodyEndHandler(v -> {
            long durationMillis = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLI;

            String userAgent = getHeader(context);
            String clientIp = resolveClientIp(context);
            int status = context.response().getStatusCode();
            long bytesWritten = context.response().bytesWritten();

            log.info("{} {} {} {}ms bytes={} ua={} ip={}",
                    context.request().method(),
                    context.request().path(),
                    status,
                    durationMillis,
                    bytesWritten,
                    userAgent,
                    clientIp);
        });

        context.next();
    }

    private static String getHeader(RoutingContext context) {
        String value = context.request().headers().get(HttpHeaders.USER_AGENT);
        return value != null && !value.isBlank() ? value : UNKNOWN;
    }

    private static String resolveClientIp(RoutingContext context) {
        String forwarded = context.request().headers().get(HEADER_X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return context.request().remoteAddress() != null
                ? context.request().remoteAddress().host()
                : UNKNOWN;
    }
}
