package com.tns.tms.domain.user;

import com.tns.tms.domain.audit.AuditLogService;
import com.tns.tms.domain.manager.ManagerAssignment;
import com.tns.tms.domain.manager.ManagerAssignmentRepository;
import com.tns.tms.domain.notification.NotificationService;
import com.tns.tms.shared.exception.ResourceNotFoundException;
import com.tns.tms.shared.exception.ValidationException;
import com.tns.tms.shared.util.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                        ManagerAssignmentRepository managerAssignmentRepository,
                        PasswordEncoder passwordEncoder,
                        AuditLogService auditLogService,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public User createUser(Long actorId, String fullName, String email, Set<Role> roles,
                            Long managerId, String department, String employeeId) {
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email already in use: " + email);
        }
        if (employeeId != null && userRepository.existsByEmployeeId(employeeId)) {
            throw new ValidationException("Employee ID already in use: " + employeeId);
        }

        String tempPassword = generateTempPassword();
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .roles(roles)
                .department(department)
                .employeeId(employeeId)
                .forcePasswordChange(true)
                .build();

        User saved = userRepository.save(user);

        // Assign manager if provided
        if (managerId != null) {
            User manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + managerId));
            ManagerAssignment assignment = ManagerAssignment.builder()
                    .employee(saved).manager(manager).build();
            managerAssignmentRepository.save(assignment);
        }

        auditLogService.log(actorId, "USER_CREATED", "USER", saved.getId(), null,
                "email=" + email + ",roles=" + roles);

        notificationService.sendWelcomeEmail(saved, tempPassword);
        log.info("User created: {} by admin {}", saved.getId(), actorId);
        return saved;
    }

    @Transactional
    public User updateUser(Long actorId, Long userId, String fullName, String email,
                            String department, String employeeId, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String before = "fullName=" + user.getFullName() + ",email=" + user.getEmail() + ",department=" + user.getDepartment();
        user.setFullName(fullName);
        if (email != null && !email.isBlank()) user.setEmail(email);
        user.setDepartment(department);
        if (employeeId != null) user.setEmployeeId(employeeId);
        if (roles != null && !roles.isEmpty()) user.setRoles(roles);

        User saved = userRepository.save(user);
        auditLogService.log(actorId, "USER_UPDATED", "USER", userId, before,
                "fullName=" + fullName + ",email=" + email + ",department=" + department);
        return saved;
    }

    @Transactional
    public void deactivateUser(Long actorId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ValidationException("User is already inactive.");
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        auditLogService.log(actorId, "USER_DEACTIVATED", "USER", userId, "ACTIVE", "INACTIVE");
        log.info("User {} deactivated by admin {}", userId, actorId);
    }

    @Transactional
    public void reactivateUser(Long actorId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ValidationException("User is already active.");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        auditLogService.log(actorId, "USER_REACTIVATED", "USER", userId, "INACTIVE", "ACTIVE");
    }

    @Transactional
    public void resetPassword(Long actorId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setForcePasswordChange(true);
        userRepository.save(user);

        notificationService.sendWelcomeEmail(user, tempPassword);
        auditLogService.log(actorId, "PASSWORD_RESET_BY_ADMIN", "USER", userId, null, null);
    }

    private String generateTempPassword() {
        byte[] bytes = new byte[12];
        SECURE_RANDOM.nextBytes(bytes);
        String base = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // Ensure complexity: add uppercase, digit, special char
        return "Tms@" + base.substring(0, 8) + "1";
    }
}
