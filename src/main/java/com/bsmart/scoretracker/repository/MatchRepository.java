package com.bsmart.scoretracker.repository;

import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByPhaseId(Long phaseId);

    Optional<Match> findByExternalId(Long externalId);

    Optional<Match> findByPhaseIdAndExternalId(Long phaseId, Long externalId);

    List<Match> findByStatus(MatchStatus status);

    List<Match> findByTrackingEnabledTrueAndStatusIn(List<MatchStatus> statuses);

    List<Match> findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
        MatchStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT m FROM Match m WHERE m.trackingEnabled = true AND m.status = :status " +
           "AND m.kickoffUtc BETWEEN :start AND :end")
    List<Match> findTrackableMatches(
        @Param("status") MatchStatus status,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);

    List<Match> findByPhaseIdOrderByKickoffUtcAsc(Long phaseId);
}
