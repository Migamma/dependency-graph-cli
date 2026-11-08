package org.example.graph;

/**
 * Ошибка источника зависимостей: репозиторий недоступен, пакет не найден
 * или файл тестового репозитория имеет неверный формат.
 */
public class DependencySourceException extends RuntimeException {

    /** Версия класса для сериализации. */
    private static final long serialVersionUID = 1L;

    /**
     * Создаёт исключение с понятным пользователю описанием ошибки.
     *
     * @param message текст ошибки, который будет показан пользователю
     */
    public DependencySourceException(String message) {
        super(message);
    }
}
