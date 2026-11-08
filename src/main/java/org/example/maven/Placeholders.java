package org.example.maven;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Подстановка значений переменных вида {@code ${name}} в строки из POM-файлов.
 */
final class Placeholders {

    /** Шаблон переменной: {@code ${имя}}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    /**
     * Максимальное число проходов подстановки: переменная может ссылаться
     * на другую переменную, а ограничение защищает от зацикливания.
     */
    private static final int MAX_PASSES = 10;

    /** Класс содержит только статические методы, создавать его не нужно. */
    private Placeholders() {
    }

    /**
     * Подставляет значения переменных в строку.
     *
     * <p>Переменные, для которых значение неизвестно, остаются как есть.
     *
     * @param text       исходная строка (может быть {@code null})
     * @param properties значения переменных
     * @return строка с подставленными значениями или {@code null}
     */
    static String resolve(String text, Map<String, String> properties) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (int pass = 0; pass < MAX_PASSES && result.contains("${"); pass++) {
            String next = replaceOnce(result, properties);
            if (next.equals(result)) {
                break;
            }
            result = next;
        }
        return result;
    }

    /**
     * Выполняет один проход подстановки.
     *
     * @param text       строка
     * @param properties значения переменных
     * @return строка после одного прохода подстановки
     */
    private static String replaceOnce(String text, Map<String, String> properties) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String value = properties.get(matcher.group(1));
            String replacement = value != null ? value : matcher.group();
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }
}
