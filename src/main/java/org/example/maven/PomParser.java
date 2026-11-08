package org.example.maven;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Разбирает POM-файл стандартным DOM-парсером из JDK.
 *
 * <p>Берутся только прямые дочерние элементы нужных секций, поэтому теги
 * {@code dependency} внутри плагинов и профилей не учитываются.
 */
public class PomParser {

    /** Имя корневого элемента POM-файла. */
    private static final String PROJECT = "project";
    /** Имя секции со списком зависимостей. */
    private static final String DEPENDENCIES = "dependencies";

    /**
     * Создаёт разборщик POM-файлов.
     */
    public PomParser() {
    }

    /**
     * Разбирает текст POM-файла.
     *
     * @param xml    содержимое POM-файла
     * @param source описание источника для сообщений об ошибках (координаты пакета)
     * @return разобранное содержимое
     * @throws MavenException если текст не является корректным POM-файлом
     */
    public PomModel parse(String xml, String source) {
        Element project = parseDocument(xml, source).getDocumentElement();
        if (!PROJECT.equals(project.getTagName())) {
            throw new MavenException("Not a Maven POM file: " + source);
        }
        Element management = child(project, "dependencyManagement");
        return new PomModel(
                parseParent(child(project, "parent"), source),
                parseProperties(child(project, "properties")),
                parseDependencies(child(project, DEPENDENCIES)),
                parseDependencies(management == null ? null : child(management, DEPENDENCIES)));
    }

    /**
     * Превращает текст в XML-документ.
     *
     * <p>Загрузка внешних DTD и сущностей отключена ради безопасности.
     *
     * @param xml    текст документа
     * @param source описание источника для сообщений об ошибках
     * @return XML-документ
     * @throws MavenException если текст не является корректным XML
     */
    private Document parseDocument(String xml, String source) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new MavenException("Invalid XML in POM file: " + source);
        }
    }

    /**
     * Разбирает секцию {@code parent}.
     *
     * @param parent элемент {@code parent} или {@code null}
     * @param source описание источника для сообщений об ошибках
     * @return координаты родительского POM или {@code null}, если его нет
     * @throws MavenException если в секции не хватает координат
     */
    private MavenCoordinates parseParent(Element parent, String source) {
        if (parent == null) {
            return null;
        }
        String groupId = text(parent, "groupId");
        String artifactId = text(parent, "artifactId");
        String version = text(parent, "version");
        if (groupId == null || artifactId == null || version == null) {
            throw new MavenException("Incomplete parent section in POM file: " + source);
        }
        return new MavenCoordinates(groupId, artifactId, version);
    }

    /**
     * Разбирает секцию {@code properties}: каждый дочерний элемент — переменная.
     *
     * @param properties элемент {@code properties} или {@code null}
     * @return переменные и их значения
     */
    private Map<String, String> parseProperties(Element properties) {
        Map<String, String> result = new HashMap<>();
        for (Element property : children(properties)) {
            String value = property.getTextContent();
            result.put(property.getTagName(), value == null ? "" : value.trim());
        }
        return result;
    }

    /**
     * Разбирает секцию {@code dependencies}.
     *
     * @param dependencies элемент {@code dependencies} или {@code null}
     * @return список зависимостей в порядке их объявления
     */
    private List<RawDependency> parseDependencies(Element dependencies) {
        List<RawDependency> result = new ArrayList<>();
        for (Element dependency : children(dependencies)) {
            if (!"dependency".equals(dependency.getTagName())) {
                continue;
            }
            result.add(new RawDependency(
                    text(dependency, "groupId"),
                    text(dependency, "artifactId"),
                    text(dependency, "version"),
                    text(dependency, "scope"),
                    text(dependency, "type"),
                    text(dependency, "optional")));
        }
        return result;
    }

    /**
     * Возвращает все прямые дочерние элементы.
     *
     * @param parent родительский элемент или {@code null}
     * @return дочерние элементы (пустой список, если родителя нет)
     */
    private List<Element> children(Element parent) {
        List<Element> result = new ArrayList<>();
        if (parent == null) {
            return result;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Возвращает первый прямой дочерний элемент с заданным именем.
     *
     * @param parent родительский элемент
     * @param name   имя элемента
     * @return найденный элемент или {@code null}
     */
    private Element child(Element parent, String name) {
        for (Element element : children(parent)) {
            if (name.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    /**
     * Возвращает текст прямого дочернего элемента.
     *
     * @param parent родительский элемент
     * @param name   имя элемента
     * @return текст без пробелов по краям или {@code null}, если элемента нет
     *         или он пустой
     */
    private String text(Element parent, String name) {
        Element element = child(parent, name);
        if (element == null) {
            return null;
        }
        String value = element.getTextContent().trim();
        return value.isEmpty() ? null : value;
    }
}
