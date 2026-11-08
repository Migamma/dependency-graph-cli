package org.example.maven;

import java.util.Map;

/**
 * Зависимость в том виде, в каком она записана в POM-файле.
 *
 * <p>Любое поле может быть {@code null}, если оно не указано, и может
 * содержать переменные вида {@code ${name}}.
 *
 * @param groupId    группа пакета
 * @param artifactId имя артефакта
 * @param version    версия
 * @param scope      область действия ({@code compile}, {@code test} и т. д.)
 * @param type       тип артефакта ({@code jar}, {@code pom} и т. д.)
 * @param optional   значение тега {@code optional}
 */
public record RawDependency(
        String groupId,
        String artifactId,
        String version,
        String scope,
        String type,
        String optional) {

    /**
     * Возвращает ключ зависимости {@code groupId:artifactId}, по которому
     * она сопоставляется с секцией {@code dependencyManagement}.
     *
     * @return ключ зависимости
     */
    public String key() {
        return groupId + ":" + artifactId;
    }

    /**
     * Подставляет значения переменных во все поля.
     *
     * @param properties значения переменных
     * @return зависимость с подставленными значениями
     */
    public RawDependency resolve(Map<String, String> properties) {
        return new RawDependency(
                Placeholders.resolve(groupId, properties),
                Placeholders.resolve(artifactId, properties),
                Placeholders.resolve(version, properties),
                Placeholders.resolve(scope, properties),
                Placeholders.resolve(type, properties),
                Placeholders.resolve(optional, properties));
    }
}
