package com.livekitdemo.livekitdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordingDto {
    private UUID id;
    private String egressId;
    private String status;
    private String fileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}
