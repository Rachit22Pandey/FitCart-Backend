
package com.fitcart.backend.service;

import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        List<UserDetails> users = jdbcTemplate.query(
            """
            SELECT email, password, role
            FROM users
            WHERE email = ?
            """,
            (rs, rowNum) -> {
                String storedPassword = rs.getString("password");
                String role = rs.getString("role")
                    .trim()
                    .toUpperCase(Locale.ROOT);

                if (storedPassword == null || storedPassword.isBlank()) {
                    throw new IllegalStateException(
                        "The account has no password configured."
                    );
                }

                return User.withUsername(rs.getString("email"))
                    .password(storedPassword)
                    .roles(role)
                    .build();
            },
            email.trim()
        );

        if (users.isEmpty()) {
            throw new UsernameNotFoundException(
                "No account found for this email."
            );
        }

        return users.get(0);
    }
}
