package com.gymtracker.service;

import com.gymtracker.dto.mesocycle.MesocycleDetailDTO;
import com.gymtracker.dto.mesocycle.MesocycleRequestDTO;
import com.gymtracker.dto.mesocycle.MesocycleResponseDTO;
import com.gymtracker.dto.mesocycle.MesocycleSummaryDTO;
import java.util.List;

/**
 * Service contract for mesocycle management operations.
 */
public interface MesocycleService {

    MesocycleResponseDTO createMesocycle(MesocycleRequestDTO requestDTO);

    MesocycleResponseDTO updateMesocycle(String mesocycleId, MesocycleRequestDTO requestDTO);

    MesocycleResponseDTO activateMesocycle(String mesocycleId);

    MesocycleResponseDTO archiveMesocycle(String mesocycleId);

    MesocycleDetailDTO getMesocycleById(String mesocycleId);

    MesocycleResponseDTO getActiveMesocycle(String athleteId);

    List<MesocycleSummaryDTO> getMesocyclesByAthlete(String athleteId);

    List<MesocycleSummaryDTO> getMesocyclesByCoach(String coachId);

    List<MesocycleSummaryDTO> searchMesocycles(String keyword);

    MesocycleResponseDTO duplicateMesocycle(String mesocycleId);

    /**
     * Returns the mesocycles visible to the authenticated caller: an Athlete
     * sees their own, a Coach sees the ones they created, and a Nutritionist
     * sees the full catalog (read-only).
     */
    List<MesocycleSummaryDTO> getMesocyclesForCurrentUser();
}
