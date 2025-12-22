package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.PhaseDTO;

import java.util.List;

public interface PhaseService {

    List<PhaseDTO> getAllPhases();

    List<PhaseDTO> getPhasesByCompetition(Long competitionId);

    PhaseDTO getPhaseById(Long id);

    PhaseDTO createPhase(PhaseDTO dto);

    PhaseDTO updatePhase(Long id, PhaseDTO dto);

    void deletePhase(Long id);
}
