package com.zixin.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceEnhanceFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest()
                .getHeaders()
                .getFirst(TRACE_ID);

        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        String finalTraceId = traceId;
        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.header(TRACE_ID, finalTraceId))
                .build();

        String finalTraceId1 = traceId;
        return chain.filter(mutated)
                .contextWrite(ctx -> ctx.put(TRACE_ID, finalTraceId1));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
