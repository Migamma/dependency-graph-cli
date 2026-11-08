package org.example.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Проверяет значения параметров конфигурации.
 *
 * <p>Типы параметров к этому моменту уже проверены в {@link ConfigLoader};
 * здесь проверяется смысл значений: формат имени пакета, адрес репозитория,
 * расширение выходного файла и допустимая глубина анализа.
 */
public class ConfigValidator {

    /** Минимально допустимая глубина анализа зависимостей. */
    private static final int MIN_DEPTH = 1;
    /** Число частей в координатах Maven: groupId, artifactId и version. */
    private static final int MAVEN_COORDINATE_PARTS = 3;
    /** Обязательное расширение выходного файла. */
    private static final String PNG_EXTENSION = ".png";
    /** Имя пакета в тестовом репозитории: одна или несколько заглавных латинских букв. */
    private static final Pattern TEST_PACKAGE_NAME = Pattern.compile("[A-Z]+");

    /**
     * Создаёт проверяющий объект для параметров конфигурации.
     */
    public ConfigValidator() {
    }

    /**
     * Проверяет все параметры конфигурации.
     *
     * @param config настройки для проверки
     * @throws ConfigException если какой-либо параметр имеет недопустимое значение
     */
    public void validate(Config config) {
        validatePackageName(config.packageName(), config.testMode());
        validateRepository(config.repository(), config.testMode());
        validateOutputFile(config.outputFile());
        validateMaxDepth(config.maxDepth());
    }

    /**
     * Проверяет имя анализируемого пакета.
     *
     * <p>В тестовом режиме имя должно состоять из заглавных латинских букв,
     * в обычном — иметь вид {@code groupId:artifactId:version}.
     *
     * @param packageName имя пакета
     * @param testMode    включён ли тестовый режим
     * @throws ConfigException если имя пустое или имеет неверный формат
     */
    private void validatePackageName(String packageName, boolean testMode) {
        if (packageName.isBlank()) {
            throw new ConfigException("Parameter packageName must not be empty");
        }
        if (testMode) {
            if (!TEST_PACKAGE_NAME.matcher(packageName).matches()) {
                throw new ConfigException("In test mode parameter packageName "
                        + "must consist of uppercase Latin letters, e.g. A");
            }
            return;
        }
        String[] parts = packageName.split(":", -1);
        if (parts.length != MAVEN_COORDINATE_PARTS || hasBlankPart(parts)) {
            throw new ConfigException("Parameter packageName must have format "
                    + "groupId:artifactId:version, got: " + packageName);
        }
    }

    /**
     * Проверяет, есть ли среди частей координат пустые.
     *
     * @param parts части координат Maven
     * @return {@code true}, если хотя бы одна часть пустая
     */
    private boolean hasBlankPart(String[] parts) {
        for (String part : parts) {
            if (part.isBlank()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет адрес репозитория.
     *
     * <p>В тестовом режиме это должен быть путь к существующему файлу,
     * в обычном — URL со схемой http или https.
     *
     * @param repository URL репозитория или путь к тестовому файлу
     * @param testMode   включён ли тестовый режим
     * @throws ConfigException если адрес пустой или недопустимый
     */
    private void validateRepository(String repository, boolean testMode) {
        if (repository.isBlank()) {
            throw new ConfigException("Parameter repository must not be empty");
        }
        if (testMode) {
            validateTestRepositoryFile(repository);
        } else {
            validateRepositoryUrl(repository);
        }
    }

    /**
     * Проверяет, что файл тестового репозитория существует.
     *
     * @param repository путь к файлу тестового репозитория
     * @throws ConfigException если путь некорректен или файл не найден
     */
    private void validateTestRepositoryFile(String repository) {
        try {
            if (!Files.isRegularFile(Path.of(repository))) {
                throw new ConfigException(
                        "Test repository file not found: " + repository);
            }
        } catch (InvalidPathException e) {
            throw new ConfigException("Invalid test repository path: " + repository);
        }
    }

    /**
     * Проверяет, что адрес репозитория — корректный URL со схемой http или https.
     *
     * @param repository URL репозитория
     * @throws ConfigException если URL некорректен
     */
    private void validateRepositoryUrl(String repository) {
        String message = "Parameter repository must be an http(s) URL, got: "
                + repository;
        try {
            URI uri = new URI(repository);
            String scheme = uri.getScheme();
            boolean isHttp = "http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme);
            if (!isHttp || uri.getHost() == null) {
                throw new ConfigException(message);
            }
        } catch (URISyntaxException e) {
            throw new ConfigException(message);
        }
    }

    /**
     * Проверяет имя выходного файла с изображением графа.
     *
     * @param outputFile имя выходного файла
     * @throws ConfigException если имя пустое или не заканчивается на .png
     */
    private void validateOutputFile(String outputFile) {
        String lowerCaseName = outputFile.toLowerCase(Locale.ROOT);
        if (!lowerCaseName.endsWith(PNG_EXTENSION)
                || lowerCaseName.equals(PNG_EXTENSION)) {
            throw new ConfigException("Parameter outputFile must be a file name "
                    + "ending with .png, got: " + outputFile);
        }
    }

    /**
     * Проверяет максимальную глубину анализа.
     *
     * @param maxDepth максимальная глубина
     * @throws ConfigException если глубина меньше 1
     */
    private void validateMaxDepth(int maxDepth) {
        if (maxDepth < MIN_DEPTH) {
            throw new ConfigException("Parameter maxDepth must be at least "
                    + MIN_DEPTH + ", got: " + maxDepth);
        }
    }
}
