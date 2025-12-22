package com.bsmart.scoretracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "competitions", indexes = {
    @Index(name = "idx_code", columnList = "code", unique = true),
    @Index(name = "idx_external_id", columnList = "external_id"),
    @Index(name = "idx_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String country;

    // ===== Champs de synchronisation avec l'API externe WECANPRONO =====

    @Column(name = "external_id")
    private Long externalId; // ID depuis WECANPRONO-SERVICE

    @Column(length = 50)
    private String slug; // Slug de la compétition (PL, FL1, CAN, etc.)

    @Column(name = "logo_url", length = 500)
    private String logoUrl; // URL du logo

    @Column(name = "nb_users")
    private Integer nbUsers; // Nombre d'utilisateurs

    @Column(name = "date_start")
    private LocalDateTime dateStart; // Date de début

    @Column(name = "date_end")
    private LocalDateTime dateEnd; // Date de fin

    @Column(name = "is_open")
    private Boolean isOpen; // Compétition ouverte

    @Column(name = "is_league")
    private Boolean isLeague; // Est une ligue (vs tournoi)

    @Column(name = "is_started")
    private Boolean isStarted; // Compétition démarrée

    @Column(name = "is_featured")
    private Boolean isFeatured; // Compétition mise en avant

    @Column(name = "background_url", length = 500)
    private String backgroundUrl; // URL image de fond

    @Column(name = "sponsor_logo_url", length = 500)
    private String sponsorLogoUrl; // URL logo sponsor

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt; // Dernière synchronisation

    // ===== Fin champs synchronisation =====

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Phase> phases = new ArrayList<>();

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
