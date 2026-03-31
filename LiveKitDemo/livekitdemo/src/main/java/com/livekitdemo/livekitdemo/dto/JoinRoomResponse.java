package com.livekitdemo.livekitdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomResponse {

    private String token;
    private UUID roomId;
    private String roomName;
    private String userName;
    private String role;
    private String livekitUrl;
}
