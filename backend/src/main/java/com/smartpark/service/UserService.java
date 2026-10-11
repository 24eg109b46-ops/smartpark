package com.smartpark.service;

import com.smartpark.dto.PasswordChangeRequest;
import com.smartpark.dto.ProfileUpdateRequest;
import com.smartpark.dto.RegisterRequest;
import com.smartpark.entity.User;
import com.smartpark.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    User registerDriver(RegisterRequest request);

    User createOperator(RegisterRequest request);

    User findByEmail(String email);

    User findById(Long id);

    User updateProfile(Long userId, ProfileUpdateRequest request);

    void changePassword(Long userId, PasswordChangeRequest request);

    User toggleUserStatus(Long userId);

    void resetPassword(Long userId, String newPassword);

    Page<User> getAllUsers(Pageable pageable);

    Page<User> getUsersByRole(Role role, Pageable pageable);

    long countTotalUsers();

    long countDrivers();

    long countOperators();
}
