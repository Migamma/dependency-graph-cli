package org.example.maven;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Загружает POM-файлы из удалённого Maven-репозитория по HTTP(S).
 *
 * <p>Используется только стандартный {@link HttpClient} из JDK.
 */
public class HttpPomSource implements PomSource {

    /** Максимальное время ожидания соединения и ответа. */
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    /** HTTP-код успешного ответа. */
    private static final int HTTP_OK = 200;
    /** HTTP-код ответа «не найдено». */
    private static final int HTTP_NOT_FOUND = 404;

    /** Адрес корня репозитория без завершающего слэша. */
    private final String repositoryUrl;
    /** HTTP-клиент, общий для всех запросов. */
    private final HttpClient client;

    /**
     * Создаёт источник POM-файлов для заданного репозитория.
     *
     * @param repositoryUrl адрес корня Maven-репозитория,
     *                      например {@code https://repo1.maven.org/maven2}
     */
    public HttpPomSource(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl.replaceAll("/+$", "");
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(TIMEOUT)
                .proxy(ProxySelector.getDefault())
                .build();
    }

    /**
     * Загружает POM-файл пакета из репозитория.
     *
     * @param coordinates координаты пакета
     * @return содержимое POM-файла
     * @throws MavenException если репозиторий недоступен, пакет не найден
     *                        или сервер вернул ошибку
     */
    @Override
    public String fetchPom(MavenCoordinates coordinates) {
        String url = repositoryUrl + "/" + coordinates.pomPath();
        HttpResponse<String> response = send(url);
        int status = response.statusCode();
        if (status == HTTP_NOT_FOUND) {
            throw new MavenException("Package not found in repository: "
                    + coordinates + " (" + url + ")");
        }
        if (status != HTTP_OK) {
            throw new MavenException("Repository returned HTTP " + status + " for " + url);
        }
        return response.body();
    }

    /**
     * Выполняет GET-запрос.
     *
     * @param url адрес запроса
     * @return ответ сервера
     * @throws MavenException если адрес некорректен или соединение не удалось
     */
    private HttpResponse<String> send(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            return client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new MavenException("Invalid URL: " + url);
        } catch (IOException e) {
            throw new MavenException("Cannot connect to repository: " + url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MavenException("Download interrupted: " + url);
        }
    }
}
