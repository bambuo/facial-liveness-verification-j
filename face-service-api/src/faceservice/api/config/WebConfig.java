package faceservice.api.config;

import faceservice.api.security.JwtAuthFilter;
import faceservice.api.security.TenantLookup;
import faceservice.application.token.TokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    /**
     * JWT 过滤器只覆盖 /v1/*；/dev/*、/actuator/* 不参与端上鉴权。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilter(TokenService tokens, TenantLookup tenants) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(new JwtAuthFilter(tokens, tenants));
        reg.addUrlPatterns("/v1/*");
        reg.setOrder(10);
        return reg;
    }
}
