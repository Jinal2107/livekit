package com.livekitdemo.livekitdemo.repository;

import com.livekitdemo.livekitdemo.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordingRepository extends JpaRepository<Recording, UUID> {
    List<Recording> findByRoomId(UUID roomId);
    Optional<Recording> findByEgressId(String egressId);
}
