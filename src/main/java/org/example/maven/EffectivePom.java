package org.example.maven;

import java.util.Map;

/**
 * POM-файл с учётом всего, что унаследовано от родительских POM.
 *
 * @param coordinates  координаты пакета
 * @param properties   все переменные, включая унаследованные и {@code project.*}
 * @param dependencies зависимости по ключу {@code groupId:artifactId};
 *                     значения ещё содержат переменные
 * @param managed      версии и области действия из {@code dependencyManagement}
 *                     по ключу {@code groupId:artifactId}; переменные уже подставлены
 */
record EffectivePom(
        MavenCoordinates coordinates,
        Map<String, String> properties,
        Map<String, RawDependency> dependencies,
        Map<String, RawDependency> managed) {
}
