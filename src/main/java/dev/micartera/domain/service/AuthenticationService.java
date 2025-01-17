package dev.micartera.domain.service;


import dev.micartera.domain.model.User;
import dev.micartera.domain.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final UserRepository userRepository;
    private final ValidationService validationService;

    public AuthenticationService(UserRepository userRepository, ValidationService validationService) {
        this.userRepository = userRepository;
        this.validationService = validationService;
    }

    public User register(String login, String password) {
        if (!validationService.validateLogin(login)) {
            throw new IllegalArgumentException("Некорректный логин");
        }
        if (!validationService.validatePassword(password)) {
            throw new IllegalArgumentException("Некорректный пароль");
        }
        if (userRepository.existsByLogin(login)) {
            throw new IllegalStateException("Пользователь уже существует");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .login(login)
                .passwordHash(BCrypt.hashpw(password, BCrypt.gensalt()))
                .created(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public Optional<User> authenticate(String login, String password) {
        return userRepository.findByLogin(login)
                .filter(user -> BCrypt.checkpw(password, user.getPasswordHash()));
    }
}