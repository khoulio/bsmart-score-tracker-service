package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.MatchEventDTO;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.enums.EventType;
import com.bsmart.scoretracker.model.enums.MatchStatus;

import java.util.List;

public interface MatchEventService {

    List<MatchEventDTO> getEventsByMatch(Long matchId);

    void createEvent(Match match, EventType eventType,
                    MatchStatus oldStatus, MatchStatus newStatus,
                    Integer oldScoreHome, Integer oldScoreAway,
                    Integer newScoreHome, Integer newScoreAway,
                    String minute, String rawStatus, String triggeredBy);
}
