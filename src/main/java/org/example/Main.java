package org.example;

import org.example.config.Config;
import org.example.config.ConfigException;
import org.example.config.ConfigLoader;
import org.example.maven.Dependency;
import org.example.maven.HttpPomSource;
import org.example.maven.MavenCoordinates;
import org.example.maven.MavenDependencyResolver;
import org.example.maven.MavenException;

import java.util.List;

/**
 * Точка входа инструмента визуализации графа зависимостей Maven-пакетов.
 *
 * <p>На этапе 2 приложение читает конфигурацию, загружает POM-файл заданного
 * пакета из Maven-репозитория и выводит его прямые зависимости.
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
            printDirectDependencies(config);
        } catch (ConfigException | MavenException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);
        }
    }

    /**
     * Загружает и выводит прямые зависимости пакета из конфигурации.
     *
     * @param config настройки приложения
     * @throws ConfigException если включён тестовый режим (он появится на этапе 3)
     * @throws MavenException  если зависимости не удалось получить
     */
    private static void printDirectDependencies(Config config) {
        if (config.testMode()) {
            throw new ConfigException("Test repository mode is not supported yet");
        }
        MavenCoordinates coordinates = MavenCoordinates.parse(config.packageName());
        MavenDependencyResolver resolver =
                new MavenDependencyResolver(new HttpPomSource(config.repository()));
        List<Dependency> dependencies = resolver.getDirectDependencies(coordinates);

        System.out.println("Direct dependencies of " + coordinates + ":");
        if (dependencies.isEmpty()) {
            System.out.println("  (none)");
        }
        for (Dependency dependency : dependencies) {
            System.out.println("  " + dependency);
        }
    }
}
