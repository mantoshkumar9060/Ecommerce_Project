package com.ecommerce.gateway.config;

import com.ecommerce.gateway.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        return http

                .csrf(csrf -> csrf.disable())

                .cors(cors -> {})

                .exceptionHandling(exception -> exception

                        // No valid authentication -> 401
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Authentication required"
                                        )
                        )

                        // Authenticated but insufficient permission -> 403
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "Access denied"
                                        )
                        )
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // ==============================
                        // PUBLIC AUTH ENDPOINTS
                        // ==============================

                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login"
                        ).permitAll()


                        // ==============================
                        // PUBLIC ACTUATOR ENDPOINTS
                        // ==============================

                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()


                        // ==============================
                        // AI SERVICE
                        // ==============================

                        // Only health is public
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/ai/health"
                        ).permitAll()

                        // Product ingestion - ADMIN only
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/ai/ingest/products"
                        ).hasRole("ADMIN")

                        // Ingestion status - ADMIN only
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/ai/ingest/status"
                        ).hasRole("ADMIN")

                        // Remaining AI APIs require valid JWT
                        .requestMatchers(
                                "/api/v1/ai/**"
                        ).authenticated()


                        // ==============================
                        // PRODUCT SERVICE
                        // ==============================

                        // Public product read APIs
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/products/**"
                        ).permitAll()

                        // Product management - ADMIN only
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/products/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/products/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/products/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/products/**"
                        ).hasRole("ADMIN")


                        // ==============================
                        // ORDER SERVICE
                        // ==============================

                        // Order status management - ADMIN only
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/orders/*/status"
                        ).hasRole("ADMIN")


                        // ==============================
                        // EVERYTHING ELSE
                        // ==============================

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }


    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:4200")
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}