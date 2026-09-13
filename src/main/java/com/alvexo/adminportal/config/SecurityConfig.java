package com.alvexo.adminportal.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Temporary: real JWT admin auth (see application.properties jwt.* config)
 * has not been wired up yet. Until then, disable Spring Security's default
 * auto-generated-password login so endpoints are reachable during
 * development. Replace this permitAll chain with real authentication /
 * role-based authorization before any endpoint is exposed outside dev.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Spring Security ignores CORS entirely unless a security filter chain
    // explicitly enables it — without this, the dashboard-frontend dev
    // server (a different origin) gets its requests blocked by the browser
    // even though every endpoint below is permitAll.
    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:4300}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
