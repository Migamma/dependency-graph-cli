package org.example.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
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
     * Описывает граф зависимостей: зависимости каждого пакета, пропущенные
     * фильтром пакеты, найденные циклы, предупреждения и итоговые числа.
     *
     * @param graph граф зависимостей
     * @return строки текста
     */
    public static List<String> format(DependencyGraph graph) {
        List<String> lines = new ArrayList<>();
        lines.add("Dependency graph of " + graph.root() + ":");
        addEdges(lines, graph, " -> ", "(no dependencies)");
        addDetails(lines, graph, cycleStrings(graph, false));
        lines.add("Packages: " + graph.nodes().size()
                + ", dependencies: " + graph.edgeCount());
        return lines;
    }

    /**
     * Описывает граф обратных зависимостей: какие пакеты зависят от корня
     * (прямо или через другие пакеты) и через какие пакеты проходят эти связи.
     *
     * <p>Граф должен быть построен по перевёрнутому репозиторию, то есть
     * его рёбра ведут от пакета к пакетам, которые от него зависят.
     *
     * @param graph граф обратных зависимостей
     * @return строки текста
     */
    public static List<String> formatReverse(DependencyGraph graph) {
        List<String> lines = new ArrayList<>();
        Set<String> dependents = new LinkedHashSet<>(graph.nodes());
        dependents.remove(graph.root());

        lines.add("Packages that depend on " + graph.root() + ":");
        lines.add(INDENT + (dependents.isEmpty() ? "(none)" : String.join(", ", dependents)));
        lines.add("Reverse dependency paths:");
        addEdges(lines, graph, " <- ", "(no dependents)");
        addDetails(lines, graph, cycleStrings(graph, true));
        lines.add("Packages depending on " + graph.root() + ": " + dependents.size());
        return lines;
    }

    /**
     * Добавляет строку для каждого пакета графа со списком его соседей.
     *
     * @param lines     строки текста
     * @param graph     граф
     * @param arrow     стрелка между пакетом и списком соседей
     * @param emptyText текст для пакета без соседей
     */
    private static void addEdges(List<String> lines, DependencyGraph graph,
                                 String arrow, String emptyText) {
        for (String node : graph.nodes()) {
            lines.add(INDENT + node + arrow + describeNeighbours(graph, node, emptyText));
        }
    }

    /**
     * Добавляет разделы о пропущенных фильтром пакетах, циклах и предупреждениях.
     *
     * @param lines  строки текста
     * @param graph  граф
     * @param cycles циклы, уже записанные строками
     */
    private static void addDetails(List<String> lines, DependencyGraph graph,
                                   List<String> cycles) {
        if (!graph.filtered().isEmpty()) {
            lines.add("Skipped by filter: " + String.join(", ", graph.filtered()));
        }
        addSection(lines, "Cycles", cycles);
        addSection(lines, "Warnings", graph.warnings());
    }

    /**
     * Описывает соседей одного пакета.
     *
     * @param graph     граф
     * @param node      имя пакета
     * @param emptyText текст для пакета без соседей
     * @return соседи через запятую или пояснение, почему их нет
     */
    private static String describeNeighbours(DependencyGraph graph, String node,
                                             String emptyText) {
        if (!graph.isExpanded(node)) {
            return "(not expanded: max depth reached)";
        }
        Set<String> neighbours = graph.dependenciesOf(node);
        if (neighbours.isEmpty()) {
            return emptyText;
        }
        return String.join(", ", neighbours);
    }

    /**
     * Записывает циклы строками вида {@code A -> B -> A}.
     *
     * <p>Циклы графа обратных зависимостей найдены по перевёрнутым рёбрам,
     * поэтому для них порядок пакетов разворачивается, чтобы стрелки
     * показывали обычное направление зависимости.
     *
     * @param graph    граф
     * @param reversed {@code true}, если граф построен по перевёрнутому репозиторию
     * @return циклы в виде строк
     */
    private static List<String> cycleStrings(DependencyGraph graph, boolean reversed) {
        List<String> result = new ArrayList<>();
        for (List<String> cycle : graph.cycles()) {
            List<String> path = reversed ? cycle.reversed() : cycle;
            result.add(String.join(" -> ", path));
        }
        return result;
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
