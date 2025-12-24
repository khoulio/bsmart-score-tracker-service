package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.MatchDTO;
import com.bsmart.scoretracker.dto.external.WecanpronoMatchDTO;
import com.bsmart.scoretracker.model.enums.MatchStatus;

import java.util.List;

public interface MatchService {

    List<MatchDTO> getAllMatches();

    List<MatchDTO> getMatchesByPhase(Long phaseId);

    List<MatchDTO> getMatchesByStatus(MatchStatus status);

    MatchDTO getMatchById(Long id);

    MatchDTO getMatchByExternalId(Long externalId);

    MatchDTO createMatch(MatchDTO dto);

    MatchDTO createOrUpdateMatchFromWecanprono(WecanpronoMatchDTO dto);

    MatchDTO updateMatch(Long id, MatchDTO dto);

    void deleteMatch(Long id);

    void enableTracking(Long id);

    void disableTracking(Long id);

    MatchDTO refreshMatch(Long id);

    MatchDTO manualUpdate(Long id, Integer scoreHome, Integer scoreAway, MatchStatus status, Integer scoreHomeTAB, Integer scoreAwayTAB);

    void deleteFinishedMatches();
}
