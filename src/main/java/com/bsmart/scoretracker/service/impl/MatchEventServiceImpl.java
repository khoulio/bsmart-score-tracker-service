package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.dto.MatchEventDTO;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.MatchEvent;
import com.bsmart.scoretracker.model.enums.EventType;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.repository.MatchEventRepository;
import com.bsmart.scoretracker.service.MatchEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchEventServiceImpl implements MatchEventService {

    private final MatchEventRepository matchEventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MatchEventDTO> getEventsByMatch(Long matchId) {
        return matchEventRepository.findByMatchIdOrderByTimestampDesc(matchId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createEvent(Match match, EventType eventType,
                          MatchStatus oldStatus, MatchStatus newStatus,
                          Integer oldScoreHome, Integer oldScoreAway,
                          Integer newScoreHome, Integer newScoreAway,
                          String minute, String rawStatus, String triggeredBy) {

        MatchEvent event = MatchEvent.builder()
            .match(match)
            .eventType(eventType)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .oldScoreHome(oldScoreHome)
            .oldScoreAway(oldScoreAway)
            .newScoreHome(newScoreHome)
            .newScoreAway(newScoreAway)
            .minute(minute)
            .rawStatus(rawStatus)
            .triggeredBy(triggeredBy)
            .build();

        matchEventRepository.save(event);

        log.debug("Created {} event for match {}: {} -> {}",
            eventType, match.getId(), oldStatus, newStatus);
    }

    private MatchEventDTO toDTO(MatchEvent event) {
        return MatchEventDTO.builder()
            .id(event.getId())
            .matchId(event.getMatch().getId())
            .timestamp(event.getTimestamp())
            .eventType(event.getEventType())
            .oldStatus(event.getOldStatus())
            .newStatus(event.getNewStatus())
            .oldScoreHome(event.getOldScoreHome())
            .oldScoreAway(event.getOldScoreAway())
            .newScoreHome(event.getNewScoreHome())
            .newScoreAway(event.getNewScoreAway())
            .minute(event.getMinute())
            .rawStatus(event.getRawStatus())
            .triggeredBy(event.getTriggeredBy())
            .build();
    }
}
