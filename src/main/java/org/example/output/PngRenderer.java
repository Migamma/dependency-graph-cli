package org.example.output;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Сохраняет диаграмму D2 в файл и строит по ней PNG-изображение
 * с помощью утилиты {@code d2}, которую нужно установить отдельно.
 */
public final class PngRenderer {

    /** Имя исполняемого файла утилиты D2. */
    private static final String D2_COMMAND = "d2";
    /** Расширение файла диаграммы. */
    private static final String D2_EXTENSION = ".d2";
    /** Код возврата утилиты при успешном завершении. */
    private static final int EXIT_SUCCESS = 0;

    /** Класс содержит только статические методы, создавать его не нужно. */
    private PngRenderer() {
    }

    /**
     * Сохраняет диаграмму рядом с PNG-файлом (с расширением {@code .d2})
     * и строит изображение.
     *
     * @param diagram текст диаграммы на языке D2
     * @param png     путь к PNG-файлу
     * @return путь к сохранённому файлу диаграммы
     * @throws VisualizationException если файл не удалось сохранить
     *                                или утилита d2 завершилась с ошибкой
     */
    public static Path render(String diagram, Path png) {
        Path d2File = d2PathFor(png);
        writeDiagram(diagram, d2File);
        runD2(d2File, png);
        return d2File;
    }

    /**
     * Возвращает путь к файлу диаграммы: имя PNG-файла с расширением {@code .d2}.
     *
     * @param png путь к PNG-файлу
     * @return путь к файлу диаграммы
     */
    private static Path d2PathFor(Path png) {
        String name = png.getFileName().toString();
        String baseName = name.substring(0, name.lastIndexOf('.'));
        return png.resolveSibling(baseName + D2_EXTENSION);
    }

    /**
     * Записывает текст диаграммы в файл, при необходимости создавая папки.
     *
     * @param diagram текст диаграммы
     * @param d2File  путь к файлу диаграммы
     * @throws VisualizationException если файл не удалось записать
     */
    private static void writeDiagram(String diagram, Path d2File) {
        try {
            Path parent = d2File.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(d2File, diagram, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new VisualizationException("Cannot write D2 file: " + d2File);
        }
    }

    /**
     * Запускает {@code d2 <файл.d2> <файл.png>} и ждёт завершения.
     *
     * @param d2File путь к файлу диаграммы
     * @param png    путь к PNG-файлу
     * @throws VisualizationException если утилита не найдена или завершилась с ошибкой
     */
    private static void runD2(Path d2File, Path png) {
        ProcessBuilder builder = new ProcessBuilder(
                D2_COMMAND, d2File.toString(), png.toString());
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = readAll(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != EXIT_SUCCESS) {
                throw new VisualizationException("d2 failed with exit code " + exitCode
                        + ":\n" + output.strip());
            }
        } catch (IOException e) {
            throw new VisualizationException("Cannot run d2. Install it from "
                    + "https://d2lang.com and make sure it is in PATH");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VisualizationException("d2 was interrupted");
        }
    }

    /**
     * Читает весь вывод процесса.
     *
     * @param stream поток вывода процесса
     * @return вывод в виде строки
     * @throws IOException если поток не удалось прочитать
     */
    private static String readAll(InputStream stream) throws IOException {
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
