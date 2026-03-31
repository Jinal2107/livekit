package com.livekitdemo.livekitdemo.service;

import io.livekit.server.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LiveKitService {

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    /**
     * Generate a LiveKit JWT token for a given room and user.
     * If isHost is true, additional moderator grants are included.
     */
    public String generateToken(String roomName, String identity, boolean isHost) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(identity);
        token.setIdentity(identity);
        token.setMetadata(isHost ? "role:HOST" : "role:PARTICIPANT");

        if (isHost) {
            token.addGrants(
                    new RoomJoin(true),
                    new RoomName(roomName),
                    new RoomAdmin(true),
                    new CanPublish(true),
                    new CanSubscribe(true),
                    new CanPublishData(true)
            );
        } else {
            token.addGrants(
                    new RoomJoin(true),
                    new RoomName(roomName),
                    new CanPublish(true),
                    new CanSubscribe(true),
                    new CanPublishData(true)
            );
        }
        return token.toJwt();
    }

    /**
     * Backward-compatible overload (defaults to non-host).
     */
    public String generateToken(String roomName, String identity) {
        return generateToken(roomName, identity, false);
    }
}
