package dev.micartera.infrastructure.config;


import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ApplicationConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static Properties properties;
    private static final String USER_DIR = System.getProperty("user.dir");
    private static final Path CONFIG_PATH = Paths.get(USER_DIR, "storage/config/application.properties");
    private static final Path STORAGE_PATH = Paths.get(USER_DIR, "storage");

    @SneakyThrows
    public static void initialize() {
        properties = new Properties();

        if (!Files.exists(CONFIG_PATH)) {
            createDefaultConfig();
        }

        try (InputStream fis = Files.newInputStream(CONFIG_PATH)) {
            properties.load(fis);
        }

        initializeFileStructure();
    }

    @SneakyThrows
    private static void createDefaultConfig() {
        // Создание структуры каталогов и конфиг-файла с дефолтными настройками

        initializeFileStructure();

        // Создаем объект Properties и добавляем дефолтные настройки
        Properties defaultProps = new Properties();
        defaultProps.setProperty("app.storage.path", "storage");
        defaultProps.setProperty("app.date-format", "yyyy-MM-dd HH:mm");
        defaultProps.setProperty("app.default-period.months", "1");

        defaultProps.setProperty("logging.file", "${app.storage.path}/logs/app.log");
        defaultProps.setProperty("logging.level.root", "INFO");
        defaultProps.setProperty("logging.pattern", "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");

        defaultProps.setProperty("security.password.min-length", "3");
        defaultProps.setProperty("security.password.max-length", "32");
        defaultProps.setProperty("security.password.validation.enabled", "true");
        defaultProps.setProperty("validation.password.pattern", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");
        defaultProps.setProperty("security.login.validation.enabled", "true");
        defaultProps.setProperty("validation.login.min-length", "3");
        defaultProps.setProperty("validation.login.max-length", "20");
        defaultProps.setProperty("validation.login.pattern", "[a-zA-Z0-9_-]+");
        defaultProps.setProperty("security.session.timeout", "30");

        defaultProps.setProperty("low.balance.threshold", "1000");

        defaultProps.setProperty("transaction.lifetime.minutes", "1440");
        defaultProps.setProperty("transaction.min-amount", "0.01");
        defaultProps.setProperty("transaction.max-amount", "1000000");

        // Создание и запись конфигурационного файла
        try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
            defaultProps.store(outputStream, "Default configuration");
        }
    }

    @SneakyThrows
    private static void initializeFileStructure() {
        // Инициализация структуры хранения
        Files.createDirectories(STORAGE_PATH.resolve("config"));
        Files.createDirectories(STORAGE_PATH.resolve("users"));
        Files.createDirectories(STORAGE_PATH.resolve("transfers"));
        Files.createDirectories(STORAGE_PATH.resolve("logs"));
    }

    public static String getProperty(String key) {
        try {
            String value = properties.getProperty(key);
            logger.debug("Got property: {}={}", key, value);
            return value;
        } catch (Exception e) {
            logger.error("Error getting property: {}", key, e);
            return null;
        }
    }
}
