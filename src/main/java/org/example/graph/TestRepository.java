package org.example.graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Тестовый репозиторий: граф зависимостей, описанный в текстовом файле.
 *
 * <p>Каждая строка файла имеет вид {@code A: B C} и означает, что пакет
 * {@code A} зависит от пакетов {@code B} и {@code C}. Пакеты называются
 * заглавными латинскими буквами, пустые строки пропускаются. Пакет,
 * для которого нет своей строки, считается пакетом без зависимостей.
 */
public final class TestRepository implements DependencySource {

    /** Допустимое имя пакета: одна или несколько заглавных латинских букв. */
    private static final Pattern PACKAGE_NAME = Pattern.compile("[A-Z]+");
    /** Разделитель между именем пакета и списком его зависимостей. */
    private static final char SEPARATOR = ':';

    /** Прямые зависимости каждого пакета, описанного в файле. */
    private final Map<String, List<String>> dependencies;

    /**
     * Создаёт репозиторий из готового описания графа.
     *
     * @param dependencies прямые зависимости каждого пакета
     */
    private TestRepository(Map<String, List<String>> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Загружает тестовый репозиторий из файла.
     *
     * @param path путь к файлу с описанием графа
     * @return тестовый репозиторий
     * @throws DependencySourceException если файл не читается или имеет неверный формат
     */
    public static TestRepository load(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new DependencySourceException("Cannot read test repository: " + path);
        }
        Map<String, List<String>> dependencies = new HashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            parseLine(lines.get(index), index + 1, path, dependencies);
        }
        return new TestRepository(dependencies);
    }

    /**
     * Разбирает одну строку файла и добавляет описанный в ней пакет.
     *
     * @param line         строка файла
     * @param lineNumber   номер строки (с единицы) для сообщений об ошибках
     * @param path         путь к файлу для сообщений об ошибках
     * @param dependencies описание графа, в которое добавляется пакет
     * @throws DependencySourceException если строка имеет неверный формат
     *                                   или пакет описан повторно
     */
    private static void parseLine(String line, int lineNumber, Path path,
                                  Map<String, List<String>> dependencies) {
        String text = line.strip();
        if (text.isEmpty()) {
            return;
        }
        int separator = text.indexOf(SEPARATOR);
        if (separator < 0) {
            throw lineError(path, lineNumber, "expected format 'A: B C'");
        }
        String name = text.substring(0, separator).strip();
        checkName(name, path, lineNumber);
        if (dependencies.containsKey(name)) {
            throw lineError(path, lineNumber, "package " + name + " is described twice");
        }
        String rest = text.substring(separator + 1).strip();
        List<String> names = rest.isEmpty() ? List.of() : Arrays.asList(rest.split("\\s+"));
        for (String dependency : names) {
            checkName(dependency, path, lineNumber);
        }
        dependencies.put(name, List.copyOf(names));
    }

    /**
     * Проверяет, что имя пакета состоит из заглавных латинских букв.
     *
     * @param name       имя пакета
     * @param path       путь к файлу для сообщений об ошибках
     * @param lineNumber номер строки для сообщений об ошибках
     * @throws DependencySourceException если имя недопустимо
     */
    private static void checkName(String name, Path path, int lineNumber) {
        if (!PACKAGE_NAME.matcher(name).matches()) {
            throw lineError(path, lineNumber, "invalid package name '" + name
                    + "', expected uppercase Latin letters");
        }
    }

    /**
     * Создаёт исключение об ошибке в строке файла.
     *
     * @param path       путь к файлу
     * @param lineNumber номер строки
     * @param problem    описание ошибки
     * @return исключение с понятным сообщением
     */
    private static DependencySourceException lineError(Path path, int lineNumber,
                                                       String problem) {
        return new DependencySourceException("Invalid test repository " + path
                + ", line " + lineNumber + ": " + problem);
    }

    /**
     * Проверяет, описан ли пакет в репозитории отдельной строкой.
     *
     * @param packageName имя пакета
     * @return {@code true}, если пакет описан в файле
     */
    public boolean contains(String packageName) {
        return dependencies.containsKey(packageName);
    }

    /**
     * Возвращает прямые зависимости пакета.
     *
     * @param packageName имя пакета
     * @return прямые зависимости или пустой список, если пакет не описан
     */
    @Override
    public List<String> getDependencies(String packageName) {
        return dependencies.getOrDefault(packageName, List.of());
    }
}
