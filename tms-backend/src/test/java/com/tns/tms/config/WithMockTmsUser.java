package com.tns.tms.config;

import com.tns.tms.domain.user.Role;
import com.tns.tms.domain.user.User;
import com.tns.tms.domain.user.UserStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.stream.Collectors;

/**
 * Injects a real TMS User as the Spring Security principal in MockMvc tests.
 *
 * Usage: .with(WithMockTmsUser.as(user))
 *
 * Uses SecurityMockMvcRequestPostProcessors.authentication() which correctly
 * sets the Authentication in the SecurityContext for the request, making
 * @AuthenticationPrincipal resolve to the User object.
 */
public final class WithMockTmsUser {

    private WithMockTmsUser() {}

    public static RequestPostProcessor as(User user) {
        var authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .collect(Collectors.toList());

        var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    public static User employee(Long id) {
        return TestSecurityConfig.mockUser(id, Role.EMPLOYEE);
    }

    public static User manager(Long id) {
        return TestSecurityConfig.mockUser(id, Role.MANAGER);
    }

    public static User hr(Long id) {
        return TestSecurityConfig.mockUser(id, Role.HR);
    }

    public static User admin(Long id) {
        return TestSecurityConfig.mockUser(id, Role.ADMIN);
    }
}
