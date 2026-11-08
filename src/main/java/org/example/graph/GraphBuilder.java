package org.example.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Строит граф зависимостей обходом в глубину (DFS) без рекурсии.
 *
 * <p>Вместо рекурсивных вызовов используется явный стек: на вершине стека
 * лежит пакет, который обрабатывается сейчас, а под ним — путь от корня
 * к этому пакету. Это позволяет:
 * <ul>
 *   <li>не заходить глубже заданной максимальной глубины;</li>
 *   <li>находить циклы: если зависимость уже лежит в стеке, значит,
 *       найден путь, ведущий обратно в неё;</li>
 *   <li>не обходить один и тот же пакет повторно.</li>
 * </ul>
 * Пакеты, имя которых содержит подстроку фильтра, в граф не попадают
 * и не раскрываются.
 */
public class GraphBuilder {

    /** Источник данных о зависимостях. */
    private final DependencySource source;
    /** Максимальная глубина анализа: корень имеет глубину 0. */
    private final int maxDepth;
    /** Подстрока фильтра; пустая строка означает, что фильтр не задан. */
    private final String filter;

    /**
     * Создаёт построитель графа.
     *
     * @param source   источник данных о зависимостях
     * @param maxDepth максимальная глубина анализа (не меньше 1)
     * @param filter   подстрока фильтра или пустая строка
     */
    public GraphBuilder(DependencySource source, int maxDepth, String filter) {
        this.source = source;
        this.maxDepth = maxDepth;
        this.filter = filter;
    }

    /**
     * Строит граф зависимостей пакета.
     *
     * @param root анализируемый пакет
     * @return граф зависимостей
     * @throws DependencySourceException если не удалось получить зависимости корня
     */
    public DependencyGraph build(String root) {
        DependencyGraph graph = new DependencyGraph(root);
        Deque<Frame> stack = new ArrayDeque<>();
        Map<String, Integer> depths = new HashMap<>();

        depths.put(root, 0);
        stack.push(new Frame(root, 0, loadDependencies(root, 0, graph)));
        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (!frame.hasNext()) {
                stack.pop();
                continue;
            }
            String child = frame.next();
            graph.addEdge(frame.node, child);
            visit(child, frame.depth + 1, stack, depths, graph);
        }
        return graph;
    }

    /**
     * Обрабатывает зависимость, найденную при обходе.
     *
     * <p>Если зависимость уже лежит в стеке, найден цикл. Если она уже была
     * обойдена на той же или меньшей глубине, повторно её обходить не нужно.
     * Иначе она кладётся в стек. Повторный обход на меньшей глубине нужен,
     * чтобы раскрыть пакет, который раньше встретился слишком глубоко.
     *
     * @param node   найденная зависимость
     * @param depth  её глубина на текущем пути
     * @param stack  стек обхода
     * @param depths наименьшая глубина, на которой встречался каждый пакет
     * @param graph  строящийся граф
     */
    private void visit(String node, int depth, Deque<Frame> stack,
                       Map<String, Integer> depths, DependencyGraph graph) {
        if (isOnStack(node, stack)) {
            graph.addCycle(cyclePath(node, stack));
            return;
        }
        Integer knownDepth = depths.get(node);
        if (knownDepth != null && knownDepth <= depth) {
            return;
        }
        depths.put(node, depth);
        stack.push(new Frame(node, depth, loadDependencies(node, depth, graph)));
    }

    /**
     * Загружает прямые зависимости пакета с учётом глубины и фильтра.
     *
     * @param node  имя пакета
     * @param depth глубина пакета
     * @param graph строящийся граф
     * @return зависимости, которые нужно обойти
     * @throws DependencySourceException если не удалось получить зависимости корня
     */
    private List<String> loadDependencies(String node, int depth, DependencyGraph graph) {
        if (depth >= maxDepth) {
            graph.setExpanded(node, false);
            return List.of();
        }
        graph.setExpanded(node, true);
        List<String> dependencies;
        try {
            dependencies = source.getDependencies(node);
        } catch (DependencySourceException e) {
            if (node.equals(graph.root())) {
                throw e;
            }
            graph.addWarning(e.getMessage());
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String dependency : dependencies) {
            if (isFiltered(dependency)) {
                graph.addFiltered(dependency);
            } else {
                result.add(dependency);
            }
        }
        return result;
    }

    /**
     * Проверяет, нужно ли пропустить пакет из-за фильтра.
     *
     * @param node имя пакета
     * @return {@code true}, если имя содержит подстроку фильтра
     */
    private boolean isFiltered(String node) {
        return !filter.isEmpty() && node.contains(filter);
    }

    /**
     * Проверяет, лежит ли пакет в стеке, то есть на пути от корня
     * к текущему пакету.
     *
     * @param node  имя пакета
     * @param stack стек обхода
     * @return {@code true}, если пакет лежит в стеке
     */
    private boolean isOnStack(String node, Deque<Frame> stack) {
        for (Frame frame : stack) {
            if (frame.node.equals(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Восстанавливает цикл по стеку: от пакета, в который вернулся путь,
     * до текущего пакета и снова до первого.
     *
     * @param node  пакет, в который вернулся путь
     * @param stack стек обхода
     * @return пакеты цикла по порядку; первый и последний совпадают
     */
    private List<String> cyclePath(String node, Deque<Frame> stack) {
        List<String> path = new ArrayList<>();
        Iterator<Frame> fromRoot = stack.descendingIterator();
        boolean inCycle = false;
        while (fromRoot.hasNext()) {
            String current = fromRoot.next().node;
            inCycle = inCycle || current.equals(node);
            if (inCycle) {
                path.add(current);
            }
        }
        path.add(node);
        return path;
    }

    /**
     * Элемент стека обхода: пакет и позиция в списке его зависимостей.
     */
    private static final class Frame {

        /** Имя пакета. */
        private final String node;
        /** Глубина пакета. */
        private final int depth;
        /** Зависимости пакета, которые нужно обойти. */
        private final List<String> dependencies;
        /** Индекс следующей зависимости для обхода. */
        private int nextIndex;

        /**
         * Создаёт элемент стека.
         *
         * @param node         имя пакета
         * @param depth        глубина пакета
         * @param dependencies зависимости пакета, которые нужно обойти
         */
        Frame(String node, int depth, List<String> dependencies) {
            this.node = node;
            this.depth = depth;
            this.dependencies = dependencies;
        }

        /**
         * Проверяет, остались ли необойдённые зависимости.
         *
         * @return {@code true}, если остались
         */
        boolean hasNext() {
            return nextIndex < dependencies.size();
        }

        /**
         * Возвращает следующую зависимость и сдвигает позицию.
         *
         * @return имя следующей зависимости
         */
        String next() {
            String dependency = dependencies.get(nextIndex);
            nextIndex++;
            return dependency;
        }
    }
}
