package com.livekitdemo.livekitdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoomRequest {

    @NotBlank(message = "Room name is required")
    private String roomName;

    @NotBlank(message = "Creator name is required")
    private String createdBy;

    @com.fasterxml.jackson.annotation.JsonProperty("isPrivate")
    private boolean isPrivate;

    private String password;
}
