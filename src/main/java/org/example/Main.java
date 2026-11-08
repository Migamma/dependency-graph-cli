package org.example;

import org.example.config.Config;
import org.example.config.ConfigException;
import org.example.config.ConfigLoader;

/**
 * Точка входа инструмента визуализации графа зависимостей Maven-пакетов.
 *
 * <p>На этапе 1 приложение читает конфигурацию, проверяет её и выводит
 * все параметры в формате ключ-значение.
 */
public final class Main {

    /** Путь к конфигурации, если он не передан аргументом. */
    private static final String DEFAULT_CONFIG_PATH = "config.yaml";
    /** Максимальное число аргументов командной строки. */
    private static final int MAX_ARGUMENTS = 1;
    /** Код возврата при ошибке. */
    private static final int EXIT_ERROR = 1;

    /** Класс содержит только статические методы, создавать его не нужно. */
    private Main() {
    }

    /**
     * Запускает приложение.
     *
     * @param args необязательный путь к YAML-файлу конфигурации;
     *             по умолчанию используется {@code config.yaml}
     */
    public static void main(String[] args) {
        if (args.length > MAX_ARGUMENTS) {
            System.err.println("Usage: java -jar dependency-graph-cli.jar [config.yaml]");
            System.exit(EXIT_ERROR);
        }
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;

        try {
            Config config = new ConfigLoader().load(configPath);
            printConfig(config);
        } catch (ConfigException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);
        }
    }

    /**
     * Выводит все параметры конфигурации в формате ключ-значение.
     *
     * @param config настройки приложения
     */
    private static void printConfig(Config config) {
        System.out.println("packageName: " + config.packageName());
        System.out.println("repository: " + config.repository());
        System.out.println("outputFile: " + config.outputFile());
        System.out.println("filter: " + config.filter());
        System.out.println("testMode: " + config.testMode());
        System.out.println("asciiTree: " + config.asciiTree());
        System.out.println("maxDepth: " + config.maxDepth());
    }
}
