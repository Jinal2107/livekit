package com.livekitdemo.livekitdemo.service;

import com.livekitdemo.livekitdemo.entity.Recording;
import com.livekitdemo.livekitdemo.entity.Room;
import com.livekitdemo.livekitdemo.repository.RecordingRepository;
import com.livekitdemo.livekitdemo.repository.RoomRepository;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EgressService {

    private final RecordingRepository recordingRepository;
    private final RoomRepository roomRepository;

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    @Value("${livekit.url}")
    private String livekitUrl;

    private OkHttpClient buildOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    private EgressServiceClient getEgressClient() {
        String httpUrl = livekitUrl.replaceFirst("^ws://", "http://").replaceFirst("^wss://", "https://");
        OkHttpClient okHttpClient = buildOkHttpClient();
        return EgressServiceClient.createClient(httpUrl, apiKey, apiSecret, () -> okHttpClient);
    }

    private void ensureRoomExistsInLiveKit(String roomName) {
        String httpUrl = livekitUrl.replaceFirst("^ws://", "http://").replaceFirst("^wss://", "https://");
        OkHttpClient okHttpClient = buildOkHttpClient();
        try {
            RoomServiceClient roomClient = RoomServiceClient.createClient(httpUrl, apiKey, apiSecret, () -> okHttpClient);
            roomClient.createRoom(roomName).execute();
        } catch (Exception e) {
            // Room may already exist – that's fine
        }
    }


    public String startRoomRecording(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        ensureRoomExistsInLiveKit(room.getRoomName());
        EgressServiceClient client = getEgressClient();
        
        LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFilepath("/out/" + room.getRoomName() + "_" + System.currentTimeMillis() + ".mp4")
                .build();

        try {
            retrofit2.Response<LivekitEgress.EgressInfo> response = client.startRoomCompositeEgress(room.getRoomName(), fileOutput).execute();
            if (!response.isSuccessful()) {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "LiveKit API Error: Code " + response.code() + ", Body: " + errorBody);
            }
            LivekitEgress.EgressInfo info = response.body();
            if (info == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to start recording: No response body from LiveKit");
            }
            String egressId = info.getEgressId();

            Recording recording = Recording.builder()

                    .room(room)
                    .egressId(egressId)
                    .status("STARTING")
                    .build();
            recordingRepository.save(recording);

            return egressId;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to start recording: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    public void stopRecording(String egressId) {
        EgressServiceClient client = getEgressClient();
        try {
            client.stopEgress(egressId).execute();
        } catch (java.io.IOException e) {
            // Log error but continue
        }

        recordingRepository.findByEgressId(egressId).ifPresent(recording -> {
            recording.setStatus("STOPPING");
            recordingRepository.save(recording);
        });
    }

    public void updateRecordingStatus(String egressId, String status, String fileUrl) {
        recordingRepository.findByEgressId(egressId).ifPresent(recording -> {
            recording.setStatus(status);
            if (fileUrl != null) {
                recording.setFileUrl(fileUrl);
            }
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                recording.setEndedAt(LocalDateTime.now());
            }
            recordingRepository.save(recording);
        });
    }
}
