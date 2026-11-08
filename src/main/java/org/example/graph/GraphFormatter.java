package org.example.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Превращает граф зависимостей в текст для вывода на экран.
 */
public final class GraphFormatter {

    /** Отступ строк внутри разделов. */
    private static final String INDENT = "  ";

    /** Класс содержит только статические методы, создавать его не нужно. */
    private GraphFormatter() {
    }

    /**
     * Описывает граф: зависимости каждого пакета, пропущенные фильтром
     * пакеты, найденные циклы, предупреждения и итоговые числа.
     *
     * @param graph граф зависимостей
     * @return строки текста
     */
    public static List<String> format(DependencyGraph graph) {
        List<String> lines = new ArrayList<>();
        lines.add("Dependency graph of " + graph.root() + ":");
        for (String node : graph.nodes()) {
            lines.add(INDENT + node + " -> " + describeDependencies(graph, node));
        }
        if (!graph.filtered().isEmpty()) {
            lines.add("Skipped by filter: " + String.join(", ", graph.filtered()));
        }
        addSection(lines, "Cycles", graph.cycles());
        addSection(lines, "Warnings", graph.warnings());
        lines.add("Packages: " + graph.nodes().size()
                + ", dependencies: " + graph.edgeCount());
        return lines;
    }

    /**
     * Описывает прямые зависимости одного пакета.
     *
     * @param graph граф зависимостей
     * @param node  имя пакета
     * @return список зависимостей через запятую или пояснение, почему их нет
     */
    private static String describeDependencies(DependencyGraph graph, String node) {
        if (!graph.isExpanded(node)) {
            return "(not expanded: max depth reached)";
        }
        Set<String> dependencies = graph.dependenciesOf(node);
        if (dependencies.isEmpty()) {
            return "(no dependencies)";
        }
        return String.join(", ", dependencies);
    }

    /**
     * Добавляет раздел со списком строк; пустой раздел выводится как none.
     *
     * @param lines  строки текста, в которые добавляется раздел
     * @param title  заголовок раздела
     * @param values строки раздела
     */
    private static void addSection(List<String> lines, String title,
                                   Collection<String> values) {
        if (values.isEmpty()) {
            lines.add(title + ": none");
            return;
        }
        lines.add(title + ":");
        for (String value : values) {
            lines.add(INDENT + value);
        }
    }
}
