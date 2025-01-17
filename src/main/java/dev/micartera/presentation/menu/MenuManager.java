package dev.micartera.presentation.menu;

import dev.micartera.domain.exception.AuthenticationException;
import dev.micartera.domain.exception.ValidationException;
import dev.micartera.domain.model.*;
import dev.micartera.domain.repository.TransactionRepository;
import dev.micartera.domain.repository.UserRepository;
import dev.micartera.domain.repository.WalletRepository;
import dev.micartera.domain.service.*;
import dev.micartera.infrastructure.config.ApplicationConfig;
import dev.micartera.infrastructure.repository.FileTransactionRepository;
import dev.micartera.infrastructure.repository.FileUserRepository;
import dev.micartera.infrastructure.repository.FileWalletRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import dev.micartera.presentation.cli.ConsoleUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuManager {
    private static final Logger logger = LoggerFactory.getLogger(MenuManager.class);
    private final Map<String, Menu> menus;
    private Menu currentMenu;
    private final Scanner scanner;
    private final InputValidator inputValidator;
    private final OutputFormatter formatter;
    private UUID currentUserId;

    // Добавляем сервисы
    private final AuthenticationService authenticationService;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    public MenuManager() {
        this.menus = new HashMap<>();
        this.scanner = new Scanner(System.in);
        this.inputValidator = new InputValidator();
        this.formatter = new OutputFormatter();

        // Инициализация репозиториев
        UserRepository userRepository = new FileUserRepository();
        WalletRepository walletRepository = new FileWalletRepository();
        TransactionRepository transactionRepository = new FileTransactionRepository();
        ValidationService validationService = new ValidationService();

        // Инициализация сервисов
        this.authenticationService = new AuthenticationService(userRepository, validationService);
        this.walletService = new WalletService(walletRepository, validationService);
        this.transactionService = new TransactionService(walletService);
        this.notificationService = new NotificationService();

        initializeMenus();
    }

    private void initializeMenus() {
        // Главное меню (неавторизованный пользователь)
        Menu mainUnauthorized = new Menu("Главное меню");
        mainUnauthorized.addOption("1", "Вход в систему", () -> switchMenu("auth"));
        mainUnauthorized.addOption("2", "Регистрация", () -> switchMenu("register"));
        mainUnauthorized.addOption("3", "Выход из приложения", () -> System.exit(0));
        menus.put("main_unauthorized", mainUnauthorized);

        // Меню авторизации
        Menu authMenu = new Menu("Авторизация");
        authMenu.addOption("1", "Войти", this::handleLogin);
        authMenu.addOption("2", "Назад", () -> switchMenu("main_unauthorized"));
        menus.put("auth", authMenu);

        // Меню регистрации
        Menu registerMenu = new Menu("Регистрация");
        registerMenu.addOption("1", "Зарегистрироваться", this::handleRegistration);
        registerMenu.addOption("2", "Назад", () -> switchMenu("main_unauthorized"));
        menus.put("register", registerMenu);

        // Главное меню (авторизованный пользователь)
        Menu mainAuthorized = new Menu("Главное меню");
        mainAuthorized.addOption("1", "Управление финансами", () -> switchMenu("finance"));
        mainAuthorized.addOption("2", "Переводы", () -> switchMenu("transfers"));
        mainAuthorized.addOption("3", "Статистика и отчеты", () -> switchMenu("reports"));
        mainAuthorized.addOption("4", "Настройки", () -> switchMenu("settings"));
        mainAuthorized.addOption("5", "Выход из аккаунта", () -> handleLogout());
        mainAuthorized.addOption("6", "Выход из приложения", () -> System.exit(0));
        menus.put("main_authorized", mainAuthorized);

        // Меню управления финансами
        Menu financeMenu = new Menu("Управление финансами");
        financeMenu.addOption("1", "Добавить доход", this::handleAddIncome);
        financeMenu.addOption("2", "Добавить расход", this::handleAddExpense);
        financeMenu.addOption("3", "Управление категориями", () -> switchMenu("categories"));
        financeMenu.addOption("4", "Просмотр баланса", this::handleViewBalance);
        financeMenu.addOption("5", "Назад", () -> switchMenu("main_authorized"));
        menus.put("finance", financeMenu);

        currentMenu = mainUnauthorized;

        initializeCategories();
        initializeTransfers();
        initializeReports();
        initializeSettings();
    }

    private void initializeCategories() {
        Menu categoriesMenu = new Menu("Управление категориями");
        categoriesMenu.addOption("1", "Просмотр категорий", this::handleViewCategories);
        categoriesMenu.addOption("2", "Создание категории", this::handleCreateCategory);
        categoriesMenu.addOption("3", "Удаление категории", this::handleDeleteCategory);
        categoriesMenu.addOption("4", "Установка бюджета", this::handleSetBudget);
        categoriesMenu.addOption("5", "Назад", () -> switchMenu("finance"));
        menus.put("categories", categoriesMenu);
    }

    private void initializeTransfers() {
        Menu transfersMenu = new Menu("Переводы");
        transfersMenu.addOption("1", "Новый перевод", this::handleNewTransfer);
        transfersMenu.addOption("2", "Активные переводы", () -> switchMenu("active_transfers"));
        transfersMenu.addOption("3", "История переводов", () -> switchMenu("transfer_history"));
        transfersMenu.addOption("4", "Назад", () -> switchMenu("main_authorized"));
        menus.put("transfers", transfersMenu);
    }

    private void initializeReports() {
        Menu reportsMenu = new Menu("Статистика и отчеты");
        reportsMenu.addOption("1", "Текущий период", this::handleCurrentPeriodReport);
        reportsMenu.addOption("2", "Выбор периода", this::handleCustomPeriodReport);
        reportsMenu.addOption("3", "История операций", () -> switchMenu("transaction_history"));
        reportsMenu.addOption("4", "Бюджеты", () -> switchMenu("budgets"));
        reportsMenu.addOption("5", "Назад", () -> switchMenu("main_authorized"));
        menus.put("reports", reportsMenu);
    }

    private void initializeSettings() {
        Menu settingsMenu = new Menu("Настройки");
        settingsMenu.addOption("1", "Сменить пароль", this::handleChangePassword);
        settingsMenu.addOption("2", "Настройка уведомлений", this::handleNotificationSettings);
        settingsMenu.addOption("3", "Назад", () -> switchMenu("main_authorized"));
        menus.put("settings", settingsMenu);
    }

    public void displayCurrentMenu() {
        System.out.println("\n=== " + currentMenu.getTitle() + " ===");
        currentMenu.getOptions().forEach((key, option) ->
                System.out.println(key + ". " + option.getDescription()));
        System.out.print("\nВыберите действие: ");
    }

    public void handleInput() {
        try {
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("До свидания!");
                saveSessionState();
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
            if (currentUserId == null) {
                throw new IllegalStateException("Пользователь не авторизован");
            }

            BigDecimal amount = inputValidator.readAmount("Введите сумму дохода: ");
            Category category = selectCategory(Category.CategoryType.INCOME);
            String description = inputValidator.readString("Введите описание (опционально): ");

            walletService.addIncome(currentUserId, amount, category, description);
            notificationService.notifyTransaction(new Transaction(
                    UUID.randomUUID(),
                    Transaction.TransactionType.INCOME,
                    amount,
                    category,
                    LocalDateTime.now(),
                    Transaction.TransactionStatus.APPROVED,
                    description,
                    null,
                    null,
                    null
            ));

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

            // TODO: Вызов сервиса для добавления расхода
            System.out.println(formatter.formatSuccess("Расход успешно добавлен"));
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private Category selectCategory(Category.CategoryType type) {
        if (currentUserId == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        // Получаем кошелек пользователя
        Optional<Wallet> walletOpt = walletService.findWalletByUserId(currentUserId);
        if (walletOpt.isEmpty()) {
            throw new IllegalStateException("Кошелек пользователя не найден");
        }

        Wallet wallet = walletOpt.get();
        List<Category> categories = wallet.getCategories().stream()
                .filter(c -> c.getType() == type)
                .toList();

        if (categories.isEmpty()) {
            System.out.println("\nУ вас пока нет категорий типа " +
                    (type == Category.CategoryType.INCOME ? "'ДОХОДЫ'" : "'РАСХОДЫ'"));
            System.out.println("Создать новую категорию? (да/нет)");

            String answer = scanner.nextLine().trim().toLowerCase();
            if (answer.equals("да")) {
                return createNewCategory(type);
            } else {
                throw new IllegalStateException("Для совершения операции необходима категория");
            }
        }

        while (true) {
            System.out.println("\nВыберите категорию:");
            for (int i = 0; i < categories.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, categories.get(i).getName());
            }
            System.out.printf("%d. Создать новую категорию%n", categories.size() + 1);
            System.out.print("\nВаш выбор: ");

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
                walletService.addCategory(currentUserId, name, type);

                // Получаем обновленный список категорий и находим только что созданную
                Optional<Wallet> walletOpt = walletService.findWalletByUserId(currentUserId);
                if (walletOpt.isPresent()) {
                    return walletOpt.get().getCategories().stream()
                            .filter(c -> c.getName().equals(name) && c.getType() == type)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Ошибка при создании категории"));
                }
                throw new IllegalStateException("Ошибка при получении кошелька");
            } catch (Exception e) {
                System.out.println(formatter.formatError("Ошибка при создании категории: " + e.getMessage()));
                System.out.println("Попробовать снова? (да/нет)");
                if (!scanner.nextLine().trim().toLowerCase().equals("да")) {
                    throw new IllegalStateException("Операция отменена пользователем");
                }
            }
        }
    }

    private void handleLogout() {
        currentUserId = null;
        switchMenu("main_unauthorized");
    }

    // ... остальные обработчики команд будут добавлены позже

    private void switchMenu(String menuKey) {
        Menu menu = menus.get(menuKey);
        if (menu != null) {
            currentMenu = menu;
        }
    }

    private void handleLogin() {
        try {
            System.out.print("Введите логин: ");
            String login = scanner.nextLine().trim();
            System.out.print("Введите пароль: ");

            String password = scanner.nextLine().trim();

            Optional<User> user = authenticationService.authenticate(login, password);
            if (user.isPresent()) {
                currentUserId = user.get().getId();
                System.out.println(formatter.formatSuccess("Вход выполнен успешно!"));
                switchMenu("main_authorized");
            } else {
                System.out.println(formatter.formatError("Неверный логин или пароль"));
            }
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void handleRegistration() {
        try {
            System.out.print("Придумайте логин: ");
            String login = scanner.nextLine().trim();
            System.out.print("Придумайте пароль: ");
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
            if (currentUserId == null) {
                throw new IllegalStateException("Пользователь не авторизован");
            }

            // TODO: Получить категории из сервиса
            List<Category> categories = new ArrayList<>(); // Временная заглушка

            if (categories.isEmpty()) {
                System.out.println("У вас пока нет категорий");
                return;
            }

            System.out.println("\nВаши категории:");
            categories.forEach(category ->
                    System.out.printf("%s (%s)%n",
                            category.getName(),
                            category.getType()));
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void handleCreateCategory() {
        try {
            System.out.println("\nСоздание новой категории");
            System.out.println("1. Доход");
            System.out.println("2. Расход");

            String choice = inputValidator.readString("Выберите тип категории (1/2): ");
            Category.CategoryType type = switch (choice) {
                case "1" -> Category.CategoryType.INCOME;
                case "2" -> Category.CategoryType.EXPENSE;
                default -> throw new ValidationException("Неверный тип категории");
            };

            String name = inputValidator.readString("Введите название категории: ");
            walletService.addCategory(currentUserId, name, type);
            System.out.println(formatter.formatSuccess("Категория успешно создана"));
        } catch (Exception e) {
            logger.error("Ошибка при создании категории", e);
            System.out.println(formatter.formatError("Не удалось создать категорию: " + e.getMessage()));
        }
    }


    private void handleDeleteCategory() {
        try {
            Optional<Wallet> walletOpt = walletService.findWalletByUserId(currentUserId);
            if (walletOpt.isEmpty()) {
                System.out.println(formatter.formatError("Кошелек не найден"));
                return;
            }

            List<Category> categories = walletOpt.get().getCategories();
            if (categories.isEmpty()) {
                System.out.println("Нет доступных категорий для удаления");
                return;
            }

            System.out.println("\nДоступные категории:");
            for (int i = 0; i < categories.size(); i++) {
                System.out.printf("%d. %s (%s)%n", i + 1, categories.get(i).getName(),
                        categories.get(i).getType());
            }

            int choice = Integer.parseInt(inputValidator.readString("Выберите номер категории для удаления: ")) - 1;
            if (choice < 0 || choice >= categories.size()) {
                throw new ValidationException("Неверный номер категории");
            }

            Category categoryToDelete = categories.get(choice);
            walletService.deleteCategory(currentUserId, categoryToDelete.getId());
            System.out.println(formatter.formatSuccess("Категория успешно удалена"));

        } catch (Exception e) {
            logger.error("Ошибка при удалении категории", e);
            System.out.println(formatter.formatError("Не удалось удалить категорию: " + e.getMessage()));
        }
    }

    private void handleSetBudget() {
        try {
            Optional<Wallet> walletOpt = walletService.findWalletByUserId(currentUserId);
            if (walletOpt.isEmpty()) {
                System.out.println(formatter.formatError("Кошелек не найден"));
                return;
            }

            List<Category> expenseCategories = walletOpt.get().getCategories().stream()
                    .filter(c -> c.getType() == Category.CategoryType.EXPENSE)
                    .collect(Collectors.toList());

            if (expenseCategories.isEmpty()) {
                System.out.println("Нет доступных категорий расходов");
                return;
            }

            System.out.println("\nКатегории расходов:");
            for (int i = 0; i < expenseCategories.size(); i++) {
                Budget budget = walletOpt.get().getBudgets().get(expenseCategories.get(i).getId());
                String limit = budget != null ? formatter.formatAmount(budget.getLimit()) : "не установлен";
                System.out.printf("%d. %s (текущий лимит: %s)%n",
                        i + 1,
                        expenseCategories.get(i).getName(),
                        limit);
            }

            int choice = Integer.parseInt(inputValidator.readString("Выберите номер категории: ")) - 1;
            if (choice < 0 || choice >= expenseCategories.size()) {
                throw new ValidationException("Неверный номер категории");
            }

            BigDecimal limit = inputValidator.readAmount("Введите лимит бюджета: ");
            walletService.setBudget(currentUserId, expenseCategories.get(choice).getId(), limit);
            System.out.println(formatter.formatSuccess("Бюджет успешно установлен"));

        } catch (Exception e) {
            logger.error("Ошибка при установке бюджета", e);
            System.out.println(formatter.formatError("Не удалось установить бюджет: " + e.getMessage()));
        }
    }

    private void handleViewBalance() {
        try {
            if (currentUserId == null) {
                throw new IllegalStateException("Пользователь не авторизован");
            }

            // TODO: Получить данные из сервиса
            BigDecimal balance = BigDecimal.ZERO; // Временная заглушка
            Map<Category, BigDecimal> categoryBalances = new HashMap<>(); // Временная заглушка

            System.out.println("\nТекущий баланс: " + formatter.formatAmount(balance));

            if (!categoryBalances.isEmpty()) {
                System.out.println("\nБаланс по категориям:");
                categoryBalances.forEach((category, amount) ->
                        System.out.printf("%s: %s%n",
                                category.getName(),
                                formatter.formatAmount(amount)));
            }
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void handleCurrentPeriodReport() {
        try {
            if (currentUserId == null) {
                throw new IllegalStateException("Пользователь не авторизован");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

            List<Transaction> transactions = transactionService.getTransactionHistory(
                    currentUserId,
                    startOfMonth,
                    now
            );
//            List<Transaction> transactions = new ArrayList<>(); // Временная заглушка

            showTransactionReport(transactions, startOfMonth, now);
        } catch (Exception e) {
            logger.error("Ошибка при формировании отчета за текущий период", e);
            System.out.println(formatter.formatError("Не удалось сформировать отчет: " + e.getMessage()));
        }
    }

    private void handleCustomPeriodReport() {
        try {
            if (currentUserId == null) {
                throw new IllegalStateException("Пользователь не авторизован");
            }

            LocalDateTime startDate = inputValidator.readDate("Введите начальную дату (ДД.ММ.ГГГГ): ");
            LocalDateTime endDate = inputValidator.readDate("Введите конечную дату (ДД.ММ.ГГГГ): ");

            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("Конечная дата не может быть раньше начальной");
            }

            List<Transaction> transactions = transactionService.getTransactionHistory(
                    currentUserId,
                    startDate,
                    endDate
            );
//            List<Transaction> transactions = new ArrayList<>(); // Временная заглушка

            showTransactionReport(transactions, startDate, endDate);
        } catch (Exception e) {
            logger.error("Ошибка при формировании отчета за выбранный период", e);
            System.out.println(formatter.formatError("Не удалось сформировать отчет: " + e.getMessage()));
        }
    }

    private void showTransactionReport(List<Transaction> transactions, LocalDateTime from, LocalDateTime to) {
        if (transactions.isEmpty()) {
            System.out.println("Транзакции за указанный период отсутствуют");
            return;
        }

        System.out.println("\nОтчет по транзакциям за период: " +
                from.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " - " +
                to.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        System.out.println("\nСписок транзакций:");
        for (Transaction t : transactions) {
            System.out.println(formatter.formatTransaction(t));

            if (t.getType() == Transaction.TransactionType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else if (t.getType() == Transaction.TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(t.getAmount());
            }
        }

        System.out.println("\nИтого:");
        System.out.println("Доходы: " + formatter.formatAmount(totalIncome));
        System.out.println("Расходы: " + formatter.formatAmount(totalExpense));
        System.out.println("Баланс за период: " +
                formatter.formatAmount(totalIncome.subtract(totalExpense)));
    }

    private void handleNewTransfer() {
        System.out.println("Переводы временно недоступны");
    }

    private void handleChangePassword() {
        try {
            if (currentUserId == null) {
                throw new IllegalStateException("Пользователь не авторизован");
            }

            System.out.print("Введите текущий пароль: ");
            String currentPassword = scanner.nextLine().trim();

            System.out.print("Введите новый пароль: ");
            String newPassword = scanner.nextLine().trim();

            // TODO: Вызов сервиса для смены пароля
            System.out.println(formatter.formatSuccess("Пароль успешно изменен"));
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void handleNotificationSettings() {
        try {
            if (currentUserId == null) {
                throw new IllegalStateException("Пользователь не авторизован");
            }

            System.out.println("\nНастройки уведомлений:");
            System.out.println("1. Включить все уведомления");
            System.out.println("2. Отключить все уведомления");
            System.out.println("3. Назад");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    // TODO: Вызов сервиса для включения уведомлений
                    System.out.println(formatter.formatSuccess("Уведомления включены"));
                    break;
                case "2":
                    // TODO: Вызов сервиса для отключения уведомлений
                    System.out.println(formatter.formatSuccess("Уведомления отключены"));
                    break;
                case "3":
                    break;
                default:
                    System.out.println("Неверный выбор");
            }
        } catch (Exception e) {
            System.out.println(formatter.formatError(e.getMessage()));
        }
    }

    private void saveSessionState() {
        if (currentUserId != null) {
            try {
                Properties sessionProps = new Properties();
                sessionProps.setProperty("userId", currentUserId.toString());
                sessionProps.setProperty("lastActivity", LocalDateTime.now().toString());

                File sessionFile = new File(ApplicationConfig.getProperty("app.storage.path") + "/session.properties");
                try (FileOutputStream out = new FileOutputStream(sessionFile)) {
                    sessionProps.store(out, "Session state");
                }
            } catch (IOException e) {
                logger.error("Error saving session state", e);
            }
        }
    }

    private void restoreSessionState() {
        File sessionFile = new File(ApplicationConfig.getProperty("app.storage.path") + "/session.properties");
        if (sessionFile.exists()) {
            try {
                Properties sessionProps = new Properties();
                try (FileInputStream in = new FileInputStream(sessionFile)) {
                    sessionProps.load(in);
                    String userId = sessionProps.getProperty("userId");
                    String lastActivity = sessionProps.getProperty("lastActivity");

                    if (userId != null && lastActivity != null) {
                        LocalDateTime lastActivityTime = LocalDateTime.parse(lastActivity);
                        if (ChronoUnit.MINUTES.between(lastActivityTime, LocalDateTime.now()) < 30) {
                            currentUserId = UUID.fromString(userId);
                            switchMenu("main_authorized");
                            return;
                        }
                    }
                }
                // Если сессия устарела или невалидна, удаляем файл
                sessionFile.delete();
            } catch (Exception e) {
                logger.error("Error restoring session state", e);
                sessionFile.delete();
            }
        }
        switchMenu("main_unauthorized");
    }

}

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

    public String getTitle() {
        return title;
    }

    public Map<String, MenuOption> getOptions() {
        return options;
    }
}

class MenuOption {
    private final String description;
    private final Runnable action;

    public MenuOption(String description, Runnable action) {
        this.description = description;
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public Runnable getAction() {
        return action;
    }
}

class InputValidator {
    private final Scanner scanner = new Scanner(System.in);
    private final ValidationService validationService = new ValidationService();

    public String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public BigDecimal readAmount(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
                if (validationService.validateAmount(amount)) {
                    return amount;
                }
                System.out.println("Некорректная сумма. Попробуйте снова.");
            } catch (NumberFormatException e) {
                System.out.println("Введите числовое значение.");
            }
        }
    }

    public LocalDateTime readDate(String prompt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        while (true) {
            try {
                System.out.print(prompt);
                return LocalDateTime.parse(scanner.nextLine().trim() + " 00:00", formatter);
            } catch (Exception e) {
                System.out.println("Некорректная дата. Используйте формат ДД.ММ.ГГГГ");
            }
        }
    }
}

class OutputFormatter {
    public String formatSuccess(String message) {
        return "✓ " + message;
    }

    public String formatError(String message) {
        return "✗ Ошибка: " + message;
    }

    public String formatAmount(BigDecimal amount) {
        return String.format("%,.2f ₽", amount);
    }

    public String formatTransaction(Transaction transaction) {
        StringBuilder sb = new StringBuilder();

        // Форматируем дату
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        sb.append(transaction.getCreated().format(dateFormatter))
                .append(" | ");

        // Добавляем тип транзакции и сумму
        switch (transaction.getType()) {
            case INCOME -> sb.append("Доход: +");
            case EXPENSE -> sb.append("Расход: -");
            case TRANSFER -> {
                // TODO: Добавить информацию об отправителе/получателе после реализации переводов
                sb.append("Перевод: ");
            }
        }

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

        return sb.toString();
    }
}