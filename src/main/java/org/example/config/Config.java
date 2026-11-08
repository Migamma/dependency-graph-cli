package org.example.config;

/**
 * Настройки приложения, прочитанные из YAML-файла конфигурации.
 *
 * @param packageName имя анализируемого пакета: координаты Maven
 *                    {@code groupId:artifactId:version} или, в тестовом режиме,
 *                    имя из заглавных латинских букв (например, {@code A})
 * @param repository  URL-адрес Maven-репозитория или, в тестовом режиме,
 *                    путь к файлу с описанием тестового графа
 * @param outputFile  имя PNG-файла, в который будет сохранено изображение графа
 * @param filter      подстрока: пакеты, в имени которых она встречается,
 *                    не учитываются при анализе; пустая строка означает,
 *                    что фильтр не задан
 * @param testMode    {@code true}, если зависимости читаются из тестового файла,
 *                    а не из настоящего репозитория
 * @param asciiTree   {@code true}, если зависимости нужно вывести на экран
 *                    в виде ASCII-дерева
 * @param maxDepth    максимальная глубина анализа зависимостей (не меньше 1)
 */
public record Config(
        String packageName,
        String repository,
        String outputFile,
        String filter,
        boolean testMode,
        boolean asciiTree,
        int maxDepth) {
}
