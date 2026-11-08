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
import org.example.output.AsciiTreeFormatter;
import org.example.output.D2Generator;
import org.example.output.PngRenderer;
import org.example.output.VisualizationException;

import java.nio.file.Path;
import java.util.List;

/**
 * Точка входа инструмента визуализации графа зависимостей Maven-пакетов.
 *
 * <p>Приложение читает конфигурацию, строит граф зависимостей заданного
 * пакета с учётом транзитивности (из Maven-репозитория или из файла тестового
 * репозитория), выводит его на экран, при необходимости — в виде ASCII-дерева,
 * и сохраняет описание на языке D2 и изображение в PNG-файле.
 * С флагом {@code --reverse} вместо этого выводятся обратные зависимости.
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
            if (arguments.reverse()) {
                print(reverseDependencies(config));
            } else {
                showDependencyGraph(config);
            }
        } catch (ConfigException | DependencySourceException | VisualizationException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(EXIT_ERROR);
        }
    }

    /**
     * Строит граф зависимостей, выводит его и сохраняет диаграмму.
     *
     * @param config настройки приложения
     * @throws DependencySourceException если данные о зависимостях не удалось получить
     * @throws VisualizationException    если изображение не удалось построить
     */
    private static void showDependencyGraph(Config config) {
        DependencyGraph graph = createBuilder(config).build(config.packageName());
        print(GraphFormatter.format(graph));
        if (config.asciiTree()) {
            System.out.println();
            print(AsciiTreeFormatter.format(graph));
        }
        Path png = Path.of(config.outputFile());
        Path d2File = PngRenderer.render(D2Generator.generate(graph), png);
        System.out.println();
        System.out.println("D2 diagram saved to " + d2File);
        System.out.println("PNG image saved to " + png);
    }

    /**
     * Создаёт построитель графа для источника, заданного режимом работы.
     *
     * @param config настройки приложения
     * @return построитель графа
     * @throws DependencySourceException если файл тестового репозитория некорректен
     *                                   или в нём нет анализируемого пакета
     */
    private static GraphBuilder createBuilder(Config config) {
        if (config.testMode()) {
            TestRepository repository = loadTestRepository(config);
            requirePackage(repository, config);
            return new GraphBuilder(repository, config.maxDepth(), config.filter());
        }
        MavenDependencySource source = new MavenDependencySource(
                new MavenDependencyResolver(new HttpPomSource(config.repository())));
        return new GraphBuilder(source, config.maxDepth(), config.filter());
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

    /**
     * Выводит строки на экран.
     *
     * @param lines строки текста
     */
    private static void print(List<String> lines) {
        lines.forEach(System.out::println);
    }
}
