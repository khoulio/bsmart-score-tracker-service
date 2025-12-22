package com.bsmart.scoretracker.repository;

import com.bsmart.scoretracker.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    Optional<Competition> findByCode(String code);

    Optional<Competition> findByExternalId(Long externalId);

    Optional<Competition> findBySlug(String slug);

    boolean existsByCode(String code);
}
