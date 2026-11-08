package org.example.output;

import org.example.graph.DependencyGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Выводит граф зависимостей в виде ASCII-дерева, как команда {@code tree}.
 *
 * <p>Используются только ASCII-символы, как в выводе {@code mvn dependency:tree}.
 * Дерево строится без рекурсии, с явным стеком. Пакет, который уже был
 * показан выше, повторно не раскрывается и помечается {@code (*)}. Зависимость,
 * которая ведёт обратно к пакету на текущем пути, помечается {@code (cycle)}.
 * Пакет, не раскрытый из-за ограничения глубины, помечается {@code (max depth)}.
 */
public final class AsciiTreeFormatter {

    /** Соединитель для элемента, после которого на этом уровне есть ещё элементы. */
    private static final String BRANCH = "+-- ";
    /** Соединитель для последнего элемента уровня. */
    private static final String LAST_BRANCH = "\\-- ";
    /** Продолжение вертикальной линии для вложенных уровней. */
    private static final String VERTICAL = "|   ";
    /** Отступ под последним элементом уровня. */
    private static final String SPACE = "    ";

    /** Класс содержит только статические методы, создавать его не нужно. */
    private AsciiTreeFormatter() {
    }

    /**
     * Строит ASCII-дерево зависимостей от корня графа.
     *
     * @param graph граф зависимостей
     * @return строки дерева и строка с пояснением пометок
     */
    public static List<String> format(DependencyGraph graph) {
        List<String> lines = new ArrayList<>();
        Set<String> shown = new HashSet<>();
        Deque<Entry> stack = new ArrayDeque<>();
        Entry root = new Entry(graph.root(), null, "", true);

        lines.add(graph.root());
        shown.add(graph.root());
        pushChildren(graph, root, "", stack);
        while (!stack.isEmpty()) {
            Entry entry = stack.pop();
            String marker = marker(graph, entry, shown);
            lines.add(entry.prefix + (entry.last ? LAST_BRANCH : BRANCH) + entry.node + marker);
            if (marker.isEmpty()) {
                shown.add(entry.node);
                String childPrefix = entry.prefix + (entry.last ? SPACE : VERTICAL);
                pushChildren(graph, entry, childPrefix, stack);
            }
        }
        lines.add("(*) already shown above, (cycle) cyclic dependency, "
                + "(max depth) not expanded");
        return lines;
    }

    /**
     * Кладёт в стек зависимости пакета в обратном порядке, чтобы они
     * доставались из стека в порядке объявления.
     *
     * @param graph  граф зависимостей
     * @param parent элемент дерева с пакетом-родителем
     * @param prefix отступ строк для зависимостей
     * @param stack  стек обхода
     */
    private static void pushChildren(DependencyGraph graph, Entry parent, String prefix,
                                     Deque<Entry> stack) {
        List<String> children = new ArrayList<>(graph.dependenciesOf(parent.node));
        for (int index = children.size() - 1; index >= 0; index--) {
            boolean last = index == children.size() - 1;
            stack.push(new Entry(children.get(index), parent, prefix, last));
        }
    }

    /**
     * Выбирает пометку для элемента дерева.
     *
     * @param graph граф зависимостей
     * @param entry элемент дерева
     * @param shown пакеты, которые уже были раскрыты выше
     * @return пометка или пустая строка, если элемент нужно раскрыть
     */
    private static String marker(DependencyGraph graph, Entry entry, Set<String> shown) {
        if (isAncestor(entry.node, entry.parent)) {
            return " (cycle)";
        }
        if (shown.contains(entry.node)) {
            return " (*)";
        }
        if (!graph.isExpanded(entry.node)) {
            return " (max depth)";
        }
        return "";
    }

    /**
     * Проверяет, встречается ли пакет на пути от корня к элементу.
     *
     * @param node    имя пакета
     * @param current элемент, с которого начинается подъём к корню
     * @return {@code true}, если пакет уже есть на пути
     */
    private static boolean isAncestor(String node, Entry current) {
        for (Entry entry = current; entry != null; entry = entry.parent) {
            if (entry.node.equals(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Элемент дерева, ожидающий вывода.
     *
     * @param node   имя пакета
     * @param parent элемент с пакетом-родителем или {@code null} для корня
     * @param prefix отступ строки
     * @param last   {@code true}, если это последний элемент своего уровня
     */
    private record Entry(String node, Entry parent, String prefix, boolean last) {
    }
}
