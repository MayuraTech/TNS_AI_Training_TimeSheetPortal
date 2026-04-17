package com.tns.tms.config;

import com.tns.tms.domain.auth.JwtAuthFilter;
import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserRepository;
import com.tns.tms.domain.user.UserStatus;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Set;

/**
 * Disables security for @WebMvcTest slices so controller tests focus on HTTP behaviour only.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** Convenience factory for a mock authenticated user. */
    public static User mockUser(Long id, Role... roles) {
        return User.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .fullName("Test User " + id)
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(roles))
                .build();
    }
}
