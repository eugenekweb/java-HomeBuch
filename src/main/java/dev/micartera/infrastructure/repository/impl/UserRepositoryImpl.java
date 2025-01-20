package dev.micartera.infrastructure.repository.impl;


import dev.micartera.domain.model.User;
import dev.micartera.infrastructure.config.ApplicationConfig;
import dev.micartera.infrastructure.repository.UserRepository;
import dev.micartera.infrastructure.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class UserRepositoryImpl implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);
    private final String usersPath;

    public UserRepositoryImpl() {
        this.usersPath = ApplicationConfig.getProperty("app.storage.path") + "/users/";
        new File(usersPath).mkdirs();
    }

    @Override
    public User save(User user) {
        try {
            String json = JsonUtils.toJson(user);
            File file = new File(usersPath + user.getId() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
            return user;
        } catch (IOException e) {
            logger.error("Error saving user: {}", user.getId(), e);
            throw new RuntimeException("Could not save user", e);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        File file = new File(usersPath + id + ".json");
        if (!file.exists()) {
            return Optional.empty();
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            return Optional.of(JsonUtils.fromJson(content, User.class));
        } catch (IOException e) {
            logger.error("Error reading user: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        File dir = new File(usersPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return Optional.empty();

        for (File file : files) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                User user = JsonUtils.fromJson(content, User.class);
                if (user.getLogin().equals(login)) {
                    return Optional.of(user);
                }
            } catch (IOException e) {
                logger.error("Error reading user file: {}", file.getName(), e);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByLogin(String login) {
        return findByLogin(login).isPresent();
    }

    @Override
    public void delete(UUID id) {
        File file = new File(usersPath + id + ".json");
        if (!file.delete() && file.exists()) {
            logger.error("Could not delete user file: {}", id);
            throw new RuntimeException("Could not delete user");
        }
    }
}