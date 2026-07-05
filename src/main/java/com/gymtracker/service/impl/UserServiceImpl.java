package com.gymtracker.service.impl;

import com.gymtracker.dto.user.ChangePasswordRequestDTO;
import com.gymtracker.dto.user.UserProfileDTO;
import com.gymtracker.dto.user.UserProfileUpdateDTO;
import com.gymtracker.entity.User;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.mapper.UserMapper;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.service.UserService;
import com.gymtracker.validation.UserValidator;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business implementation for the authenticated user's generic profile,
 * account and security settings.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserValidator userValidator;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            UserValidator userValidator,
            PasswordEncoder passwordEncoder,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userValidator = userValidator;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Override
    public UserProfileDTO getCurrentUserProfile() {
        return userMapper.toProfileDTO(getAuthenticatedUser());
    }

    @Override
    public UserProfileDTO updateProfile(UserProfileUpdateDTO requestDTO) {
        userValidator.validateProfileUpdate(requestDTO);
        User currentUser = getAuthenticatedUser();

        userMapper.updateEntityFromRequest(requestDTO, currentUser);
        currentUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(currentUser);

        LOGGER.info("Profile updated for userId={}", savedUser.getId());
        return userMapper.toProfileDTO(savedUser);
    }

    @Override
    public void changePassword(ChangePasswordRequestDTO requestDTO) {
        userValidator.validatePasswordChange(requestDTO);
        User currentUser = getAuthenticatedUser();

        if (!passwordEncoder.matches(requestDTO.getCurrentPassword(), currentUser.getPassword())) {
            LOGGER.warn("Incorrect current password on change attempt for userId={}", currentUser.getId());
            throw new UnauthorizedOperationException("Current password is incorrect.");
        }

        currentUser.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        currentUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(currentUser);
        LOGGER.info("Password changed for userId={}", currentUser.getId());
    }

    private User getAuthenticatedUser() {
        return authenticatedUserProvider.getAuthenticatedUser();
    }
}
