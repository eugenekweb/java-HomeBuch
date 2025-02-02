package dev.micartera.presentation.menu;

import dev.micartera.domain.exception.AuthenticationException;
import dev.micartera.domain.exception.CommandCancelledException;
import dev.micartera.domain.exception.ValidationException;
import dev.micartera.domain.model.*;
import dev.micartera.domain.service.*;
import dev.micartera.infrastructure.config.ApplicationConfig;
import dev.micartera.infrastructure.repository.impl.UserRepositoryImpl;
import dev.micartera.infrastructure.repository.impl.WalletRepositoryImpl;
import dev.micartera.presentation.service.SessionState;
import dev.micartera.presentation.util.Color;
import dev.micartera.presentation.util.ColorPrinter;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MenuManager {
    private static final Logger logger = LoggerFactory.getLogger(MenuManager.class);
    private final Map<String, Menu> menus;
    private Menu currentMenu;
    private final Scanner scanner;
    private final InputValidator inputValidator;
    private final OutputFormatter formatter;
    private final SessionState sessionState;
    private final AuthenticationService authenticationService;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final ValidationService validationService;
    private boolean balanceIsAlwaysVisible = true;
    private final String MENU_SIGN = " \u25BC";
    private final String CLOSE_SIGN = " \u2612";
    private final String SIGNOUT_SIGN = " \u27A6";

    public MenuManager() {
        UserRepositoryImpl userRepository = new UserRepositoryImpl();
        WalletRepositoryImpl walletRepository = new WalletRepositoryImpl();

        this.sessionState = new SessionState(walletRepository, userRepository);
        this.scanner = new Scanner(System.in);
        this.inputValidator = new InputValidator();
        this.formatter = new OutputFormatter();
        this.menus = new HashMap<>();
        this.notificationService = new NotificationService(sessionState);
        this.validationService = new ValidationService();

        this.authenticationService = new AuthenticationService(
                sessionState,
                userRepository,
                validationService
        );

        this.walletService = new WalletService(
                walletRepository,
                validationService,
                notificationService,
                sessionState
        );

        this.transactionService = new TransactionService(walletService, sessionState);

        initializeMenus();
    }

    private void switchMenu(String menuKey) {
        Menu nextMenu = menus.get(menuKey);
        if (menuKey.equalsIgnoreCase("transfers")) menuItemIsUnderReconstruction(); // TODO: transfers are not ready yet
        if (nextMenu == null) {
            logger.error("Меню '{}' не найдено", menuKey);
            return;
        }
        currentMenu = nextMenu;
        logger.debug("Переключение на меню: {}", menuKey);
    }

    private void initializeMenus() {
        // Главное меню (неавторизованный пользователь)
        Menu mainUnauthorized = new Menu("Главное меню");
        mainUnauthorized.addOption("1", "Вход в систему" + MENU_SIGN, () -> switchMenu("auth"));
        mainUnauthorized.addOption("2", "Регистрация" + MENU_SIGN, () -> switchMenu("register"));
        mainUnauthorized.addOption("3", "Выход из приложения" + CLOSE_SIGN, () -> System.exit(0));
        menus.put("main_unauthorized", mainUnauthorized);

        // Меню авторизации
        Menu authMenu = new Menu("Авторизация");
        authMenu.addOption("1", "Войти", this::handleLogin);
        authMenu.addOption("2", "Назад" + MENU_SIGN, () -> switchMenu("main_unauthorized"));
        menus.put("auth", authMenu);

        // Меню регистрации
        Menu registerMenu = new Menu("Регистрация");
        registerMenu.addOption("1", "Зарегистрироваться", this::handleRegistration);
        registerMenu.addOption("2", "Назад" + MENU_SIGN, () -> switchMenu("main_unauthorized"));
        menus.put("register", registerMenu);

        // Главное меню (авторизованный пользователь)
        Menu mainAuthorized = new Menu("Главное меню");
        // TODO: delete TEST_BUTTON after tests
//        mainAuthorized.addOption("0", ColorPrinter.getColoredString("TEST_BUTTON", Color.PURPLE),
//                this::handleCustomPeriodReport);
        mainAuthorized.addOption("1", "Управление финансами" + MENU_SIGN, () -> switchMenu("finance"));
        mainAuthorized.addOption("2", "Переводы" + MENU_SIGN, () -> switchMenu("transfers"));
        mainAuthorized.addOption("3", "Статистика и отчеты" + MENU_SIGN, () -> switchMenu("reports"));
        mainAuthorized.addOption("4", "Настройки" + MENU_SIGN, () -> switchMenu("settings"));
        mainAuthorized.addOption("5", "Выход из аккаунта" + SIGNOUT_SIGN, () -> handleLogout());
        mainAuthorized.addOption("6", "Выход из приложения" + CLOSE_SIGN, () -> System.exit(0));
        menus.put("main_authorized", mainAuthorized);

        // Меню управления финансами
        Menu financeMenu = new Menu("Управление финансами");
        financeMenu.addOption("1", "Добавить расход", this::handleAddExpense);
        financeMenu.addOption("2", "Добавить доход", this::handleAddIncome);
        financeMenu.addOption("3", "Управление категориями" + MENU_SIGN, () -> switchMenu("categories"));
        financeMenu.addOption("4", "Просмотр баланса и бюджетов", this::handleViewBalance);
        financeMenu.addOption("5", "Назад" + MENU_SIGN, () -> switchMenu("main_authorized"));
        financeMenu.addOption("6", "Выход из аккаунта" + SIGNOUT_SIGN, () -> handleLogout());
        menus.put("finance", financeMenu);

        currentMenu = mainUnauthorized; // начальное меню

        initializeCategories();
        initializeBudgets();
        initializeTransfers();
        initializeReports();
        initializeSettings();
    }

    private void initializeCategories() {
        Menu categoriesMenu = new Menu("Управление категориями");
        categoriesMenu.addOption("1", "Просмотр категорий", this::handleViewCategories);
        categoriesMenu.addOption("2", "Создание категории", this::handleCreateCategory);
        categoriesMenu.addOption("3", "Удаление категории", this::handleDeleteCategory);
        categoriesMenu.addOption("4", "Управление бюджетами" + MENU_SIGN, () -> switchMenu("budgets"));
        categoriesMenu.addOption("5", "Назад" + MENU_SIGN, () -> switchMenu("finance"));
        categoriesMenu.addOption("6", "Выход из аккаунта" + SIGNOUT_SIGN, () -> handleLogout());
        menus.put("categories", categoriesMenu);
    }

    private void initializeBudgets() {
        Menu budgetsMenu = new Menu("Управление бюджетами");
        budgetsMenu.addOption("1", "Установка бюджета", this::handleSetBudget);
        budgetsMenu.addOption("2", "Удаление бюджета", this::handleDeleteBudget);
        budgetsMenu.addOption("3", "Просмотр баланса и бюджетов", this::handleViewBalance);
        // TODO:
        // budgetsMenu.addOption("4", "Включить/отключить бюджет", this::handleToggleBudget);
        // budgetsMenu.addOption("5", "Изменить лимит бюджета", this::handleUpdateBudgetLimit);
        // budgetsMenu.addOption("6", "Изменить дату начала бюджета", this::handleUpdateBudgetStartDate);
        budgetsMenu.addOption("4", "Назад" + MENU_SIGN, () -> switchMenu("main_authorized"));
        budgetsMenu.addOption("5", "Выход из аккаунта" + SIGNOUT_SIGN, () -> handleLogout());
        menus.put("budgets", budgetsMenu);
    }

    private void initializeTransfers() {
        Menu transfersMenu = new Menu("Переводы");
        transfersMenu.addOption("1", "Новый перевод", this::handleNewTransfer);
        transfersMenu.addOption("2", "Активные переводы" + MENU_SIGN, () -> switchMenu("active_transfers"));
        transfersMenu.addOption("3", "История переводов" + MENU_SIGN, () -> switchMenu("transfer_history"));
        transfersMenu.addOption("4", "Назад" + MENU_SIGN, () -> switchMenu("main_authorized"));
        transfersMenu.addOption("5", "Выход из аккаунта" + SIGNOUT_SIGN, () -> handleLogout());
        menus.put("transfers", transfersMenu);
    }

    private void initializeReports() {
        Menu reportsMenu = new Menu("Статистика и отчеты");
        reportsMenu.addOption("1", "Текущий период", this::handleCurrentPeriodReport);
        reportsMenu.addOption("2", "Выбор периода", this::handleCustomPeriodReport);
        reportsMenu.addOption("3", "История транзакций", this::handleAllTimeReport);
        reportsMenu.addOption("4", "Бюджеты" + MENU_SIGN, () -> switchMenu("budgets"));
        reportsMenu.addOption("5", "Назад" + MENU_SIGN, () -> switchMenu("main_authorized"));
        reportsMenu.addOption("6", "Выход из аккаунта" + SIGNOUT_SIGN, () -> handleLogout());
        menus.put("reports", reportsMenu);
    }

    private void initializeSettings() {
        Menu settingsMenu = new Menu("Настройки");
        settingsMenu.addOption("1", "Сменить пароль", this::handleChangePassword);
        settingsMenu.addOption("2", "Настройка уведомлений", this::handleNotificationSettings);
        settingsMenu.addOption("3", "Отображение баланса", this::handleBalanceSettings);
        settingsMenu.addOption("4", "Назад" + MENU_SIGN, () -> switchMenu("main_authorized"));
        settingsMenu.addOption("5", "Выход из аккаунта" + SIGNOUT_SIGN, () -> handleLogout());
        menus.put("settings", settingsMenu);
    }

    private void menuItemIsUnderReconstruction() {
        ColorPrinter.println("####    Этот раздел меню еще в разработке.", Color.BRIGHT_YELLOW);
    }

    public void displayCurrentMenu() {
        if (balanceIsAlwaysVisible && sessionState.getCurrentWallet() != null) { // TODO: если включен флаг отображения баланса везде -> Notifications
            formatter.printDelimiter();
            displayBalance();
        }

        // Показываем уведомления если они есть
        List<String> notifications = notificationService.getNotifications();
        if (!notifications.isEmpty()) {
            System.out.println(formatter.formatCaption("Уведомления:"));
            notifications.forEach(n -> System.out.println(formatter.formatWarning("! " + n)));
            notificationService.clearNotifications(); // Очищаем после показа
        }

        System.out.println("\n"
                + ColorPrinter.getColoredString("=== " + currentMenu.getTitle()
                + " ===", Color.BLACK, Color.BG_BRIGHT_WHITE));
        currentMenu.getOptions().forEach((key, option) ->
                System.out.println(key + ". " + option.getDescription()));
        System.out.println(formatter.formatPrompt("Выберите действие: "));
    }

    private void displayBalance() {
        System.out.println(formatter.formatCaption(
                "###  Текущий баланс: "
                        + formatter.formatAmount(sessionState.getCurrentWallet().getBalance())
                        + "  ###"));
    }

    public void handleInput() {
        try {
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("До свидания!");
//                saveSessionState();
                System.exit(0);
            }

            MenuOption option = currentMenu.getOptions().get(input);
            if (option != null) {
                option.getAction().run();
            } else {
                System.out.println(formatter.formatError("Неверный выбор. Попробуйте снова."));
            }
        } catch (AuthenticationException e) {
            logger.warn("Ошибка аутентификации: {}", e.getMessage());
            System.out.println(formatter.formatError("Ошибка аутентификации: " + e.getMessage()));
        } catch (ValidationException e) {
            logger.warn("Ошибка валидации: {}", e.getMessage());
            System.out.println(formatter.formatError("Ошибка валидации: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Непредвиденная ошибка: ", e);
            System.out.println(formatter.formatError("Произошла ошибка: " + e.getMessage()));
        }
    }

    // Обработчики команд
    private void handleAddIncome() {
        try {
            BigDecimal amount = inputValidator.readAmount("Введите сумму дохода: ");
            Category category = selectCategory(Category.CategoryType.INCOME);
            String description = inputValidator.readString("Введите описание (опционально): ");

            walletService.addIncome(amount, category, description);
            System.out.println(formatter.formatSuccess("Доход успешно добавлен"));
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void handleAddExpense() {
        try {
            BigDecimal amount = inputValidator.readAmount("Введите сумму расхода: ");
            Category category = selectCategory(Category.CategoryType.EXPENSE);
            String description = inputValidator.readString("Введите описание (опционально): ");

            walletService.addExpense(amount, category, description);
            System.out.println(formatter.formatSuccess("Расход успешно добавлен"));
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
            logger.error("Ошибка при добавлении расхода", e);
        }
    }

    private Category selectCategory(Category.CategoryType type) {
        Wallet wallet = sessionState.getCurrentWallet();
        List<Category> categories = wallet.getCategories().stream()
                .filter(c -> c.getType() == type)
                .toList();

        if (categories.isEmpty()) {
            System.out.println(formatter.formatInfo("\nУ вас пока нет категорий типа " +
                    (type == Category.CategoryType.INCOME ? "'ДОХОДЫ'" : "'РАСХОДЫ'")));
            System.out.println(formatter.formatPrompt("Создать новую категорию? (да/нет)"));

            String answer = scanner.nextLine().trim().toLowerCase();
            if (answer.equals("да")) {
                return createNewCategory(type);
            } else {
                throw new IllegalStateException("Для совершения операции необходима категория");
            }
        }

        while (true) {
            System.out.println(formatter.formatCaption("Выберите категорию:"));
            for (int i = 0; i < categories.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, categories.get(i).getName());
            }
            System.out.printf("%d. Создать новую категорию%n", categories.size() + 1);
            System.out.print(formatter.formatPrompt("\nВаш выбор: "));

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice > 0 && choice <= categories.size()) {
                    return categories.get(choice - 1);
                } else if (choice == categories.size() + 1) {
                    return createNewCategory(type);
                } else {
                    System.out.println(formatter.formatError("Неверный выбор. Попробуйте снова."));
                }
            } catch (NumberFormatException e) {
                System.out.println(formatter.formatError("Введите число."));
            }
        }
    }

    private Category createNewCategory(Category.CategoryType type) {
        while (true) {
            try {
                String name = inputValidator.readString("Введите название категории: ");
                walletService.addCategory(name, type);

                return sessionState.getCurrentWallet().getCategories().stream()
                        .filter(c -> c.getName().equals(name) && c.getType() == type)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Ошибка при создании категории"));
            } catch (Exception e) {
                System.out.println(formatter.formatError("Ошибка при создании категории: " + e.getMessage()));
                System.out.println(formatter.formatPrompt("Попробовать снова? (да/нет)"));
                if (!scanner.nextLine().trim().toLowerCase().equals("да")) {
                    throw new IllegalStateException("Операция отменена пользователем");
                }
            }
        }
    }

    private void handleLogout() {
        try {
            sessionState.saveAndClose();
            switchMenu("auth");
            System.out.println(formatter.formatSuccess("Вы успешно вышли из системы"));
        } catch (Exception e) {
            logger.error("Ошибка при выходе", e);
            System.out.println(formatter.formatError("Ошибка при выходе из системы"));
        }
    }


    private void handleLogin() {
        try {
            String login = inputValidator.readString("Введите логин: ");
            String password = inputValidator.readMaskedPassword("Введите пароль: ");

            Optional<User> user = authenticationService.authenticate(login, password);
            if (user.isPresent()) {
                sessionState.setCurrentSession(user.get().getId());
                switchMenu("main_authorized");
                System.out.println(formatter.formatSuccess("Добро пожаловать, " + login + "!"));
            } else {
                System.out.println(formatter.formatError("Неверный пароль"));
            }
        } catch (CommandCancelledException e) {
            System.out.println(formatter.formatInfo("Операция отменена"));
        } catch (Exception e) {
            logger.error("Ошибка при входе", e);
            System.out.println(formatter.formatError("Ошибка авторизации. " + e.getMessage()));
        }
    }

    private void handleRegistration() {
        try {
            System.out.print(formatter.formatPrompt("Придумайте логин: "));
            String login = scanner.nextLine().trim();
            System.out.print(formatter.formatPrompt("Придумайте пароль: "));
            String password = scanner.nextLine().trim();

            User user = authenticationService.register(login, password);
            walletService.createWallet(user.getId()); // Создаем кошелек для нового пользователя

            System.out.println(formatter.formatSuccess("Регистрация успешна! Теперь вы можете войти в систему."));
            switchMenu("auth");
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void handleViewCategories() {
        try {
            List<Category> categories = sessionState.getCurrentWallet().getCategories();
            if (categories.isEmpty()) {
                System.out.println(formatter.formatInfo("У вас пока нет категорий"));
                return;
            }

            System.out.println(formatter.formatCaption("Ваши категории:\t\tСумма по категории (с 1 числа месяца):"));
            categories.forEach(category ->
            {
                System.out.printf("%s (%s)\t|\t%s",
                        category.getName(),
                        walletService.getFormattedCategoryType(category),
                        formatter.formatAmount(walletService.getCategoryMonthTotal(category)));
                System.out.println();
            });
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void handleCreateCategory() {
        try {
            System.out.println(formatter.formatCaption("Создание новой категории"));
            System.out.println("1. Расход");
            System.out.println("2. Доход");

            String choice = inputValidator.readString("Выберите тип категории (1/2): ");
            Category.CategoryType type = switch (choice) {
                case "2" -> Category.CategoryType.INCOME;
                case "1" -> Category.CategoryType.EXPENSE;
                default -> throw new ValidationException("Неверный тип категории");
            };

            String name = inputValidator.readString("Введите название категории: ");
            walletService.addCategory(name, type);
            System.out.println(formatter.formatSuccess("Категория успешно создана"));
        } catch (Exception e) {
            logger.error("Ошибка при создании категории", e);
            System.out.println(formatter.formatError("Не удалось создать категорию: " + e.getMessage()));
        }
    }


    private void handleDeleteCategory() {
        try {
            if (sessionState.getCurrentWallet() == null) {
                System.out.println(formatter.formatError("Кошелек не найден"));
                return;
            }

            List<Category> categories = sessionState.getCurrentWallet().getCategories();
            if (categories.isEmpty()) {
                System.out.println(formatter.formatInfo("Нет доступных категорий для удаления"));
                return;
            }

            System.out.println(formatter.formatCaption("Доступные категории:"));
            for (int i = 0; i < categories.size(); i++) {
                System.out.printf("%d. %s (%s)%n", i + 1, categories.get(i).getName(),
                        categories.get(i).getType());
            }

            int choice = Integer.parseInt(inputValidator.readString("Выберите номер категории для удаления: ")) - 1;
            if (choice < 0 || choice >= categories.size()) {
                throw new ValidationException("Неверный номер категории");
            }

            Category categoryToDelete = categories.get(choice);
            walletService.deleteCategory(categoryToDelete.getId());
            System.out.println(formatter.formatSuccess("Категория успешно удалена"));

        } catch (Exception e) {
            logger.error("Ошибка при удалении категории", e);
            System.out.println(formatter.formatError("Не удалось удалить категорию: " + e.getMessage()));
        }
    }

    private void handleSetBudget() {
        try {
            List<Category> expenseCategories = sessionState.getCurrentWallet().getCategories().stream()
                    .filter(c -> c.getType() == Category.CategoryType.EXPENSE)
                    .toList();

            if (expenseCategories.isEmpty()) {
                System.out.println(formatter.formatInfo("У вас нет категорий расходов"));
                return;
            }

            System.out.println(formatter.formatCaption("Категории расходов:"));
            for (int i = 0; i < expenseCategories.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, expenseCategories.get(i).getName());
            }

            int index = Integer.parseInt(inputValidator.readString("Выберите номер категории: ")) - 1;
            if (index < 0 || index >= expenseCategories.size()) {
                throw new ValidationException("Неверный номер категории");
            }

            BigDecimal limit = inputValidator.readAmount("Введите лимит бюджета: ");
            walletService.setBudget(expenseCategories.get(index).getId(), limit);
            System.out.println(formatter.formatSuccess("Бюджет успешно установлен"));
        } catch (Exception e) {
            logger.error("Ошибка при установке бюджета", e);
            System.out.println(formatter.formatError("Не удалось установить бюджет"));
        }
    }

    private void handleDeleteBudget() {
        try {
            List<UUID> budgetCategories = sessionState.getCurrentWallet().getBudgets().values().stream()
                    .map(Budget::getCategoryId)
                    .toList();

            if (budgetCategories.isEmpty()) {
                System.out.println(formatter.formatInfo("У вас нет категорий с бюджетом"));
                return;
            }

            System.out.println(formatter.formatCaption("Категории с бюджетом:"));
            List<Category> categories = sessionState.getCurrentWallet().getCategories().stream()
                    .filter(c -> budgetCategories.contains(c.getId()))
                    .toList();
            for (int i = 0; i < categories.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, categories.get(i).getName());
            }

            int index = Integer.parseInt(inputValidator.readString("В какой категории удалить бюджет? ")) - 1;
            if (index < 0 || index >= categories.size()) {
                throw new ValidationException("Неверный номер категории");
            }

            walletService.deleteBudget(categories.get(index).getId());
            System.out.println(formatter.formatSuccess("Бюджет успешно удален"));
            logger.info("Бюджет успешно удален");
        } catch (Exception e) {
            logger.error("Ошибка при удалении бюджета", e);
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void handleViewBalance() {
        try {
            if (!balanceIsAlwaysVisible) { // TODO: если не включен флаг отображения баланса везде -> Notifications
                displayBalance();
            }

            // Отображение бюджетов по категориям
            Wallet wallet = sessionState.getCurrentWallet();
            Map<UUID, Budget> budgets = wallet.getBudgets();
            if (!budgets.isEmpty()) {
                System.out.println(formatter.formatCaption("Бюджеты по категориям:"));
                for (Map.Entry<UUID, Budget> entry : budgets.entrySet()) {
                    Category category = wallet.getCategories().stream()
                            .filter(c -> c.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow();
                    Budget budget = entry.getValue();

                    if (budget.isEnabled()) {
                        boolean overrun = budget.getSpent().compareTo(budget.getLimit()) > 0;
                        String pattern = "%s: %s из %s (%.1f%%).";
                        pattern = overrun ? Color.BRIGHT_YELLOW.getCode() + pattern
                                + Color.BRIGHT_RED.getCode() + " - Бюджет превышен!" + Color.RESET.getCode() : pattern;
                        pattern += "%nБюджет установлен с %s%n";
                        System.out.printf(pattern,
                                category.getName(),
                                formatter.formatAmount(budget.getSpent()),
                                formatter.formatAmount(budget.getLimit()),
                                budget.getSpent().divide(budget.getLimit(), 3, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)),
                                formatter.formatDate(budget.getSetAtDate())
                        );
                        formatter.printDelimiter();
                        System.out.println();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при отображении баланса и бюджетов", e);
            System.out.println(formatter.formatError("Не удалось отобразить баланс и бюджеты"));
        }
    }

    private void handleCurrentPeriodReport() {
        try {
            int defaultPeriodMonths = Integer.parseInt(ApplicationConfig.getProperty("app.default-period.months"));
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusMonths(defaultPeriodMonths);

            List<Transaction> transactions = transactionService.getTransactionHistory(startDate, endDate);
            showTransactionReport(transactions, startDate, endDate);
        } catch (Exception e) {
            logger.error("Ошибка при формировании отчета за текущий период", e);
            System.out.println(formatter.formatError("Не удалось сформировать отчет"));
        }
    }

    private void handleAllTimeReport() {
        try {
            List<Transaction> transactions =
                    transactionService.getTransactionHistory(LocalDateTime.MIN, LocalDateTime.now());
            showTransactionReport(transactions, LocalDateTime.MIN, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Ошибка при формировании истории транзакций", e);
            System.out.println(formatter.formatError("Не удалось сформировать историю транзакций"));
        }
    }

    private void handleCustomPeriodReport() {
        try {
            LocalDateTime startDate = inputValidator.readDate("Введите начальную дату (dd.MM.yyyy): ");
            LocalDateTime endDate = inputValidator.readDate("Введите конечную дату (dd.MM.yyyy): ");
            startDate = startDate.with(LocalTime.MIN); // сбрасываем время на 00:00
            endDate = endDate.with(LocalTime.MAX); // сбрасываем время на 23:59

            if (endDate.isBefore(startDate)) {
                System.out.println(formatter.formatError("Конечная дата не может быть раньше начальной"));
                return;
            }

            List<Transaction> transactions = transactionService.getTransactionHistory(startDate, endDate);
            showTransactionReport(transactions, startDate, endDate);
        } catch (Exception e) {
            logger.error("Ошибка при формировании отчета за выбранный период", e);
            System.out.println(formatter.formatError("Не удалось сформировать отчет: " + e.getMessage()));
        }
    }

    private void showTransactionReport(List<Transaction> transactions, LocalDateTime from, LocalDateTime to) {
        if (transactions.isEmpty()) {
            System.out.println(formatter.formatInfo("Нет транзакций за указанный период"));
            return;
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(ApplicationConfig.getProperty("app.date-format"));
        String reportsCaption = String.format("Отчет по транзакциям за период: %s - %s",
                from.format(dateFormat), to.format(dateFormat));
        System.out.println(formatter.formatCaption(reportsCaption));

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            System.out.println(formatter.formatTransaction(t));

            if (t.getType() == Transaction.TransactionType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else if (t.getType() == Transaction.TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(t.getAmount());
            }
        }

        System.out.println(formatter.formatCaption("Итого за период:"));
        System.out.println("Доходы: " + formatter.formatAmount(totalIncome));
        System.out.println("Расходы: " + formatter.formatAmount(totalExpense));
        System.out.println("Баланс: " + formatter.formatAmount(totalIncome.subtract(totalExpense)));
    }

    private void handleNewTransfer() {
        ColorPrinter.println("Переводы временно недоступны", Color.RED);
    }

    private void handleChangePassword() {
        try {
            User currentUser = sessionState.getCurrentUser();
            String currentPassword = inputValidator.readString("Введите текущий пароль: ");
            String newPassword = inputValidator.readString("Введите новый пароль: ");

            if (authenticationService.authenticate(currentUser.getLogin(), currentPassword).isEmpty()) {
                throw new AuthenticationException("Неверный текущий пароль");
            }

            if (!validationService.validatePassword(newPassword)) {
                throw new ValidationException("Новый пароль не соответствует требованиям безопасности");
            }
            authenticationService.changePassword(currentUser.getId(), newPassword);
            System.out.println(formatter.formatSuccess("Пароль успешно изменен"));
//            sessionState.saveAndClose();
            System.out.println(formatter.formatInfo("Авторизуйтесь заново."));
            handleLogout();
        } catch (Exception e) {
            logger.error("Ошибка при смене пароля", e);
            System.out.println(formatter.formatError("Не удалось изменить пароль"));
        }
    }

    private void handleBalanceSettings() {
        try {
            System.out.println(formatter.formatCaption("Показывать состояние баланса:"));
            System.out.println("1. Всегда (при любой операции)");
            System.out.println("2. Только в соответствующих меню");
            System.out.println("3. Назад");

            String choice = inputValidator.readString("Ваш выбор: ");

            switch (choice) {
                case "1" -> {
                    balanceIsAlwaysVisible = true;
                    System.out.println(formatter.formatSuccess("Отображение баланса всегда - включено"));
                }
                case "2" -> {
                    balanceIsAlwaysVisible = false;
                    System.out.println(formatter.formatSuccess("Отображение баланса всегда - выключено"));
                }
                case "3" -> {
                }
                default -> System.out.println(formatter.formatError("Неверный выбор"));
            }
        } catch (Exception e) {
            logger.error("Ошибка при настройке отображения баланса", e);
            System.out.println(formatter.formatError("Не удалось изменить настройки отображения баланса"));
        }
    }

    private void handleNotificationSettings() {
        menuItemIsUnderReconstruction(); // TODO: настройки пока хранятся только во время сессии
        try {
            System.out.println(formatter.formatCaption("Настройки уведомлений:"));
            System.out.println("1. Включить все уведомления");
            System.out.println("2. Отключить все уведомления");
            System.out.println("3. Назад");

            String choice = inputValidator.readString("Ваш выбор: ");

            switch (choice) {
                case "1" -> {
                    notificationService.enableNotifications(sessionState.getCurrentUser().getId());
                    System.out.println(formatter.formatSuccess("Уведомления включены"));
                }
                case "2" -> {
                    notificationService.disableNotifications(sessionState.getCurrentUser().getId());
                    System.out.println(formatter.formatSuccess("Уведомления отключены"));
                }
                case "3" -> {
                }
                default -> System.out.println(formatter.formatError("Неверный выбор"));
            }
        } catch (Exception e) {
            logger.error("Ошибка при настройке уведомлений", e);
            System.out.println(formatter.formatError("Не удалось изменить настройки уведомлений"));
        }
    }

    public void emergencyShutdown() {
        try {
            User currentUser = sessionState.getCurrentUser();
            if (currentUser != null) {
                sessionState.saveAndClose();
                logger.info("Состояние сессии сохранено при аварийном завершении");
                scanner.close();
            }
        } catch (Exception e) {
            logger.error("Ошибка при аварийном сохранении состояния", e);
        }
    }
}

@Data
class Menu {
    private final String title;
    private final Map<String, MenuOption> options;

    public Menu(String title) {
        this.title = title;
        this.options = new LinkedHashMap<>();
    }

    public void addOption(String key, String description, Runnable action) {
        options.put(key, new MenuOption(description, action));
    }
}

@Data
class MenuOption {
    private final String description;
    private final Runnable action;
}

class InputValidator {
    private static final Logger logger = LoggerFactory.getLogger(InputValidator.class);
    private final Scanner scanner = new Scanner(System.in);
    private final ValidationService validationService = new ValidationService();
    private final OutputFormatter formatter = new OutputFormatter();

    public String readString(String prompt) {
        System.out.print(formatter.formatPrompt(prompt));
        String input = scanner.nextLine().trim();
        if (input.equals("\u001B") || input.equalsIgnoreCase("esc")) { // Escape или esc
            throw new CommandCancelledException("Команда отменена");
        }
        return input;
    }

    public String readMaskedPassword(String prompt) {
        System.out.print(prompt + Color.GRAY.getCode() + Color.BG_GRAY.getCode() + "pass_mask");
        String password = scanner.nextLine().trim();
        System.out.print(Color.RESET.getCode());
        return password;
    }

    public BigDecimal readAmount(String prompt) {
        while (true) {
            try {
                System.out.print(formatter.formatPrompt(prompt));
                BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
                if (validationService.validateAmount(amount)) {
                    return amount;
                }
                System.out.println(formatter.formatWarning("Некорректная сумма. Попробуйте снова."));
            } catch (NumberFormatException e) {
                System.out.println(formatter.formatError("Введите числовое значение."));
            }
        }
    }

    public LocalDateTime readDate(String prompt) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        while (true) {
            try {
                System.out.print(formatter.formatPrompt(prompt));
                String date = scanner.nextLine().trim();
                return LocalDateTime.parse(date + " 00:00", dateFormatter);
            } catch (Exception e) {
                logger.error("Ошибка при вводе даты", e);
                System.out.println(formatter.formatError("Некорректная дата. Используйте формат ДД.ММ.ГГГГ"));
            }
        }
    }
}

class OutputFormatter {
    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern(ApplicationConfig.getProperty("app.date-format"));
    private static final String INCOME_SIGN = "➕ ";
    private static final String EXPENSE_SIGN = "➖ ";
    private static final String TRANSFER_SIGN = " ↔  ";

    public void printDelimiter() {
        System.out.print("-".repeat(32));
    }

    public String formatSuccess(String message) {

        return "\n" + ColorPrinter.getColoredString("  ✓ " + message + "  ", Color.BRIGHT_WHITE, Color.BG_GREEN);
    }

    public String formatError(String message) {
        return "\n" + ColorPrinter.getColoredString("  ✗ Ошибка: " + message + "  ", Color.BRIGHT_WHITE, Color.BG_RED);
    }

    public String formatCaption(String message) {
        return "\n" + ColorPrinter.getColoredString("  " + message + "  ", Color.BRIGHT_WHITE, Color.BG_BLUE);
    }

    public String formatWarning(String message) {
        return ColorPrinter.getColoredString("  " + message + "  ", Color.BLUE, Color.BG_BRIGHT_WHITE);
    }

    public String formatInfo(String message) {
        return ColorPrinter.getColoredString(message, Color.BRIGHT_CYAN);
    }

    public String formatPrompt(String prompt) {
        return ColorPrinter.getColoredString(prompt, Color.BRIGHT_GREEN);
    }

    public String formatAmount(BigDecimal amount) {
        return String.format("%,.2f ₽", amount);
    }

    public String formatDate(LocalDateTime date) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return date.format(dateFormatter);
    }

    public String formatTransaction(Transaction transaction) {

        StringBuilder sb = new StringBuilder();
        String sign = "";
        String colorCode = "";
        switch (transaction.getType()) {
            case INCOME -> {
                sign = INCOME_SIGN;
                colorCode = Color.BRIGHT_WHITE.getCode();
            }
            case EXPENSE -> {
                sign = EXPENSE_SIGN;
                colorCode = Color.RESET.getCode();
            }
            case TRANSFER -> { // TODO: будет реализовано позже при добавлении переводов
                sign = TRANSFER_SIGN;
                colorCode = Color.GRAY.getCode();
            }
        }

        // Выделим транзакции по типу разными цветами
        sb.append(colorCode);

        // Форматируем дату
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        sb.append(transaction.getCreated().format(dateFormatter))
                .append(" | ");

        // Добавляем тип транзакции и сумму
        sb.append(sign);

        sb.append(formatAmount(transaction.getAmount()))
                .append(" | ");

        // Добавляем категорию если есть
        if (transaction.getCategory() != null) {
            sb.append(transaction.getCategory().getName())
                    .append(" | ");
        }

        // Добавляем описание если есть
        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            sb.append(transaction.getDescription());
        }

        // Добавляем статус для переводов
        if (transaction.getType() == Transaction.TransactionType.TRANSFER) {
            sb.append(" | Статус: ").append(transaction.getStatus());
        }
        sb.append(Color.RESET.getCode());

        return sb.toString();
    }
}
