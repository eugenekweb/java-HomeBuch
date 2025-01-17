package dev.micartera.domain.repository;


import dev.micartera.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);
    void delete(UUID id);
}

