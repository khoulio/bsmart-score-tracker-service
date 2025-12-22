package com.bsmart.scoretracker.repository;

import com.bsmart.scoretracker.model.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    List<MatchEvent> findByMatchIdOrderByTimestampDesc(Long matchId);

    List<MatchEvent> findTop10ByMatchIdOrderByTimestampDesc(Long matchId);
}
