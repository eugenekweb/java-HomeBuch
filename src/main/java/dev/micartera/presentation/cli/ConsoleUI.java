package dev.micartera.presentation.cli;

import dev.micartera.presentation.menu.MenuManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleUI {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleUI.class); // TODO: логироание
    private final MenuManager menuManager;

    public ConsoleUI() {
        this.menuManager = new MenuManager();
    }

    public void start() {
        logger.info("Starting console UI");
        System.out.println("Добро пожаловать в систему управления личными финансами!");

        while (true) {
            try {
                menuManager.displayCurrentMenu();
                menuManager.handleInput();
            } catch (Exception e) {
                logger.error("Error in console UI", e);
                System.out.println("Произошла ошибка: " + e.getMessage());
            }
        }
    }
}