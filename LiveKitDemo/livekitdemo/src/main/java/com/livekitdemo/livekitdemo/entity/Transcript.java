package com.livekitdemo.livekitdemo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transcripts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcript {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "participant_identity")
    private String participantIdentity;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @CreationTimestamp
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
