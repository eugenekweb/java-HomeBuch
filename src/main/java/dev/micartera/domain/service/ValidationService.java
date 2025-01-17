package dev.micartera.domain.service;


import java.math.BigDecimal;
import java.util.regex.Pattern;

public class ValidationService {
    private final Pattern loginPattern;
    private final Pattern passwordPattern;
    private final int minLoginLength;
    private final int maxLoginLength;

    public ValidationService() {
        this.loginPattern = Pattern.compile(System.getProperty("validation.login.pattern", "[a-zA-Z0-9_-]+"));
        this.passwordPattern = Pattern.compile(System.getProperty("validation.password.pattern", ".*"));
//                "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$"));
        this.minLoginLength = Integer.parseInt(System.getProperty("validation.login.min-length", "3"));
        this.maxLoginLength = Integer.parseInt(System.getProperty("validation.login.max-length", "20"));
    }

    public boolean validateLogin(String login) {
        return login != null &&
                login.length() >= minLoginLength &&
                login.length() <= maxLoginLength &&
                loginPattern.matcher(login).matches();
    }

    public boolean validatePassword(String password) {
        return password != null; //&& passwordPattern.matcher(password).matches();
    }

    public boolean validateAmount(BigDecimal amount) {
        return amount != null &&
                amount.compareTo(BigDecimal.ZERO) > 0 &&
                amount.scale() <= 2;
    }
}