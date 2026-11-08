package org.example.config;

/**
 * Ошибка конфигурации: файл не удалось прочитать, он имеет неверный формат
 * или какой-либо параметр отсутствует либо имеет недопустимое значение.
 */
public class ConfigException extends RuntimeException {

    /** Версия класса для сериализации. */
    private static final long serialVersionUID = 1L;

    /**
     * Создаёт исключение с понятным пользователю описанием ошибки.
     *
     * @param message текст ошибки, который будет показан пользователю
     */
    public ConfigException(String message) {
        super(message);
    }
}
