package org.example.maven;

import java.util.List;
import java.util.Map;

/**
 * Содержимое одного POM-файла, нужное для поиска зависимостей.
 *
 * @param parent       координаты родительского POM или {@code null}, если его нет
 * @param properties   переменные из секции {@code properties}
 * @param dependencies зависимости из секции {@code dependencies}
 * @param managed      зависимости из секции {@code dependencyManagement}
 */
public record PomModel(
        MavenCoordinates parent,
        Map<String, String> properties,
        List<RawDependency> dependencies,
        List<RawDependency> managed) {
}
