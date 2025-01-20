package dev.micartera.infrastructure.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerConfig {
    private static final Logger logger = LoggerFactory.getLogger(LoggerConfig.class);

    public static void initialize() {
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.reset();

            FileAppender fileAppender = new FileAppender();
            fileAppender.setContext(context);
            fileAppender.setName("FILE");

            // Получаем путь к файлу логов из конфигурации
            String logFile = ApplicationConfig.getProperty("logging.file")
                    .replace("${app.storage.path}", ApplicationConfig.getProperty("app.storage.path"));
            fileAppender.setFile(logFile);

            // Настраиваем формат вывода
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern(ApplicationConfig.getProperty("logging.pattern"));
            encoder.start();

            fileAppender.setEncoder(encoder);
            fileAppender.start();

            // Получаем корневой логгер и добавляем аппендер
            ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(fileAppender);
            rootLogger.setLevel(ch.qos.logback.classic.Level.valueOf(
                    ApplicationConfig.getProperty("logging.level.root")));

            logger.info("Логирование успешно инициализировано");
        } catch (Exception e) {
            System.err.println("Ошибка при инициализации логирования: " + e.getMessage());
        }
    }
}