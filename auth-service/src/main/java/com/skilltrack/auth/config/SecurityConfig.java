package com.skilltrack.auth.config;

import com.skilltrack.auth.security.JwtAuthenticationEntryPoint;
import com.skilltrack.auth.security.JwtAuthenticationFilter;
import com.skilltrack.auth.security.JwtAccessDeniedHandler;
import com.skilltrack.auth.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF is disabled because this API uses JWT Bearer tokens (no server-side session).
                .csrf(csrf -> csrf.disable())
            // CORS is required so the Angular UI can call APIs from a different origin.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Stateless session policy ensures Spring Security never creates an HTTP session.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/auth/register",
                                "/api/auth/auth/login",
                                "/api/auth/v3/api-docs/**",
                                "/api/auth/swagger-ui.html",
                                "/api/auth/swagger-ui/**",
                                "/api/auth/actuator/health",
                                "/api/auth/actuator/info"
                        ).permitAll()
                        .anyRequest().authenticated()
                );
        // The JWT filter must run BEFORE UsernamePasswordAuthenticationFilter so that
        // requests bearing Authorization: Bearer <token> are authenticated early.
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength 12 is a common production baseline (balance between security and latency).
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        // AuthenticationManager uses CustomUserDetailsService + BCrypt for username/password login.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }
}
