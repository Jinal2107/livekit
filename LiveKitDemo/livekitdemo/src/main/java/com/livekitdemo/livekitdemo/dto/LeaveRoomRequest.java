package com.livekitdemo.livekitdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LeaveRoomRequest {

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotBlank(message = "User name is required")
    private String userName;
}
