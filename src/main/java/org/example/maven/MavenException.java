package org.example.maven;

/**
 * Ошибка при получении данных о зависимостях из Maven-репозитория:
 * репозиторий недоступен, пакет не найден или POM-файл повреждён.
 */
public class MavenException extends RuntimeException {

    /** Версия класса для сериализации. */
    private static final long serialVersionUID = 1L;

    /**
     * Создаёт исключение с понятным пользователю описанием ошибки.
     *
     * @param message текст ошибки, который будет показан пользователю
     */
    public MavenException(String message) {
        super(message);
    }
}
