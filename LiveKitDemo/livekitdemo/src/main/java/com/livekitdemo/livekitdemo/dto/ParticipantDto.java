package com.livekitdemo.livekitdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto {

    private UUID id;
    private String userName;
    private String role;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
