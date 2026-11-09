package org.example.graph;

import java.util.List;


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
