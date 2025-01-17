package dev.micartera;

import dev.micartera.infrastructure.config.ApplicationConfig;
import dev.micartera.presentation.cli.ConsoleUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting Personal Finance Management System");
            ApplicationConfig.initialize();
            ConsoleUI ui = new ConsoleUI();
            ui.start();
        } catch (Exception e) {
            logger.error("Application can't start", e);
            System.err.println("Ошибка запуска приложения: " + e.getMessage());
        }
    }
}
