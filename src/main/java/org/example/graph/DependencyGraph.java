package org.example.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Граф зависимостей пакета, построенный с учётом транзитивности.
 *
 * <p>Помимо вершин и рёбер хранит сведения, полезные для вывода:
 * найденные циклы, пакеты, пропущенные фильтром, пакеты, которые не были
 * раскрыты из-за ограничения глубины, и предупреждения.
 */
public final class DependencyGraph {

    /** Анализируемый пакет — корень графа. */
    private final String root;
    /** Прямые зависимости каждого пакета графа в порядке обнаружения. */
    private final Map<String, Set<String>> edges = new LinkedHashMap<>();
    /** Пакеты, зависимости которых не загружались из-за ограничения глубины. */
    private final Set<String> notExpanded = new LinkedHashSet<>();
    /** Пакеты, пропущенные из-за фильтра. */
    private final Set<String> filtered = new LinkedHashSet<>();
    /** Найденные циклы в виде {@code A -> B -> A}. */
    private final Set<String> cycles = new LinkedHashSet<>();
    /** Предупреждения о пакетах, данные о которых не удалось получить. */
    private final List<String> warnings = new ArrayList<>();

    /**
     * Создаёт граф, состоящий из одного корня.
     *
     * @param root анализируемый пакет
     */
    public DependencyGraph(String root) {
        this.root = root;
        addNode(root);
    }

    /**
     * Добавляет вершину, если её ещё нет.
     *
     * @param node имя пакета
     */
    void addNode(String node) {
        edges.computeIfAbsent(node, key -> new LinkedHashSet<>());
    }

    /**
     * Добавляет ребро: пакет from зависит от пакета to.
     *
     * @param from зависящий пакет
     * @param to   пакет-зависимость
     */
    void addEdge(String from, String to) {
        addNode(from);
        addNode(to);
        edges.get(from).add(to);
    }

    /**
     * Отмечает, раскрыты ли зависимости пакета.
     *
     * @param node     имя пакета
     * @param expanded {@code true}, если зависимости пакета были загружены
     */
    void setExpanded(String node, boolean expanded) {
        if (expanded) {
            notExpanded.remove(node);
        } else {
            notExpanded.add(node);
        }
    }

    /**
     * Запоминает пакет, пропущенный из-за фильтра.
     *
     * @param node имя пакета
     */
    void addFiltered(String node) {
        filtered.add(node);
    }

    /**
     * Запоминает найденный цикл.
     *
     * @param path пакеты цикла по порядку; первый и последний совпадают
     */
    void addCycle(List<String> path) {
        cycles.add(String.join(" -> ", path));
    }

    /**
     * Запоминает предупреждение.
     *
     * @param warning текст предупреждения
     */
    void addWarning(String warning) {
        warnings.add(warning);
    }

    /**
     * Возвращает корень графа.
     *
     * @return анализируемый пакет
     */
    public String root() {
        return root;
    }

    /**
     * Возвращает все пакеты графа в порядке обнаружения.
     *
     * @return пакеты графа
     */
    public Set<String> nodes() {
        return Collections.unmodifiableSet(edges.keySet());
    }

    /**
     * Возвращает прямые зависимости пакета внутри графа.
     *
     * @param node имя пакета
     * @return прямые зависимости или пустое множество, если пакета нет в графе
     */
    public Set<String> dependenciesOf(String node) {
        return Collections.unmodifiableSet(edges.getOrDefault(node, Set.of()));
    }

    /**
     * Возвращает число рёбер графа.
     *
     * @return число зависимостей между пакетами графа
     */
    public int edgeCount() {
        int count = 0;
        for (Set<String> dependencies : edges.values()) {
            count += dependencies.size();
        }
        return count;
    }

    /**
     * Проверяет, были ли загружены зависимости пакета.
     *
     * @param node имя пакета
     * @return {@code false}, если пакет не раскрыт из-за ограничения глубины
     */
    public boolean isExpanded(String node) {
        return !notExpanded.contains(node);
    }

    /**
     * Возвращает пакеты, пропущенные из-за фильтра.
     *
     * @return пропущенные пакеты
     */
    public Set<String> filtered() {
        return Collections.unmodifiableSet(filtered);
    }

    /**
     * Возвращает найденные циклы.
     *
     * @return циклы в виде {@code A -> B -> A}
     */
    public Set<String> cycles() {
        return Collections.unmodifiableSet(cycles);
    }

    /**
     * Возвращает предупреждения.
     *
     * @return предупреждения о пакетах, данные о которых не удалось получить
     */
    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}
