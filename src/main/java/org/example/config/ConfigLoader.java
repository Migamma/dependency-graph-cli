package org.example.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Загружает настройки приложения из YAML-файла.
 *
 * <p>Читает файл, проверяет наличие и типы всех параметров, а затем передаёт
 * готовый объект {@link Config} в {@link ConfigValidator} для проверки значений.
 * О любой проблеме сообщает исключением {@link ConfigException}.
 */
public class ConfigLoader {

    /** Ключ параметра с именем анализируемого пакета. */
    private static final String PACKAGE_NAME = "packageName";
    /** Ключ параметра с URL репозитория или путём к тестовому файлу. */
    private static final String REPOSITORY = "repository";
    /** Ключ параметра с именем выходного PNG-файла. */
    private static final String OUTPUT_FILE = "outputFile";
    /** Ключ параметра с подстрокой для фильтрации пакетов. */
    private static final String FILTER = "filter";
    /** Ключ параметра, включающего тестовый режим. */
    private static final String TEST_MODE = "testMode";
    /** Ключ параметра, включающего вывод ASCII-дерева. */
    private static final String ASCII_TREE = "asciiTree";
    /** Ключ параметра с максимальной глубиной анализа. */
    private static final String MAX_DEPTH = "maxDepth";

    /** Все допустимые ключи конфигурации; любой другой ключ считается ошибкой. */
    private static final List<String> KNOWN_KEYS = List.of(
            PACKAGE_NAME, REPOSITORY, OUTPUT_FILE, FILTER,
            TEST_MODE, ASCII_TREE, MAX_DEPTH);

    /** Проверяет значения параметров после того, как они прочитаны. */
    private final ConfigValidator validator = new ConfigValidator();

    /**
     * Создаёт загрузчик конфигурации.
     */
    public ConfigLoader() {
    }

    /**
     * Читает и проверяет файл конфигурации.
     *
     * @param path путь к YAML-файлу конфигурации
     * @return настройки приложения, прошедшие все проверки
     * @throws ConfigException если файл не читается, повреждён или содержит
     *                         отсутствующие либо недопустимые параметры
     */
    public Config load(String path) {
        Map<String, Object> data = readYaml(path);
        checkUnknownKeys(data);

        String packageName = getString(data, PACKAGE_NAME);
        String repository = getString(data, REPOSITORY);
        String outputFile = getString(data, OUTPUT_FILE);
        String filter = getOptionalString(data, FILTER);
        boolean testMode = getBoolean(data, TEST_MODE);
        boolean asciiTree = getBoolean(data, ASCII_TREE);
        int maxDepth = getInteger(data, MAX_DEPTH);

        Config config = new Config(packageName, repository, outputFile, filter,
                testMode, asciiTree, maxDepth);
        validator.validate(config);
        return config;
    }

    /**
     * Читает YAML-файл и возвращает его содержимое как набор пар ключ-значение.
     *
     * <p>Используется безопасный конструктор SnakeYAML (он не создаёт
     * произвольные Java-объекты), а повторяющиеся ключи запрещены.
     *
     * @param path путь к YAML-файлу
     * @return содержимое файла в виде {@code Map}
     * @throws ConfigException если файл не найден, не читается, повреждён
     *                         или не содержит пар ключ-значение
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readYaml(String path) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));

        try (InputStream inputStream = Files.newInputStream(Path.of(path))) {
            Object data = yaml.load(inputStream);
            if (!(data instanceof Map)) {
                throw new ConfigException(
                        "Config must contain key-value pairs: " + path);
            }
            return (Map<String, Object>) data;
        } catch (IOException | InvalidPathException e) {
            throw new ConfigException("Cannot read file: " + path);
        } catch (YAMLException e) {
            throw new ConfigException("Invalid YAML in file: " + path);
        }
    }

    /**
     * Проверяет, что в конфигурации нет лишних ключей (например, с опечаткой).
     *
     * @param data содержимое файла конфигурации
     * @throws ConfigException если найден неизвестный ключ
     */
    private void checkUnknownKeys(Map<String, Object> data) {
        for (Object key : data.keySet()) {
            if (!KNOWN_KEYS.contains(key)) {
                throw new ConfigException("Unknown parameter: " + key
                        + ". Allowed parameters: " + String.join(", ", KNOWN_KEYS));
            }
        }
    }

    /**
     * Возвращает значение обязательного параметра.
     *
     * @param data содержимое файла конфигурации
     * @param key  имя параметра
     * @return значение параметра (может быть {@code null}, если оно не указано)
     * @throws ConfigException если параметр отсутствует
     */
    private Object requireValue(Map<String, Object> data, String key) {
        if (!data.containsKey(key)) {
            throw new ConfigException("Missing parameter: " + key);
        }
        return data.get(key);
    }

    /**
     * Возвращает обязательный строковый параметр.
     *
     * @param data содержимое файла конфигурации
     * @param key  имя параметра
     * @return строковое значение параметра
     * @throws ConfigException если параметр отсутствует или не является строкой
     */
    private String getString(Map<String, Object> data, String key) {
        Object value = requireValue(data, key);
        if (!(value instanceof String text)) {
            throw new ConfigException("Parameter " + key + " must be a string");
        }
        return text;
    }

    /**
     * Возвращает необязательный строковый параметр.
     *
     * @param data содержимое файла конфигурации
     * @param key  имя параметра
     * @return значение параметра или пустая строка, если он не указан
     * @throws ConfigException если параметр указан, но не является строкой
     */
    private String getOptionalString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return "";
        }
        if (!(value instanceof String text)) {
            throw new ConfigException("Parameter " + key + " must be a string");
        }
        return text;
    }

    /**
     * Возвращает обязательный логический параметр.
     *
     * @param data содержимое файла конфигурации
     * @param key  имя параметра
     * @return значение параметра
     * @throws ConfigException если параметр отсутствует или не равен true/false
     */
    private boolean getBoolean(Map<String, Object> data, String key) {
        Object value = requireValue(data, key);
        if (!(value instanceof Boolean flag)) {
            throw new ConfigException(
                    "Parameter " + key + " must be true or false");
        }
        return flag;
    }

    /**
     * Возвращает обязательный целочисленный параметр.
     *
     * @param data содержимое файла конфигурации
     * @param key  имя параметра
     * @return значение параметра
     * @throws ConfigException если параметр отсутствует или не является целым числом
     */
    private int getInteger(Map<String, Object> data, String key) {
        Object value = requireValue(data, key);
        if (!(value instanceof Integer number)) {
            throw new ConfigException("Parameter " + key + " must be an integer");
        }
        return number;
    }
}
