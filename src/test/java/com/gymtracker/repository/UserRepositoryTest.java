package com.gymtracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymtracker.entity.User;
import com.gymtracker.enums.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

/**
 * Repository test verifying User persistence and derived query methods
 * against a real MongoDB instance (test database).
 */
@DataMongoTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    private User buildUser(String email, Role role) {
        return User.builder()
                .role(role)
                .email(email)
                .password("encoded-password")
                .firstName("Jane")
                .lastName("Doe")
                .age(25)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void savesAndFindsUserById() {
        User saved = userRepository.save(buildUser("jane.doe@example.com", Role.ATHLETE));

        var found = userRepository.findById(saved.getId().toHexString());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void findsUserByEmail() {
        userRepository.save(buildUser("coach@example.com", Role.COACH));

        var found = userRepository.findByEmail("coach@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.COACH);
    }

    @Test
    void findByEmailReturnsEmptyWhenNotFound() {
        var found = userRepository.findByEmail("missing@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmailReflectsPersistedState() {
        userRepository.save(buildUser("existing@example.com", Role.NUTRITIONIST));

        assertThat(userRepository.existsByEmail("existing@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    void findsUsersByRole() {
        userRepository.save(buildUser("athlete1@example.com", Role.ATHLETE));
        userRepository.save(buildUser("athlete2@example.com", Role.ATHLETE));
        userRepository.save(buildUser("coach1@example.com", Role.COACH));

        var athletes = userRepository.findByRole(Role.ATHLETE);

        assertThat(athletes).hasSize(2)
                .allMatch(user -> user.getRole() == Role.ATHLETE);
    }
}
