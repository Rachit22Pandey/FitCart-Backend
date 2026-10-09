
package com.fitcart.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse()
                )
            )
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/",
                    "/login.html",
                    "/style.css",
                    "/app.js",
                    "/favicon.ico",
                    "/csrf"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/auth/login",
                    "/auth/register"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/products",
                    "/products/**",
                    "/categories",
                    "/categories/**",
                    "/inventory/product/**"
                ).permitAll()

                .requestMatchers("/users", "/users/**")
                    .hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.POST,
                    "/categories",
                    "/categories/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/categories/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/categories/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.POST,
                    "/products"
                ).hasAnyRole("SELLER", "ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/products/**"
                ).hasAnyRole("SELLER", "ADMIN")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/products/**"
                ).hasAnyRole("SELLER", "ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/inventory/**"
                ).hasAnyRole("SELLER", "ADMIN")

                .requestMatchers(
                    "/cart/**",
                    "/wishlist/**",
                    "/addresses/**"
                ).hasAnyRole("BUYER", "ADMIN")

                .requestMatchers(
                    HttpMethod.POST,
                    "/orders"
                ).hasAnyRole("BUYER", "ADMIN")

                .requestMatchers(
                    HttpMethod.GET,
                    "/orders/user/**"
                ).hasAnyRole("BUYER", "ADMIN")

                .requestMatchers(
                    HttpMethod.GET,
                    "/orders/seller/**"
                ).hasAnyRole("SELLER", "ADMIN")

                .requestMatchers(
                    HttpMethod.GET,
                    "/orders"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/orders/*/status"
                ).hasAnyRole("SELLER", "ADMIN")

                .requestMatchers("/orders/**").authenticated()

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login.html?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login.html?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
