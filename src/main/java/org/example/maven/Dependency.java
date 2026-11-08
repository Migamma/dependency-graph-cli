package org.example.maven;

/**
 * Прямая зависимость пакета.
 *
 * @param coordinates координаты пакета-зависимости
 * @param scope       область действия зависимости ({@code compile} или {@code runtime})
 */
public record Dependency(MavenCoordinates coordinates, String scope) {

    /**
     * Возвращает зависимость в виде {@code groupId:artifactId:version (scope)}.
     *
     * @return строковая запись зависимости
     */
    @Override
    public String toString() {
        return coordinates + " (" + scope + ")";
    }
}
