package com.bsmart.scoretracker.repository;

import com.bsmart.scoretracker.model.Phase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhaseRepository extends JpaRepository<Phase, Long> {

    List<Phase> findByCompetitionId(Long competitionId);

    List<Phase> findByCompetitionIdOrderByStartDateDesc(Long competitionId);

    Optional<Phase> findByExternalId(Long externalId);

    Optional<Phase> findByCompetitionIdAndExternalId(Long competitionId, Long externalId);

    Optional<Phase> findByCompetitionIdAndName(Long competitionId, String name);
}
