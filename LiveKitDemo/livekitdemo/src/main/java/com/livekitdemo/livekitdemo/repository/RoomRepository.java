package com.livekitdemo.livekitdemo.repository;

import com.livekitdemo.livekitdemo.entity.Room;
import com.livekitdemo.livekitdemo.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByRoomName(String roomName);
    
    Optional<Room> findByRoomNameAndStatus(String roomName, RoomStatus status);

    Optional<Room> findByIdAndStatus(UUID id, RoomStatus status);

    boolean existsByRoomNameAndStatus(String roomName, RoomStatus status);
}
