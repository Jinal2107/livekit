package com.livekitdemo.livekitdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptDto {
    private String participantIdentity;
    private String text;
    private LocalDateTime timestamp;
}
