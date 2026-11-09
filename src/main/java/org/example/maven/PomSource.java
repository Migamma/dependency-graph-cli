package org.example.maven;


public interface PomSource {

    /**
     * Загружает POM-файл пакета.
     *
     * @param coordinates координаты пакета
     * @return содержимое POM-файла в виде XML-текста
     * @throws MavenException если POM-файл не удалось получить
     */
    String fetchPom(MavenCoordinates coordinates);
}
