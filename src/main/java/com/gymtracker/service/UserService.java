package com.gymtracker.service;

import com.gymtracker.dto.user.ChangePasswordRequestDTO;
import com.gymtracker.dto.user.UserProfileDTO;
import com.gymtracker.dto.user.UserProfileUpdateDTO;

/**
 * Service contract for the authenticated user's generic profile, account and
 * security settings, shared by every role (Athlete-specific biometric data
 * remains the responsibility of AthleteService).
 */
public interface UserService {

    UserProfileDTO getCurrentUserProfile();

    UserProfileDTO updateProfile(UserProfileUpdateDTO requestDTO);

    void changePassword(ChangePasswordRequestDTO requestDTO);
}
