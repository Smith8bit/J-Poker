package dev.gamfactory.poker;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    // Spring Data automatically implements this based on the method name
    Optional<User> findByUsername(String username);
}