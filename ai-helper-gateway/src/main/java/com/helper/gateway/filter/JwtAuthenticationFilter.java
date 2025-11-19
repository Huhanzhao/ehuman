package com.helper.gateway.filter;

import com.helper.common.entity.BusinessException;
import com.helper.common.utils.RedisUtil;
import com.helper.gateway.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取令牌token
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String refreshToken = exchange.getRequest().getHeaders().getFirst("RefreshToken");

        // token为空或者不是Bearer 开头直接返回
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("无效token，请重新登录");
        }

        // 获取校验token
        String token = authHeader.substring(7);
        String version = JwtUtils.getVersion(token);
        String userId = JwtUtils.getUserId(token);

        // 判断该账号redis中是否存在 且登录版本是否是当前token的版本 不是就返回未认证
        String loginVersion = (String) redisUtil.hGet("user:" + userId, "version");
        if (!version.equals(loginVersion)){
            throw new BusinessException("您的账号已在其他设备登录，请重新登录");
        }

        // 校验是否有效 无效就刷新token
        if (!JwtUtils.validateToken(token)) {
            // token无效
            if (!JwtUtils.validateToken(refreshToken)  ){
                // 长token也无效
                throw new BusinessException("登录过期，请重新登录");
            }else {
                // 刷新token
                String newToken = JwtUtils.generateToken(userId, version);
                exchange.getResponse().getHeaders().add("Authorization", "Bearer " + newToken);
            }
        }

        // 请求中放入用户ID 方便后续处理
        exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .build();

        return chain.filter(exchange);
    }
}