package com.livekitdemo.livekitdemo.repository;

import com.livekitdemo.livekitdemo.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findByRoomId(UUID roomId);

    // Participants still in the room (have not left)
    List<Participant> findByRoomIdAndLeftAtIsNull(UUID roomId);

    Optional<Participant> findByRoomIdAndUserNameAndLeftAtIsNull(UUID roomId, String userName);
}
