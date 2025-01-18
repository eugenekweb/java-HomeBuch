package dev.micartera.presentation.cli;

import dev.micartera.presentation.menu.MenuManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleUI {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleUI.class);
    private final MenuManager menuManager;

    public ConsoleUI() {
        this.menuManager = new MenuManager();
    }

    public void start() {
        logger.info("Запуск приложения");
        try {
//            menuManager.restoreSessionState(); // механизм восстановления сессии
            while (true) {
                menuManager.displayCurrentMenu();
                menuManager.handleInput();
            }
        } catch (Exception e) {
            logger.error("Неожиданная ошибка в работе приложения", e);
            menuManager.emergencyShutdown();
        }
    }
}