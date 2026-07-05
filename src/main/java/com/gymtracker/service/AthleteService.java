package com.gymtracker.service;

import com.gymtracker.dto.athlete.AthleteDetailDTO;
import com.gymtracker.dto.athlete.AthleteRequestDTO;
import com.gymtracker.dto.athlete.AthleteResponseDTO;
import com.gymtracker.dto.athlete.AthleteSummaryDTO;
import java.util.List;

/**
 * Service contract for athlete profile operations.
 */
public interface AthleteService {

    AthleteDetailDTO getAthleteById(String athleteId);

    AthleteDetailDTO getCurrentAthlete();

    AthleteResponseDTO updateAthleteProfile(AthleteRequestDTO requestDTO);

    List<AthleteSummaryDTO> getAllAthletes();

    List<AthleteSummaryDTO> searchAthletes(String keyword);

    boolean existsByEmail(String email);

    /**
     * Returns the athletes visible to the authenticated caller: a Coach sees
     * athletes with at least one mesocycle assigned to them, a Nutritionist
     * sees athletes with at least one nutrition plan assigned to them.
     * Athletes may not call this operation.
     */
    List<AthleteSummaryDTO> getAthletesForCurrentUser();
}
