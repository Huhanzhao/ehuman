package com.helper.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // 禁用 CSRF（REST API 一般不需要）
                .csrf(csrf -> csrf.disable())
                // 配置路由权限
                .authorizeExchange(exchanges -> exchanges  // 白名单 全局鉴权
//                        .pathMatchers(
//                                "/user/api/user/login",
//                                "/doc.html",
//                                "/swagger-ui/**",
//                                "/v3/api-docs/**",
//                                "/v3/api-docs",
//                                "/*/v3/api-docs",
//                                "/webjars/**"
//                        ).permitAll()  // 不需要认证
                        .anyExchange().permitAll()           // 除了 /public/** 以外的所有接口都必须认证
                )
                // 禁用默认登录方式
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .build();
    }
}