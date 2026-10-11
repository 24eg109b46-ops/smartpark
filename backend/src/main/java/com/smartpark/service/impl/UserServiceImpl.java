package com.smartpark.service.impl;

import com.smartpark.dto.PasswordChangeRequest;
import com.smartpark.dto.ProfileUpdateRequest;
import com.smartpark.dto.RegisterRequest;
import com.smartpark.entity.User;
import com.smartpark.enums.Role;
import com.smartpark.enums.UserStatus;
import com.smartpark.exception.BadRequestException;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.repository.UserRepository;
import com.smartpark.service.AuditLogService;
import com.smartpark.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserServiceImpl(UserRepository userRepository,
                           @Lazy PasswordEncoder passwordEncoder,
                           AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    public User registerDriver(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email address is already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_DRIVER);
        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        auditLogService.log(saved.getId(), saved.getEmail(), "REGISTER", "New driver registered: " + saved.getEmail(), null);
        return saved;
    }

    @Override
    public User createOperator(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email address is already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_OPERATOR);
        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        auditLogService.log(saved.getId(), saved.getEmail(), "ADMIN_CREATE_OPERATOR", "Operator account created: " + saved.getEmail(), null);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Override
    public User updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = findById(userId);
        user.setName(request.getName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        User saved = userRepository.save(user);
        auditLogService.log(user.getId(), user.getEmail(), "PROFILE_UPDATE", "User updated profile details", null);
        return saved;
    }

    @Override
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = findById(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match our records");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.log(user.getId(), user.getEmail(), "PASSWORD_CHANGE", "User changed account password", null);
    }

    @Override
    public User toggleUserStatus(Long userId) {
        User user = findById(userId);
        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new BadRequestException("Administrator account status cannot be modified");
        }
        user.setStatus(user.getStatus() == UserStatus.ACTIVE ? UserStatus.BLOCKED : UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        auditLogService.log(saved.getId(), saved.getEmail(), "USER_STATUS_CHANGE",
                "User status changed to " + saved.getStatus(), null);
        return saved;
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.log(user.getId(), user.getEmail(), "PASSWORD_RESET", "Admin reset user password", null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDrivers() {
        return userRepository.countByRole(Role.ROLE_DRIVER);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOperators() {
        return userRepository.countByRole(Role.ROLE_OPERATOR);
    }
}
