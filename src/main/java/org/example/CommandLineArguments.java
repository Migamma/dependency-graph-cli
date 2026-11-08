package org.example;

import org.example.config.ConfigException;

/**
 * Аргументы командной строки.
 *
 * @param configPath путь к YAML-файлу конфигурации
 * @param reverse    {@code true}, если нужно вывести обратные зависимости
 */
public record CommandLineArguments(String configPath, boolean reverse) {

    /** Путь к конфигурации, если он не передан аргументом. */
    private static final String DEFAULT_CONFIG_PATH = "config.yaml";
    /** Флаг режима обратных зависимостей. */
    private static final String REVERSE_FLAG = "--reverse";
    /** Подсказка о правильном запуске. */
    private static final String USAGE =
            "Usage: java -jar dependency-graph-cli.jar [--reverse] [config.yaml]";

    /**
     * Разбирает аргументы командной строки.
     *
     * <p>Допустимы флаг {@code --reverse} и путь к конфигурации, оба
     * необязательные и в любом порядке.
     *
     * @param args аргументы командной строки
     * @return разобранные аргументы
     * @throws ConfigException если аргументы некорректны
     */
    public static CommandLineArguments parse(String[] args) {
        String configPath = null;
        boolean reverse = false;
        for (String arg : args) {
            if (REVERSE_FLAG.equals(arg) && !reverse) {
                reverse = true;
            } else if (!arg.startsWith("-") && configPath == null) {
                configPath = arg;
            } else {
                throw new ConfigException("Invalid argument: " + arg + ". " + USAGE);
            }
        }
        return new CommandLineArguments(
                configPath == null ? DEFAULT_CONFIG_PATH : configPath, reverse);
    }
}
