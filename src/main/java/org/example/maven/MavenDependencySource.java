package org.example.maven;

import org.example.graph.DependencySource;
import org.example.graph.DependencySourceException;

import java.util.ArrayList;
import java.util.List;

/**
 * Источник зависимостей на основе настоящего Maven-репозитория.
 *
 * <p>Пакеты называются координатами вида {@code groupId:artifactId:version}.
 */
public class MavenDependencySource implements DependencySource {

    /** Резолвер прямых зависимостей Maven-пакетов. */
    private final MavenDependencyResolver resolver;

    /**
     * Создаёт источник зависимостей.
     *
     * @param resolver резолвер прямых зависимостей Maven-пакетов
     */
    public MavenDependencySource(MavenDependencyResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Возвращает прямые зависимости Maven-пакета.
     *
     * @param packageName координаты пакета {@code groupId:artifactId:version}
     * @return координаты прямых зависимостей
     * @throws DependencySourceException если POM-файл не удалось получить или разобрать
     */
    @Override
    public List<String> getDependencies(String packageName) {
        try {
            List<String> result = new ArrayList<>();
            MavenCoordinates coordinates = MavenCoordinates.parse(packageName);
            for (Dependency dependency : resolver.getDirectDependencies(coordinates)) {
                result.add(dependency.coordinates().toString());
            }
            return result;
        } catch (MavenException e) {
            throw new DependencySourceException(e.getMessage());
        }
    }
}
