package com.helper.gateway.filter;

import com.helper.gateway.utils.JwtUtils;
import com.helper.gateway.utils.RedisServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter {

    @Autowired
    private RedisServices redisUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取令牌token
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String refreshToken = exchange.getRequest().getHeaders().getFirst("RefreshToken");

        // token为空或者不是Bearer 开头直接返回
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        // 获取校验token
        String token = authHeader.substring(7);
        String version = JwtUtils.getVersion(token);
        String userId = JwtUtils.getUserId(token);

        // 判断该账号redis中是否存在 且登录版本是否是当前token的版本 不是就返回未认证
        return redisUtil.hGet("user:" + userId, "version")
                .map(obj -> obj == null ? null : obj.toString()) // 转成 Mono<String>
                .flatMap(loginVersion -> {
                    // 没有登录信息或登录版本不对
                    if (loginVersion == null || !loginVersion.equals(version)) {
                        return unauthorized(exchange);
                    }
                    // 校验是否有效 无效就刷新token  每隔一个刷新token周期需要登录一次的效果
                    if (!JwtUtils.validateToken(token)) {
                        // token无效
                        if (!JwtUtils.validateToken(refreshToken)  ){
                            // 长token也无效
                            return unauthorized(exchange);
                        }else {
                            // 刷新token
                            String newToken = JwtUtils.generateToken(userId, version);
                            exchange.getResponse().getHeaders().add("Authorization", "Bearer " + newToken);
                        }
                    }

                    // userId 放到请求里面 放行
                    ServerWebExchange serverWebExchange = mutateRequestWithUserId(exchange, userId);
                    return chain.filter(serverWebExchange);
                })
                // 任何异常也统一返回 401，防止抛出导致 500
                .onErrorResume(err -> {
                    log.error("鉴权异常", err);
                    return unauthorized(exchange);
                });
    }

    // 请求中添加用户ID
    private ServerWebExchange mutateRequestWithUserId(ServerWebExchange exchange, String userId) {
        // 构造一个包含 X-User-Id 的新的 Exchange（不破坏原始 exchange）
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .build();
        return exchange.mutate().request(mutatedRequest).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
}