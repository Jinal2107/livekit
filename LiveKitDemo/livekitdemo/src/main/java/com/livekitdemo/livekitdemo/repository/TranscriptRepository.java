package com.livekitdemo.livekitdemo.repository;

import com.livekitdemo.livekitdemo.entity.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    List<Transcript> findByRoomIdOrderByTimestampAsc(UUID roomId);
}
