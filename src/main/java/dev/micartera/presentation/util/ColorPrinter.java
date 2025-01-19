package dev.micartera.presentation.util;

public class ColorPrinter {

    // Метод для печати строки в консоль с указанным цветом
    public static void print(String message, Color... colors) {
        String combinedCode = Color.combineColors(colors);
        System.out.print(combinedCode + message + Color.RESET.getCode());
    }

    // Перегруженный метод для печати строки с новой строкой в конце
    public static void println(String message, Color... colors) {
        String combinedCode = Color.combineColors(colors);
        System.out.println(combinedCode + message + Color.RESET.getCode());
    }

    // отдаем просто окрашенную строку
    public static String getColoredString(String message, Color... colors) {
        String combinedCode = Color.combineColors(colors);
        return combinedCode + message + Color.RESET.getCode();
    }
}

