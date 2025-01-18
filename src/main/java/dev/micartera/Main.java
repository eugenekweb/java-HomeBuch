package dev.micartera;

import dev.micartera.infrastructure.config.ApplicationConfig;
import dev.micartera.infrastructure.config.LoggerConfig;
import dev.micartera.presentation.cli.ConsoleUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            ApplicationConfig.initialize();
            LoggerConfig.initialize();
            ConsoleUI ui = new ConsoleUI();
            ui.start();
        } catch (Exception e) {
            logger.error("Критическая ошибка при запуске приложения", e);
            System.err.println("Произошла ошибка при запуске приложения. Проверьте логи.");
        }
    }
}