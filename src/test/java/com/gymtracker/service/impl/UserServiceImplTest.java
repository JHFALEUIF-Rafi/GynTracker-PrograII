package com.gymtracker.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymtracker.dto.user.ChangePasswordRequestDTO;
import com.gymtracker.dto.user.UserProfileDTO;
import com.gymtracker.dto.user.UserProfileUpdateDTO;
import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import com.gymtracker.exception.UnauthorizedOperationException;
import com.gymtracker.exception.ValidationException;
import com.gymtracker.mapper.ObjectIdMapperImpl;
import com.gymtracker.mapper.UserMapper;
import com.gymtracker.mapper.UserMapperImpl;
import com.gymtracker.repository.UserRepository;
import com.gymtracker.security.AuthenticatedUserProvider;
import com.gymtracker.validation.UserValidator;
import jakarta.validation.Validation;
import java.time.LocalDateTime;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for UserServiceImpl, using the real UserMapper (generated) and
 * UserValidator implementations against a mocked repository, matching this
 * project's convention of testing services against real collaborating
 * mappers/validators rather than re-mocking every layer.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private final UserMapper userMapper = new UserMapperImpl();
    private final UserValidator userValidator = new UserValidator(Validation.buildDefaultValidatorFactory().getValidator());
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userMapper, "objectIdMapper", new ObjectIdMapperImpl());
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider(userRepository);
        userService = new UserServiceImpl(userRepository, userMapper, userValidator, passwordEncoder, authenticatedUserProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser() {
        return User.builder()
                .id(new ObjectId())
                .role(Role.ATHLETE)
                .email("athlete@example.com")
                .password(passwordEncoder.encode("current-password"))
                .firstName("Jane")
                .lastName("Doe")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void authenticateAs(User user) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(user.getEmail(), null);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void getCurrentUserProfileReturnsMappedProfile() {
        User user = buildUser();
        authenticateAs(user);

        UserProfileDTO profile = userService.getCurrentUserProfile();

        assertThat(profile.getEmail()).isEqualTo("athlete@example.com");
        assertThat(profile.getRole()).isEqualTo(Role.ATHLETE);
        assertThat(profile.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void getCurrentUserProfileThrowsWhenNotAuthenticated() {
        assertThatThrownBy(() -> userService.getCurrentUserProfile())
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void updateProfileUpdatesFirstAndLastName() {
        User user = buildUser();
        authenticateAs(user);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateDTO requestDTO = UserProfileUpdateDTO.builder()
                .firstName("Updated")
                .lastName("Name")
                .build();

        UserProfileDTO result = userService.updateProfile(requestDTO);

        assertThat(result.getFirstName()).isEqualTo("Updated");
        assertThat(result.getLastName()).isEqualTo("Name");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileRejectsBlankFirstName() {
        User user = buildUser();
        authenticateAs(user);

        UserProfileUpdateDTO requestDTO = UserProfileUpdateDTO.builder()
                .firstName("")
                .lastName("Name")
                .build();

        assertThatThrownBy(() -> userService.updateProfile(requestDTO))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void changePasswordSucceedsWithCorrectCurrentPassword() {
        User user = buildUser();
        authenticateAs(user);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChangePasswordRequestDTO requestDTO = ChangePasswordRequestDTO.builder()
                .currentPassword("current-password")
                .newPassword("brand-new-password")
                .confirmNewPassword("brand-new-password")
                .build();

        userService.changePassword(requestDTO);

        verify(userRepository).save(user);
        assertThat(passwordEncoder.matches("brand-new-password", user.getPassword())).isTrue();
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        User user = buildUser();
        authenticateAs(user);

        ChangePasswordRequestDTO requestDTO = ChangePasswordRequestDTO.builder()
                .currentPassword("wrong-password")
                .newPassword("brand-new-password")
                .confirmNewPassword("brand-new-password")
                .build();

        assertThatThrownBy(() -> userService.changePassword(requestDTO))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void changePasswordRejectsMismatchedConfirmation() {
        User user = buildUser();
        authenticateAs(user);

        ChangePasswordRequestDTO requestDTO = ChangePasswordRequestDTO.builder()
                .currentPassword("current-password")
                .newPassword("brand-new-password")
                .confirmNewPassword("different-password")
                .build();

        assertThatThrownBy(() -> userService.changePassword(requestDTO))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void changePasswordRejectsSameAsCurrentPassword() {
        User user = buildUser();
        authenticateAs(user);

        ChangePasswordRequestDTO requestDTO = ChangePasswordRequestDTO.builder()
                .currentPassword("current-password")
                .newPassword("current-password")
                .confirmNewPassword("current-password")
                .build();

        assertThatThrownBy(() -> userService.changePassword(requestDTO))
                .isInstanceOf(ValidationException.class);
    }
}
