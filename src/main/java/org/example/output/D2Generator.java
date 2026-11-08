package org.example.output;

import org.example.graph.DependencyGraph;

/**
 * Формирует текстовое описание графа зависимостей на языке диаграмм D2.
 *
 * <p>Каждый пакет становится узлом, каждая зависимость — стрелкой
 * {@code "A" -> "B"}. Имена узлов берутся в кавычки, потому что в D2
 * точка в имени без кавычек означает вложенность, а имена Maven-пакетов
 * содержат точки и двоеточия.
 */
public final class D2Generator {

    /** Класс содержит только статические методы, создавать его не нужно. */
    private D2Generator() {
    }

    /**
     * Описывает граф на языке D2.
     *
     * @param graph граф зависимостей
     * @return текст диаграммы
     */
    public static String generate(DependencyGraph graph) {
        StringBuilder text = new StringBuilder();
        text.append("direction: down\n\n");
        for (String node : graph.nodes()) {
            text.append(quote(node)).append('\n');
        }
        text.append('\n');
        for (String node : graph.nodes()) {
            for (String dependency : graph.dependenciesOf(node)) {
                text.append(quote(node)).append(" -> ").append(quote(dependency)).append('\n');
            }
        }
        return text.toString();
    }

    /**
     * Берёт имя узла в двойные кавычки, экранируя обратную косую черту
     * и кавычки внутри имени.
     *
     * @param name имя узла
     * @return имя в кавычках
     */
    private static String quote(String name) {
        return "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
