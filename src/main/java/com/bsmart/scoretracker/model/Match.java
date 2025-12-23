package com.bsmart.scoretracker.model;

import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches", indexes = {
    @Index(name = "idx_phase", columnList = "phase_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_kickoff", columnList = "kickoff_utc"),
    @Index(name = "idx_tracking", columnList = "tracking_enabled"),
    @Index(name = "idx_external_id", columnList = "external_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @Column(name = "home_team", nullable = false, length = 200)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 200)
    private String awayTeam;

    @Column(name = "kickoff_utc", nullable = false)
    private LocalDateTime kickoffUtc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProviderType provider;

    @Column(name = "match_url", nullable = false, length = 500)
    private String matchUrl;

    @Column(name = "tracking_enabled", nullable = false)
    @Builder.Default
    private Boolean trackingEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "score_home")
    private Integer scoreHome;

    @Column(name = "score_away")
    private Integer scoreAway;

    @Column(length = 20)
    private String minute;

    @Column(name = "raw_status", length = 100)
    private String rawStatus;

    @Column(name = "last_fetch_utc")
    private LocalDateTime lastFetchUtc;

    @Column(name = "error_count")
    @Builder.Default
    private Integer errorCount = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    // ===== Champs de synchronisation avec l'API externe WECANPRONO =====

    @Column(name = "external_id")
    private Long externalId; // ID depuis WECANPRONO-SERVICE

    @Column(name = "team_domicile_id")
    private Long teamDomicileId; // ID équipe domicile externe

    @Column(name = "team_exterieur_id")
    private Long teamExterieurId; // ID équipe extérieure externe

    @Column(name = "is_prolongation_enabled")
    private Boolean isProlongationEnabled; // Prolongations activées

    @Column(name = "score_home_tab")
    private Integer scoreHomeTAB;

    @Column(name = "score_away_tab")
    private Integer scoreAwayTAB;

    @Column(name = "winner_home_tab")
    private Boolean winnerHomeTAB;

    @Column(name = "winner_away_tab")
    private Boolean winnerAwayTAB;

    @Column(name = "is_monetized")
    private Boolean isMonetized; // Match monétisé

    @Column(name = "is_half_time_send")
    private Boolean isHalfTimeSend; // Signal mi-temps envoyé

    @Column(name = "is_end_half_time_send")
    private Boolean isEndHalfTimeSend; // Signal fin mi-temps envoyé

    @Column(name = "is_for_test")
    private Boolean isForTest; // Match de test

    @Column(name = "external_score_provider")
    private String externalScoreProvider; // Fournisseur de score externe

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt; // Dernière synchronisation

    // ===== Fin champs synchronisation =====

    // Anti-flapping fields
    @Enumerated(EnumType.STRING)
    @Column(name = "status_candidate", length = 30)
    private MatchStatus statusCandidate;

    @Column(name = "consecutive_same_candidate")
    @Builder.Default
    private Integer consecutiveSameCandidate = 0;

    @Column(name = "status_candidate_since_utc")
    private LocalDateTime statusCandidateSinceUtc;

    @Column(name = "half_time_seen")
    @Builder.Default
    private Boolean halfTimeSeen = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp DESC")
    @Builder.Default
    private List<MatchEvent> events = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
