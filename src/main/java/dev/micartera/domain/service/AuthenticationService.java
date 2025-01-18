package dev.micartera.domain.service;


import dev.micartera.domain.exception.AuthenticationException;
import dev.micartera.domain.model.User;
import dev.micartera.infrastructure.repository.UserRepository;
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
//        if (userRepository.existsByLogin(login)) {
//            throw new IllegalStateException("Пользователь уже существует");
//        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .login(login)
                .passwordHash(BCrypt.hashpw(password, BCrypt.gensalt()))
                .created(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public Optional<User> authenticate(String login, String password) {
        logger.debug("Попытка аутентификации пользователя: {}", login);
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new AuthenticationException("Пользователь не найден"));
//        Optional<User> user = userRepository.findByLogin(login)
//                .filter(u -> BCrypt.checkpw(password, u.getPasswordHash()));
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            logger.info("Успешная аутентификация пользователя: {}", login);
        } else {
            logger.warn("Неудачная попытка аутентификации: {}", login);
        }
        return Optional.of(user);
    }

    public void changePassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Пользователь не найден"));

        String newPasswordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPasswordHash(newPasswordHash);

        userRepository.save(user);
        userRepository.clear();
        logger.info("Пароль успешно изменен для пользователя: {}", user.getLogin());
    }
}