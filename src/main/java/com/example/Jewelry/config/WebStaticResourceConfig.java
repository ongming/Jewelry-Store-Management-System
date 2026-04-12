package com.example.Jewelry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebStaticResourceConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor;

    public WebStaticResourceConfig(RoleInterceptor roleInterceptor) {
        this.roleInterceptor = roleInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get("uploads").toAbsolutePath();
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadPath + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor)
            .addPathPatterns(
                "/dashboard/**",
                "/products/manage/**",
                "/products/categories/**",
                "/products/staff",
                "/orders/**",
                "/customers/**",
                "/pos/**",
                "/auth/accounts/**",
                "/admin/**",
                "/staff/**"
            );
    }
}