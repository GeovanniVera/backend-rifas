package com.semillatecnologica.backend.security.config;

import com.semillatecnologica.backend.security.authentication.RestAccessDeniedHandler;
import com.semillatecnologica.backend.security.authentication.RestAuthenticationEntryPoint;
import com.semillatecnologica.backend.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración principal de Spring Security.
 *
 * <p>Define la cadena de filtros, endpoints públicos vs protegidos,
 * manejo de errores y política de sesiones (stateless con JWT).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfig corsConfig;

    /**
     * Endpoints que no requieren autenticación.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
        "/auth/login",
        "/auth/register",
        "/auth/forgot-password",
        "/auth/verify-otp",
        "/auth/reset-password",
        "/auth/resend-verification",
        "/auth/verify-email",
        "/auth/refresh",
        "/files/**",
        "/products",
        "/products/**",
        "/raffles",
        "/raffles/**",
        "/docs/**",
        "/docs/swagger/**",
        "/actuator/health",
        "/actuator/info"
    };

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     *
     * @return SecurityFilterChain configurada
     * @throws Exception si hay error en la configuración
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> {
                exception.authenticationEntryPoint(authenticationEntryPoint);
                exception.accessDeniedHandler(accessDeniedHandler);
            })
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configura CORS con las propiedades definidas.
     *
     * @return CorsConfigurationSource con la configuración de CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsConfig.getAllowedOrigins());
        configuration.setAllowedMethods(corsConfig.getAllowedMethods());
        configuration.setAllowedHeaders(corsConfig.getAllowedHeaders());
        configuration.setExposedHeaders(corsConfig.getExposedHeaders());
        configuration.setAllowCredentials(corsConfig.isAllowCredentials());
        configuration.setMaxAge((long) corsConfig.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Expone el AuthenticationManager para uso en AuthService.
     *
     * @param config Configuración de autenticación
     * @return AuthenticationManager configurado
     * @throws Exception si hay error en la configuración
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Bean para hash de contraseñas con Argon2.
     *
     * @return PasswordEncoder configurado
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

}
