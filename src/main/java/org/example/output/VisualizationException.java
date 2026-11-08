package org.example.output;

/**
 * Ошибка визуализации: не удалось сохранить файл диаграммы
 * или утилита d2 не смогла построить изображение.
 */
public class VisualizationException extends RuntimeException {

    /** Версия класса для сериализации. */
    private static final long serialVersionUID = 1L;

    /**
     * Создаёт исключение с понятным пользователю описанием ошибки.
     *
     * @param message текст ошибки, который будет показан пользователю
     */
    public VisualizationException(String message) {
        super(message);
    }
}
