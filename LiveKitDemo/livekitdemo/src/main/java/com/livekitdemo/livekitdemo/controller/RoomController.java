package com.livekitdemo.livekitdemo.controller;

import com.livekitdemo.livekitdemo.dto.*;
import com.livekitdemo.livekitdemo.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class RoomController {

    private final RoomService roomService;

    /**
     * POST /api/rooms
     * Create a new room.
     */
    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        log.info("POST /api/rooms - Creating/Fetching room: {}", request.getRoomName());
        CreateRoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/rooms
     * List all active rooms.
     */
    @GetMapping
    public ResponseEntity<List<CreateRoomResponse>> getAllRooms() {
        log.info("GET /api/rooms - Listing all active rooms");
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    /**
     * DELETE /api/rooms/{roomId}
     * Delete a room.
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable UUID roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok(Map.of("message", "Room deleted successfully."));
    }

    /**
     * POST /api/rooms/join
     * Join an existing room. Returns a LiveKit token.
     */
    @PostMapping("/join")
    public ResponseEntity<JoinRoomResponse> joinRoom(@Valid @RequestBody JoinRoomRequest request) {
        log.info("POST /api/rooms/join - Joining room: {} for user: {}", request.getRoomId(), request.getUserName());
        JoinRoomResponse response = roomService.joinRoom(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/rooms/leave
     * Leave a room — updates participant left_at timestamp.
     */
    @PostMapping("/leave")
    public ResponseEntity<Map<String, String>> leaveRoom(@Valid @RequestBody LeaveRoomRequest request) {
        roomService.leaveRoom(request);
        return ResponseEntity.ok(Map.of("message", "Successfully left the room."));
    }

    /**
     * GET /api/rooms/{roomId}
     * Get room details.
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<CreateRoomResponse> getRoomDetails(@PathVariable UUID roomId) {
        CreateRoomResponse response = roomService.getRoomDetails(roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rooms/{roomId}/participants
     * Get active participants in a room.
     */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<List<ParticipantDto>> getParticipants(@PathVariable UUID roomId) {
        List<ParticipantDto> participants = roomService.getParticipants(roomId);
        return ResponseEntity.ok(participants);
    }

    /**
     * POST /api/rooms/{roomId}/record/start
     */
    @PostMapping("/{roomId}/record/start")
    public ResponseEntity<Map<String, String>> startRecording(@PathVariable UUID roomId) {
        String egressId = roomService.startRecording(roomId);
        log.info("Recording started: {}", egressId);
        return ResponseEntity.ok(Map.of("egressId", egressId, "message", "Recording started."));
    }

    /**
     * POST /api/rooms/record/stop?egressId=...
     */
    @PostMapping("/record/stop")
    public ResponseEntity<Map<String, String>> stopRecording(@RequestParam String egressId) {
        roomService.stopRecording(egressId);
        return ResponseEntity.ok(Map.of("message", "Recording stop signal sent."));
    }

    /**
     * GET /api/rooms/{roomId}/recordings
     */
    @GetMapping("/{roomId}/recordings")
    public ResponseEntity<List<RecordingDto>> getRecordings(@PathVariable UUID roomId) {
        log.info("GET /api/rooms/{}/recordings - Listing recordings", roomId);
        return ResponseEntity.ok(roomService.getRecordings(roomId));
    }

    /**
     * GET /api/rooms/{roomId}/transcript
     */
    @GetMapping("/{roomId}/transcript")
    public ResponseEntity<List<TranscriptDto>> getTranscript(@PathVariable UUID roomId) {
        log.info("GET /api/rooms/{}/transcript - Getting transcript", roomId);
        return ResponseEntity.ok(roomService.getTranscript(roomId));
    }
}
