package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.CompetitionDTO;

import java.util.List;

public interface CompetitionService {

    List<CompetitionDTO> getAllCompetitions();

    CompetitionDTO getCompetitionById(Long id);

    CompetitionDTO createCompetition(CompetitionDTO dto);

    CompetitionDTO updateCompetition(Long id, CompetitionDTO dto);

    void deleteCompetition(Long id);
}
