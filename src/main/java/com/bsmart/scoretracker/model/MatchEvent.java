package com.bsmart.scoretracker.model;

import com.bsmart.scoretracker.model.enums.EventType;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_events", indexes = {
    @Index(name = "idx_match_timestamp", columnList = "match_id,timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private MatchStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private MatchStatus newStatus;

    @Column(name = "old_score_home")
    private Integer oldScoreHome;

    @Column(name = "old_score_away")
    private Integer oldScoreAway;

    @Column(name = "new_score_home")
    private Integer newScoreHome;

    @Column(name = "new_score_away")
    private Integer newScoreAway;

    @Column(length = 20)
    private String minute;

    @Column(name = "raw_status", length = 100)
    private String rawStatus;

    @Column(name = "triggered_by", length = 50)
    private String triggeredBy;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
