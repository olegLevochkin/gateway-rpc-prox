package com.github.olegLevochkin.gateway.metrics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MetricsNames {
    public static final String JSONRPC_CALLS_TOTAL = "jsonrpc_calls_total";
    public static final String TAG_METHOD = "method";
}
