package com.zixin.gateway.config;

import com.zixin.authapi.api.TokenValidationAPI;
import dto.ValidateTokenRequest;
import dto.ValidateTokenResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import java.util.*;

import static com.zixin.utils.constant.HeaderConstant.*;

/**
 * 网关JWT认证和链路追踪全局过滤器
 *
 * 功能：
 * 1. 生成/传递 TraceId
 * 2. 验证JWT Token (通过Dubbo调用auth-server)
 * 3. 提取用户信息和权限
 * 4. 将用户信息注入到下游服务的请求头
 * 5. 白名单路径放行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceAndJwtAuthFilter implements GlobalFilter, Ordered {

    @DubboReference(check = false)
    private TokenValidationAPI tokenValidationAPI;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void init() {
        log.info("========== TraceAndJwtAuthFilter 已加载 ==========");
        log.info("白名单路径: {}", WHITE_LIST);
    }

    /**
     * 白名单路径 - 这些路径不需要JWT认证
     */
    private static final Set<String> WHITE_LIST = new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/validate",
            "/api/auth/sms/code", 
            "/actuator/**",
            "/api/actuator/**",
            "/v3/api-docs/**",
            "/api/v3/api-docs/**",
            "/swagger-ui/**",
            "/api/swagger-ui/**",
            "/error"
    ));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 1. 生成/获取 TraceId
        String traceId = generateOrGetTraceId(exchange);

        // 1.1 预检请求直接放行
        if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
            return continueWithTraceId(exchange, chain, traceId);
        }

        // 2. 检查是否是白名单路径
        String path = exchange.getRequest().getPath().value();
        if (isWhiteListPath(path)) {
            log.info("White list path: {}, skip JWT authentication", path);
            return continueWithTraceId(exchange, chain, traceId);
        }

        // 3. 提取JWT Token
        String token = extractToken(exchange);
        if (token == null) {
            log.warn("Missing JWT token for path: {}", path);
            return unauthorized(exchange, traceId, "Missing authentication token");
        }

        // 4. 通过Dubbo调用auth-server验证Token
        return validateTokenAsync(token)
                .flatMap(response -> {
                    if (response == null || !Boolean.TRUE.equals(response.getValid())) {
                        log.warn("JWT validation failed for path: {}", path);
                        return unauthorized(exchange, traceId, "Invalid or expired token");
                    }

                    Long userId = response.getUserId();
                    String username = response.getUsername();
                    List<String> roles = response.getRoles();
                    List<String> authorities = response.getAuthorities();

                    if (userId == null) {
                        log.warn("JWT validation response missing userId, path: {}", path);
                        return unauthorized(exchange, traceId, "Invalid token payload");
                    }

                    log.info("JWT validated successfully - userId: {}, username: {}, roles: {}, permission:{}",
                            userId, username, roles, authorities);

                    // 5. 将用户完整信息注入到下游请求头
                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                            .header(TRACE_ID, traceId)
                            .header(USER_ID, userId == null ? "" : String.valueOf(userId));

                    if (username != null) {
                        log.info("Injecting username into header: {}", username);
                        requestBuilder.header(USERNAME, username);
                    }
                    if (roles != null && !roles.isEmpty()) {
                        log.info("Injecting roles into header: {}", roles);
                        requestBuilder.header(USER_ROLES, String.join(",", roles));
                    }
                    if (authorities != null && !authorities.isEmpty()) {
                        log.info("Injecting authorities into header: {}", authorities);
                        requestBuilder.header(USER_AUTHORITIES, String.join(",", authorities));
                    }

                    ServerHttpRequest modifiedRequest = requestBuilder.build();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(modifiedRequest)
                            .build();

                    // 6. 构建包含所有用户信息的上下文
                    Context initialContext = Context.empty()
                            .put(TRACE_ID, traceId)
                            .put(USER_ID, userId)
                            .put(USERNAME, username != null ? username : "")
                            .put(USER_ROLES, roles != null ? roles : Collections.emptyList())
                            .put(USER_AUTHORITIES, authorities != null ? authorities : Collections.emptyList());

                    // 7. 在进入过滤链之前设置MDC
                    MDC.put(TRACE_ID, traceId);

                    return chain.filter(mutatedExchange)
                            .contextWrite(ctx -> ctx.putAll(initialContext))  // 合并所有上下文
                            .doFinally(signal -> {
                                // 清理MDC
                                MDC.remove(TRACE_ID);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error during JWT validation", e);
                    return unauthorized(exchange, traceId, "Authentication failed");
                });
    }

    /**
     * 白名单路径继续执行，只添加TraceId
     */
    private Mono<Void> continueWithTraceId(ServerWebExchange exchange, GatewayFilterChain chain, String traceId) {
        // 添加模拟用户信息
        ServerHttpRequest modifiedRequest = exchange.getRequest()
                .mutate()
                .header(TRACE_ID, traceId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        // 设置MDC用于日志
        MDC.put(TRACE_ID, traceId);

        // 创建包含traceId的上下文
        Context context = Context.empty().put(TRACE_ID, traceId);

        return chain.filter(mutatedExchange)
                .contextWrite(ctx -> ctx.putAll(context))
                .doFinally(signal -> MDC.remove(TRACE_ID));
    }

    /**
     * 生成或获取TraceId
     */
    private String generateOrGetTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteListPath(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 从请求头提取JWT Token
     */
    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 异步验证Token
     * 通过Dubbo调用auth-server的TokenValidationAPI
     */
    private Mono<ValidateTokenResponse> validateTokenAsync(String token) {
        return Mono.fromCallable(() -> {
                    ValidateTokenRequest request = new ValidateTokenRequest();
                    request.setToken(token);
                    ValidateTokenResponse response = tokenValidationAPI.validateToken(request);
                    if (response == null) {
                        ValidateTokenResponse fallback = new ValidateTokenResponse();
                        fallback.setValid(false);
                        fallback.setMessage("Empty token validation response");
                        return fallback;
                    }
                    return response;
                })
                // Dubbo 同步调用会阻塞，必须切到弹性线程池，避免阻塞 Netty 事件循环
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Failed to validate token via Dubbo", e);
                    ValidateTokenResponse fallback = new ValidateTokenResponse();
                    fallback.setValid(false);
                    fallback.setMessage("Dubbo token validation failed");
                    return Mono.just(fallback);
                });

    }

    /**
     * 返回401未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String traceId, String message) {
        log.warn("Unauthorized access: {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("trace-context","unauthorized");
        if (traceId != null && !traceId.isEmpty()) {
            exchange.getResponse().getHeaders().add(TRACE_ID, traceId);
        }
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        // 设置为最高优先级，确保在其他过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
