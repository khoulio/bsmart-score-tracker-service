package com.bsmart.scoretracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "phases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Phase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String stage;

    @Column(name = "match_day")
    private Integer matchDay;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "tracking_enabled", nullable = false)
    @Builder.Default
    private Boolean trackingEnabled = true;

    // ===== Champs de synchronisation avec l'API externe WECANPRONO =====

    @Column(name = "external_id")
    private Long externalId; // ID depuis WECANPRONO-SERVICE

    @Column(name = "is_prolongation_enabled")
    private Boolean isProlongationEnabled; // Prolongations activées

    @Column(name = "is_monetized")
    private Boolean isMonetized; // Phase monétisée

    @Column(name = "is_full")
    private Boolean isFull; // Phase complète

    @Column(name = "is_next_phase_created")
    private Boolean isNextPhaseCreated; // Phase suivante créée

    @Column(name = "is_started")
    private Boolean isStarted; // Phase démarrée

    @Column(name = "multiply")
    private Integer multiply; // Multiplicateur de points

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt; // Dernière synchronisation

    // ===== Fin champs synchronisation =====

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
