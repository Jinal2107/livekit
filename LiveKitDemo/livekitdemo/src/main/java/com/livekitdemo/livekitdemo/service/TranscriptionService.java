package com.livekitdemo.livekitdemo.service;

import com.livekitdemo.livekitdemo.entity.Room;
import com.livekitdemo.livekitdemo.entity.Transcript;
import com.livekitdemo.livekitdemo.repository.RoomRepository;
import com.livekitdemo.livekitdemo.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final TranscriptRepository transcriptRepository;
    private final RoomRepository roomRepository;

    public void saveSegment(String roomName, String participantIdentity, String text) {
        Room room = roomRepository.findByRoomName(roomName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        Transcript segment = Transcript.builder()
                .room(room)
                .participantIdentity(participantIdentity)
                .text(text)
                .build();
        transcriptRepository.save(segment);
    }

    public List<Transcript> getTranscript(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        return transcriptRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }
}
