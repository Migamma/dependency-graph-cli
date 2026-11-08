package org.example.maven;

/**
 * Координаты Maven-пакета: группа, имя артефакта и версия.
 *
 * @param groupId    группа, например {@code com.google.guava}
 * @param artifactId имя артефакта, например {@code guava}
 * @param version    версия, например {@code 33.0.0-jre}
 */
public record MavenCoordinates(String groupId, String artifactId, String version) {

    /** Число частей в строковой записи координат. */
    private static final int PARTS = 3;

    /**
     * Разбирает координаты из строки вида {@code groupId:artifactId:version}.
     *
     * @param text строка с координатами
     * @return координаты пакета
     * @throws MavenException если строка имеет неверный формат
     */
    public static MavenCoordinates parse(String text) {
        String[] parts = text.split(":", -1);
        if (parts.length != PARTS) {
            throw new MavenException("Invalid Maven coordinates: " + text);
        }
        for (String part : parts) {
            if (part.isBlank()) {
                throw new MavenException("Invalid Maven coordinates: " + text);
            }
        }
        return new MavenCoordinates(parts[0].trim(), parts[1].trim(), parts[2].trim());
    }

    /**
     * Возвращает путь к POM-файлу пакета относительно корня репозитория.
     *
     * <p>Например, для {@code com.google.guava:guava:33.0.0-jre} это
     * {@code com/google/guava/guava/33.0.0-jre/guava-33.0.0-jre.pom}.
     *
     * @return относительный путь к POM-файлу
     */
    public String pomPath() {
        return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/"
                + artifactId + "-" + version + ".pom";
    }

    /**
     * Возвращает координаты в виде {@code groupId:artifactId:version}.
     *
     * @return строковая запись координат
     */
    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
