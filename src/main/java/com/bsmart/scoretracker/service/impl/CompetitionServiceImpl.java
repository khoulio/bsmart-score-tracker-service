package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.dto.CompetitionDTO;
import com.bsmart.scoretracker.exception.ResourceNotFoundException;
import com.bsmart.scoretracker.model.Competition;
import com.bsmart.scoretracker.repository.CompetitionRepository;
import com.bsmart.scoretracker.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CompetitionDTO> getAllCompetitions() {
        return competitionRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompetitionDTO getCompetitionById(Long id) {
        Competition competition = competitionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Competition", id));
        return toDTO(competition);
    }

    @Override
    @Transactional
    public CompetitionDTO createCompetition(CompetitionDTO dto) {
        Competition competition = Competition.builder()
            .code(dto.getCode())
            .name(dto.getName())
            .country(dto.getCountry())
            .build();

        Competition saved = competitionRepository.save(competition);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public CompetitionDTO updateCompetition(Long id, CompetitionDTO dto) {
        Competition competition = competitionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Competition", id));

        competition.setCode(dto.getCode());
        competition.setName(dto.getName());
        competition.setCountry(dto.getCountry());

        Competition updated = competitionRepository.save(competition);
        return toDTO(updated);
    }

    @Override
    @Transactional
    public void deleteCompetition(Long id) {
        if (!competitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Competition", id);
        }
        competitionRepository.deleteById(id);
    }

    private CompetitionDTO toDTO(Competition competition) {
        return CompetitionDTO.builder()
            .id(competition.getId())
            .code(competition.getCode())
            .name(competition.getName())
            .country(competition.getCountry())
            .createdAt(competition.getCreatedAt())
            .updatedAt(competition.getUpdatedAt())
            // Sync fields
            .externalId(competition.getExternalId())
            .slug(competition.getSlug())
            .logoUrl(competition.getLogoUrl())
            .nbUsers(competition.getNbUsers())
            .dateStart(competition.getDateStart())
            .dateEnd(competition.getDateEnd())
            .isOpen(competition.getIsOpen())
            .isLeague(competition.getIsLeague())
            .isStarted(competition.getIsStarted())
            .isFeatured(competition.getIsFeatured())
            .backgroundUrl(competition.getBackgroundUrl())
            .sponsorLogoUrl(competition.getSponsorLogoUrl())
            .lastSyncAt(competition.getLastSyncAt())
            .build();
    }
}
