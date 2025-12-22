package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.dto.PhaseDTO;
import com.bsmart.scoretracker.exception.ResourceNotFoundException;
import com.bsmart.scoretracker.model.Competition;
import com.bsmart.scoretracker.model.Phase;
import com.bsmart.scoretracker.repository.CompetitionRepository;
import com.bsmart.scoretracker.repository.PhaseRepository;
import com.bsmart.scoretracker.service.PhaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhaseServiceImpl implements PhaseService {

    private final PhaseRepository phaseRepository;
    private final CompetitionRepository competitionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PhaseDTO> getAllPhases() {
        return phaseRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhaseDTO> getPhasesByCompetition(Long competitionId) {
        return phaseRepository.findByCompetitionIdOrderByStartDateDesc(competitionId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PhaseDTO getPhaseById(Long id) {
        Phase phase = phaseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Phase", id));
        return toDTO(phase);
    }

    @Override
    @Transactional
    public PhaseDTO createPhase(PhaseDTO dto) {
        Competition competition = competitionRepository.findById(dto.getCompetitionId())
            .orElseThrow(() -> new ResourceNotFoundException("Competition", dto.getCompetitionId()));

        Phase phase = Phase.builder()
            .competition(competition)
            .name(dto.getName())
            .stage(dto.getStage())
            .matchDay(dto.getMatchDay())
            .startDate(dto.getStartDate())
            .endDate(dto.getEndDate())
            .trackingEnabled(dto.getTrackingEnabled() != null ? dto.getTrackingEnabled() : true)
            .build();

        Phase saved = phaseRepository.save(phase);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public PhaseDTO updatePhase(Long id, PhaseDTO dto) {
        Phase phase = phaseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Phase", id));

        if (dto.getCompetitionId() != null && !dto.getCompetitionId().equals(phase.getCompetition().getId())) {
            Competition competition = competitionRepository.findById(dto.getCompetitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Competition", dto.getCompetitionId()));
            phase.setCompetition(competition);
        }

        phase.setName(dto.getName());
        phase.setStage(dto.getStage());
        phase.setMatchDay(dto.getMatchDay());
        phase.setStartDate(dto.getStartDate());
        phase.setEndDate(dto.getEndDate());
        phase.setTrackingEnabled(dto.getTrackingEnabled());

        Phase updated = phaseRepository.save(phase);
        return toDTO(updated);
    }

    @Override
    @Transactional
    public void deletePhase(Long id) {
        if (!phaseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Phase", id);
        }
        phaseRepository.deleteById(id);
    }

    private PhaseDTO toDTO(Phase phase) {
        return PhaseDTO.builder()
            .id(phase.getId())
            .competitionId(phase.getCompetition().getId())
            .competitionName(phase.getCompetition().getName())
            .name(phase.getName())
            .stage(phase.getStage())
            .matchDay(phase.getMatchDay())
            .startDate(phase.getStartDate())
            .endDate(phase.getEndDate())
            .trackingEnabled(phase.getTrackingEnabled())
            .createdAt(phase.getCreatedAt())
            .updatedAt(phase.getUpdatedAt())
            // Sync fields
            .externalId(phase.getExternalId())
            .isProlongationEnabled(phase.getIsProlongationEnabled())
            .isMonetized(phase.getIsMonetized())
            .isFull(phase.getIsFull())
            .isNextPhaseCreated(phase.getIsNextPhaseCreated())
            .isStarted(phase.getIsStarted())
            .multiply(phase.getMultiply())
            .lastSyncAt(phase.getLastSyncAt())
            .build();
    }
}
