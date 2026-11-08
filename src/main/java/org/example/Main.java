package org.example;

import org.example.config.Config;
import org.example.config.ConfigException;
import org.example.config.ConfigLoader;
import org.example.graph.DependencyGraph;
import org.example.graph.DependencySourceException;
import org.example.graph.GraphBuilder;
import org.example.graph.GraphFormatter;
import org.example.graph.TestRepository;
import org.example.maven.HttpPomSource;
import org.example.maven.MavenDependencyResolver;
import org.example.maven.MavenDependencySource;

import java.nio.file.Path;
import java.util.List;

/**
 * Точка входа инструмента визуализации графа зависимостей Maven-пакетов.
 *
 * <p>Приложение читает конфигурацию и строит граф зависимостей заданного
 * пакета с учётом транзитивности (из Maven-репозитория или из файла тестового
 * репозитория). С флагом {@code --reverse} вместо этого выводятся обратные
 * зависимости — пакеты тестового репозитория, которые зависят от заданного.
 */
public final class Main {

    /** Код возврата при ошибке. */
    private static final int EXIT_ERROR = 1;

    /** Класс содержит только статические методы, создавать его не нужно. */
    private Main() {
    }

    /**
     * Запускает приложение.
     *
     * @param args необязательные флаг {@code --reverse} и путь к YAML-файлу
     *             конфигурации; по умолчанию используется {@code config.yaml}
     */
    public static void main(String[] args) {
        try {
            CommandLineArguments arguments = CommandLineArguments.parse(args);
            Config config = new ConfigLoader().load(arguments.configPath());
            List<String> output = arguments.reverse()
                    ? reverseDependencies(config)
                    : dependencyGraph(config);
            output.forEach(System.out::println);
        } catch (ConfigException | DependencySourceException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);
        }
    }

    /**
     * Строит граф зависимостей пакета и описывает его.
     *
     * @param config настройки приложения
     * @return строки для вывода на экран
     * @throws DependencySourceException если данные о зависимостях не удалось получить
     */
    private static List<String> dependencyGraph(Config config) {
        GraphBuilder builder;
        if (config.testMode()) {
            TestRepository repository = loadTestRepository(config);
            requirePackage(repository, config);
            builder = new GraphBuilder(repository, config.maxDepth(), config.filter());
        } else {
            MavenDependencySource source = new MavenDependencySource(
                    new MavenDependencyResolver(new HttpPomSource(config.repository())));
            builder = new GraphBuilder(source, config.maxDepth(), config.filter());
        }
        return GraphFormatter.format(builder.build(config.packageName()));
    }

    /**
     * Находит обратные зависимости пакета тем же обходом в глубину,
     * но по перевёрнутому тестовому репозиторию.
     *
     * @param config настройки приложения
     * @return строки для вывода на экран
     * @throws ConfigException           если тестовый режим выключен
     * @throws DependencySourceException если файл тестового репозитория некорректен
     *                                   или в нём нет заданного пакета
     */
    private static List<String> reverseDependencies(Config config) {
        if (!config.testMode()) {
            throw new ConfigException(
                    "Reverse dependencies mode requires testMode: true");
        }
        TestRepository reversed = loadTestRepository(config).reversed();
        requirePackage(reversed, config);
        GraphBuilder builder =
                new GraphBuilder(reversed, config.maxDepth(), config.filter());
        DependencyGraph graph = builder.build(config.packageName());
        return GraphFormatter.formatReverse(graph);
    }

    /**
     * Загружает тестовый репозиторий, указанный в конфигурации.
     *
     * @param config настройки приложения
     * @return тестовый репозиторий
     * @throws DependencySourceException если файл некорректен
     */
    private static TestRepository loadTestRepository(Config config) {
        return TestRepository.load(Path.of(config.repository()));
    }

    /**
     * Проверяет, что анализируемый пакет есть в тестовом репозитории.
     *
     * @param repository тестовый репозиторий
     * @param config     настройки приложения
     * @throws DependencySourceException если пакета нет в репозитории
     */
    private static void requirePackage(TestRepository repository, Config config) {
        if (!repository.contains(config.packageName())) {
            throw new DependencySourceException("Package " + config.packageName()
                    + " not found in test repository " + config.repository());
        }
    }
}
