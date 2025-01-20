package dev.micartera.infrastructure.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ApplicationConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static Properties properties;
    private static final String CONFIG_PATH = "./storage/config/application.properties";

    public static void initialize() throws IOException {
        properties = new Properties();
        File configFile = new File(CONFIG_PATH);

        if (!configFile.exists()) {
            createDefaultConfig();
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }

        initializeStorage();
    }

    private static void createDefaultConfig() throws IOException {
        // Создание структуры каталогов и конфиг-файла с дефолтными настройками
        File storageDir = new File("./storage");
        new File(storageDir, "config").mkdirs();
        new File(storageDir, "users").mkdir();
        new File(storageDir, "transfers").mkdir();
        new File(storageDir, "logs").mkdir();

        Properties defaultProps = new Properties();
        // Добавление дефолтных настроек
        defaultProps.setProperty("app.storage.path", "storage");
        defaultProps.setProperty("security.password.validation.enabled", "true");
        // ... другие настройки по умолчанию

        defaultProps.store(new FileOutputStream(CONFIG_PATH), "Default configuration");
    }

    private static void initializeStorage() {
        String storagePath = properties.getProperty("app.storage.path", "storage");
        // Инициализация структуры хранения
        new File(storagePath + "/users").mkdirs();
        new File(storagePath + "/transfers").mkdirs();
        new File(storagePath + "/logs").mkdirs();
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
