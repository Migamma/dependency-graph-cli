package org.example.graph;

import java.util.List;

/**
 * Источник данных о зависимостях: по имени пакета возвращает имена
 * его прямых зависимостей.
 *
 * <p>Обход графа не знает, откуда берутся данные: из настоящего
 * Maven-репозитория или из файла тестового репозитория.
 */
public interface DependencySource {

    /**
     * Возвращает прямые зависимости пакета.
     *
     * @param packageName имя пакета
     * @return имена прямых зависимостей в порядке их объявления
     * @throws DependencySourceException если данные о пакете не удалось получить
     */
    List<String> getDependencies(String packageName);
}
