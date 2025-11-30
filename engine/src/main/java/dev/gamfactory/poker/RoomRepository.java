package dev.gamfactory.poker;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
    // You do not need to define findByRoomId.
    // Use .findById(id) and .existsById(id) which are provided automatically.
}