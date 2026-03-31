package com.livekitdemo.livekitdemo.service;

import com.livekitdemo.livekitdemo.dto.*;
import com.livekitdemo.livekitdemo.entity.*;
import com.livekitdemo.livekitdemo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final LiveKitService liveKitService;
    private final EgressService egressService;
    private final TranscriptionService transcriptionService;
    private final RecordingRepository recordingRepository;

    @Value("${livekit.url}")
    private String livekitUrl;

    // -------------------------------------------------------------------------
    // Create Room
    // -------------------------------------------------------------------------
    @Transactional
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        // Validate: room name must be unique among active rooms
        Optional<Room> existingRoom = roomRepository.findByRoomNameAndStatus(request.getRoomName(), RoomStatus.ACTIVE);
        if (existingRoom.isPresent()) {
            Room room = existingRoom.get();
            return CreateRoomResponse.builder()
                    .roomId(room.getId())
                    .roomName(room.getRoomName())
                    .createdBy(room.getCreatedBy())
                    .isPrivate(room.isPrivate())
                    .createdAt(room.getCreatedAt())
                    .status(room.getStatus().name())
                    .roomUrl("http://localhost:4200/room/" + room.getId())
                    .build();
        }

        // Validate: private rooms must have a password
        if (request.isPrivate() && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Private rooms must have a password."
            );
        }

        Room room = Room.builder()
                .roomName(request.getRoomName())
                .createdBy(request.getCreatedBy())
                .isPrivate(request.isPrivate())
                .password(request.getPassword())
                .status(RoomStatus.ACTIVE)
                .build();

        room = roomRepository.save(room);

        return CreateRoomResponse.builder()
                .roomId(room.getId())
                .roomName(room.getRoomName())
                .createdBy(room.getCreatedBy())
                .isPrivate(room.isPrivate())
                .createdAt(room.getCreatedAt())
                .status(room.getStatus().name())
                .roomUrl("http://localhost:4200/room/" + room.getId())
                .build();
    }

    // -------------------------------------------------------------------------
    // Join Room
    // -------------------------------------------------------------------------
    @Transactional
    public JoinRoomResponse joinRoom(JoinRoomRequest request) {
        // 1. Find active room
        Room room = roomRepository.findByIdAndStatus(request.getRoomId(), RoomStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found or no longer active."
                ));

        // 2. Validate password for private rooms
        if (room.isPrivate()) {
            if (request.getPassword() == null || !request.getPassword().equals(room.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect room password.");
            }
        }

        // 3. Determine role: first participant is HOST, rest are PARTICIPANTs
        boolean isFirstParticipant = participantRepository.findByRoomIdAndLeftAtIsNull(room.getId()).isEmpty();
        ParticipantRole role = isFirstParticipant ? ParticipantRole.HOST : ParticipantRole.PARTICIPANT;
        boolean isHost = (role == ParticipantRole.HOST);

        // 4. Save participant
        Participant participant = Participant.builder()
                .room(room)
                .userName(request.getUserName())
                .role(role)
                .build();
        participantRepository.save(participant);

        // 5. Generate LiveKit token
        String token = liveKitService.generateToken(room.getRoomName(), request.getUserName(), isHost);

        return JoinRoomResponse.builder()
                .token(token)
                .roomId(room.getId())
                .roomName(room.getRoomName())
                .userName(request.getUserName())
                .role(role.name())
                .livekitUrl(livekitUrl)
                .build();
    }

    // -------------------------------------------------------------------------
    // Leave Room
    // -------------------------------------------------------------------------
    @Transactional
    public void leaveRoom(LeaveRoomRequest request) {
        participantRepository
                .findByRoomIdAndUserNameAndLeftAtIsNull(request.getRoomId(), request.getUserName())
                .ifPresent(participant -> {
                    participant.setLeftAt(LocalDateTime.now());
                    participantRepository.save(participant);
                });
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // List Rooms
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CreateRoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.ACTIVE)
                .map(room -> CreateRoomResponse.builder()
                        .roomId(room.getId())
                        .roomName(room.getRoomName())
                        .createdBy(room.getCreatedBy())
                        .isPrivate(room.isPrivate())
                        .createdAt(room.getCreatedAt())
                        .status(room.getStatus().name())
                        .roomUrl("http://localhost:4200/room/" + room.getId())
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Get Room Details
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public CreateRoomResponse getRoomDetails(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));

        return CreateRoomResponse.builder()
                .roomId(room.getId())
                .roomName(room.getRoomName())
                .createdBy(room.getCreatedBy())
                .isPrivate(room.isPrivate())
                .createdAt(room.getCreatedAt())
                .status(room.getStatus().name())
                .roomUrl("http://localhost:4200/room/" + room.getId())
                .build();
    }

    // -------------------------------------------------------------------------
    // Get Active Participants
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ParticipantDto> getParticipants(UUID roomId) {
        // Validate room exists
        if (!roomRepository.existsById(roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found.");
        }

        return participantRepository.findByRoomIdAndLeftAtIsNull(roomId)
                .stream()
                .map(p -> ParticipantDto.builder()
                        .id(p.getId())
                        .userName(p.getUserName())
                        .role(p.getRole().name())
                        .joinedAt(p.getJoinedAt())
                        .leftAt(p.getLeftAt())
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Delete Room
    // -------------------------------------------------------------------------
    @Transactional
    public void deleteRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
        
        // Soft delete or hard delete? Let's do soft delete for consistency
        room.setStatus(RoomStatus.ENDED);
        roomRepository.save(room);
    }

    // -------------------------------------------------------------------------
    // Recording & Transcription
    // -------------------------------------------------------------------------

    @Transactional
    public String startRecording(UUID roomId) {
        return egressService.startRoomRecording(roomId);
    }

    @Transactional
    public void stopRecording(String egressId) {
        egressService.stopRecording(egressId);
    }

    @Transactional(readOnly = true)
    public List<RecordingDto> getRecordings(UUID roomId) {
        return recordingRepository.findByRoomId(roomId).stream()
                .map(r -> RecordingDto.builder()
                        .id(r.getId())
                        .egressId(r.getEgressId())
                        .status(r.getStatus())
                        .fileUrl(r.getFileUrl())
                        .createdAt(r.getCreatedAt())
                        .endedAt(r.getEndedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TranscriptDto> getTranscript(UUID roomId) {
        return transcriptionService.getTranscript(roomId).stream()
                .map(t -> TranscriptDto.builder()
                        .participantIdentity(t.getParticipantIdentity())
                        .text(t.getText())
                        .timestamp(t.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }
}
