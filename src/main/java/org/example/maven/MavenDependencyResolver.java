package org.example.maven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Определяет прямые зависимости Maven-пакета по его POM-файлу.
 *
 * <p>Учитывает родительские POM, переменные {@code ${...}}, секцию
 * {@code dependencyManagement} и импорт BOM-файлов. В результат попадают
 * только зависимости с областью действия {@code compile} и {@code runtime},
 * необязательные ({@code optional}) зависимости пропускаются.
 * Загруженные POM-файлы кэшируются.
 */
public class MavenDependencyResolver {

    /** Максимальная длина цепочки родительских и импортируемых POM. */
    private static final int MAX_POM_DEPTH = 10;
    /** Область действия по умолчанию. */
    private static final String DEFAULT_SCOPE = "compile";
    /** Области действия, которые нужны пакету во время работы. */
    private static final Set<String> INCLUDED_SCOPES = Set.of("compile", "runtime");
    /** Область действия, означающая импорт BOM-файла. */
    private static final String IMPORT_SCOPE = "import";

    /** Источник POM-файлов. */
    private final PomSource source;
    /** Разборщик POM-файлов. */
    private final PomParser parser = new PomParser();
    /** Уже построенные POM-файлы по координатам. */
    private final Map<MavenCoordinates, EffectivePom> cache = new HashMap<>();

    /**
     * Создаёт резолвер, который берёт POM-файлы из заданного источника.
     *
     * @param source источник POM-файлов
     */
    public MavenDependencyResolver(PomSource source) {
        this.source = source;
    }

    /**
     * Возвращает прямые зависимости пакета.
     *
     * @param coordinates координаты пакета
     * @return прямые зависимости в порядке их объявления
     * @throws MavenException если POM-файл не удалось получить или разобрать,
     *                        либо версию зависимости определить невозможно
     */
    public List<Dependency> getDirectDependencies(MavenCoordinates coordinates) {
        EffectivePom pom = buildEffectivePom(coordinates, 0);
        List<Dependency> result = new ArrayList<>();
        for (RawDependency raw : pom.dependencies().values()) {
            toDependency(raw.resolve(pom.properties()), pom).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Превращает зависимость из POM-файла в итоговую, дополняя её версией
     * и областью действия из {@code dependencyManagement}.
     *
     * @param dependency зависимость с подставленными переменными
     * @param pom        POM-файл, в котором она объявлена
     * @return зависимость или пустое значение, если её нужно пропустить
     * @throws MavenException если версию зависимости определить невозможно
     */
    private Optional<Dependency> toDependency(RawDependency dependency, EffectivePom pom) {
        RawDependency managed = pom.managed().get(dependency.key());
        String scope = firstNonNull(dependency.scope(),
                managed == null ? null : managed.scope(), DEFAULT_SCOPE);
        if (!INCLUDED_SCOPES.contains(scope) || "true".equals(dependency.optional())) {
            return Optional.empty();
        }
        String version = firstNonNull(dependency.version(),
                managed == null ? null : managed.version(), null);
        if (version == null || dependency.groupId() == null
                || dependency.artifactId() == null) {
            throw new MavenException("Cannot determine coordinates of dependency "
                    + dependency.key() + " in " + pom.coordinates());
        }
        MavenCoordinates coordinates = new MavenCoordinates(
                dependency.groupId(), dependency.artifactId(), version);
        return Optional.of(new Dependency(coordinates, scope));
    }

    /**
     * Строит POM-файл пакета с учётом родительских POM.
     *
     * @param coordinates координаты пакета
     * @param depth       текущая глубина цепочки родительских и импортируемых POM
     * @return построенный POM-файл
     * @throws MavenException если цепочка слишком длинная или POM недоступен
     */
    private EffectivePom buildEffectivePom(MavenCoordinates coordinates, int depth) {
        EffectivePom cached = cache.get(coordinates);
        if (cached != null) {
            return cached;
        }
        if (depth > MAX_POM_DEPTH) {
            throw new MavenException("Parent POM chain is too long at " + coordinates);
        }
        PomModel model = parser.parse(source.fetchPom(coordinates), coordinates.toString());
        EffectivePom parent = model.parent() == null
                ? null
                : buildEffectivePom(model.parent(), depth + 1);
        EffectivePom pom = merge(coordinates, model, parent, depth);
        cache.put(coordinates, pom);
        return pom;
    }

    /**
     * Объединяет содержимое POM-файла с унаследованным от родителя.
     *
     * @param coordinates координаты пакета
     * @param model       содержимое POM-файла пакета
     * @param parent      построенный родительский POM или {@code null}
     * @param depth       текущая глубина цепочки POM
     * @return построенный POM-файл
     */
    private EffectivePom merge(MavenCoordinates coordinates, PomModel model,
                               EffectivePom parent, int depth) {
        Map<String, String> properties = new HashMap<>();
        Map<String, RawDependency> dependencies = new LinkedHashMap<>();
        Map<String, RawDependency> managed = new HashMap<>();
        if (parent != null) {
            properties.putAll(parent.properties());
            dependencies.putAll(parent.dependencies());
            managed.putAll(parent.managed());
        }
        properties.putAll(model.properties());
        addProjectProperties(properties, coordinates, model.parent());
        for (RawDependency dependency : model.dependencies()) {
            dependencies.put(dependency.key(), dependency);
        }
        for (RawDependency entry : model.managed()) {
            addManaged(entry.resolve(properties), managed, depth);
        }
        return new EffectivePom(coordinates, properties, dependencies, managed);
    }

    /**
     * Добавляет встроенные переменные {@code project.*} и {@code project.parent.*}.
     *
     * @param properties  переменные, в которые нужно добавить значения
     * @param coordinates координаты пакета
     * @param parent      координаты родительского POM или {@code null}
     */
    private void addProjectProperties(Map<String, String> properties,
                                      MavenCoordinates coordinates, MavenCoordinates parent) {
        properties.put("project.groupId", coordinates.groupId());
        properties.put("project.artifactId", coordinates.artifactId());
        properties.put("project.version", coordinates.version());
        if (parent != null) {
            properties.put("project.parent.groupId", parent.groupId());
            properties.put("project.parent.artifactId", parent.artifactId());
            properties.put("project.parent.version", parent.version());
        }
    }

    /**
     * Добавляет запись из {@code dependencyManagement}; если это импорт
     * BOM-файла, добавляет все записи из него.
     *
     * @param entry   запись с подставленными переменными
     * @param managed записи, в которые нужно добавить новые
     * @param depth   текущая глубина цепочки POM
     * @throws MavenException если у импортируемого BOM не указана версия
     */
    private void addManaged(RawDependency entry, Map<String, RawDependency> managed,
                            int depth) {
        if (!IMPORT_SCOPE.equals(entry.scope())) {
            managed.put(entry.key(), entry);
            return;
        }
        if (entry.groupId() == null || entry.artifactId() == null || entry.version() == null) {
            throw new MavenException("Cannot determine coordinates of imported BOM "
                    + entry.key());
        }
        MavenCoordinates bom = new MavenCoordinates(
                entry.groupId(), entry.artifactId(), entry.version());
        for (Map.Entry<String, RawDependency> imported
                : buildEffectivePom(bom, depth + 1).managed().entrySet()) {
            managed.putIfAbsent(imported.getKey(), imported.getValue());
        }
    }

    /**
     * Возвращает первое значение, не равное {@code null}.
     *
     * @param first    первое значение
     * @param second   второе значение
     * @param fallback значение по умолчанию
     * @return первое непустое значение или {@code fallback}
     */
    private static String firstNonNull(String first, String second, String fallback) {
        if (first != null) {
            return first;
        }
        return second != null ? second : fallback;
    }
}
