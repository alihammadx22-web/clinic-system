package com.dentalclinic.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctor-schedules", "/doctor-schedules/**").hasAnyRole("RECEPTION", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/doctor-schedules").hasRole("RECEPTION")
                        .requestMatchers(HttpMethod.PUT, "/doctor-schedules/**").hasRole("RECEPTION")
                        .requestMatchers(HttpMethod.DELETE, "/doctor-schedules/**").hasRole("RECEPTION")
                        .requestMatchers(HttpMethod.GET, "/appointments", "/appointments/**").hasAnyRole("RECEPTION", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/appointments").hasRole("RECEPTION")
                        .requestMatchers(HttpMethod.PUT, "/appointments/*/status").hasAnyRole("RECEPTION", "DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/appointments/*/reschedule").hasRole("RECEPTION")
                        .requestMatchers(HttpMethod.DELETE, "/appointments/**").hasRole("RECEPTION")
                        .requestMatchers(HttpMethod.GET, "/dental-cases", "/dental-cases/**").hasAnyRole("RECEPTION", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/dental-cases").hasAnyRole("RECEPTION", "DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/dental-cases/**").hasAnyRole("RECEPTION", "DOCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/dental-cases/**").hasRole("RECEPTION")
                        .requestMatchers(HttpMethod.GET, "/users", "/users/**").hasAnyRole("RECEPTION", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/users").hasRole("RECEPTION")
                        .requestMatchers(HttpMethod.PUT, "/users/**").hasRole("RECEPTION")
                        .requestMatchers("/users/**").hasRole("RECEPTION")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(jwtSecretKey(secret))
                .algorithm(JWSAlgorithm.HS256)
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk)));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private SecretKeySpec jwtSecretKey(String secret) {
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "HmacSHA256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        scopes.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return scopes.convert(jwt);
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}
