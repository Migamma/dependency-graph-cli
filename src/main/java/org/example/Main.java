package org.example;

import org.example.config.Config;
import org.example.config.ConfigException;
import org.example.config.ConfigLoader;
import org.example.graph.DependencyGraph;
import org.example.graph.DependencySource;
import org.example.graph.DependencySourceException;
import org.example.graph.GraphBuilder;
import org.example.graph.GraphFormatter;
import org.example.graph.TestRepository;
import org.example.maven.HttpPomSource;
import org.example.maven.MavenDependencyResolver;
import org.example.maven.MavenDependencySource;

import java.nio.file.Path;

/**
 * Точка входа инструмента визуализации графа зависимостей Maven-пакетов.
 *
 * <p>На этапе 3 приложение читает конфигурацию, строит граф зависимостей
 * заданного пакета с учётом транзитивности (из Maven-репозитория или из файла
 * тестового репозитория) и выводит его на экран.
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
            DependencySource source = createSource(config);
            GraphBuilder builder =
                    new GraphBuilder(source, config.maxDepth(), config.filter());
            DependencyGraph graph = builder.build(config.packageName());
            GraphFormatter.format(graph).forEach(System.out::println);
        } catch (ConfigException | DependencySourceException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);
        }
    }

    /**
     * Создаёт источник зависимостей в зависимости от режима работы.
     *
     * @param config настройки приложения
     * @return тестовый репозиторий из файла или настоящий Maven-репозиторий
     * @throws DependencySourceException если файл тестового репозитория
     *                                   некорректен или в нём нет анализируемого пакета
     */
    private static DependencySource createSource(Config config) {
        if (!config.testMode()) {
            return new MavenDependencySource(
                    new MavenDependencyResolver(new HttpPomSource(config.repository())));
        }
        TestRepository repository = TestRepository.load(Path.of(config.repository()));
        if (!repository.contains(config.packageName())) {
            throw new DependencySourceException("Package " + config.packageName()
                    + " not found in test repository " + config.repository());
        }
        return repository;
    }
}
