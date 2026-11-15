package jobtracker.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jobtracker.exception.LlmNoDisponibleException;
import jobtracker.exception.LlmRespuestaInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GeminiClient implements LlmClient {

    private static final String MENSAJE_NO_DISPONIBLE =
            "El servicio de IA no está disponible en este momento. Probá de nuevo más tarde.";

    private final RestClient restClient;
    private final String model;

    @Autowired
    public GeminiClient(@Value("${llm.api-key}") String apiKey,
                        @Value("${llm.model}") String model,
                        @Value("${llm.base-url}") String baseUrl,
                        @Value("${llm.timeout-seconds}") long timeoutSeconds) {
        this(crearRestClient(apiKey, baseUrl, timeoutSeconds), model);
    }

    GeminiClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    private static RestClient crearRestClient(String apiKey, String baseUrl, long timeoutSeconds) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    @Override
    public String generarJson(String instrucciones, String contenido) {
        Map<String, Object> cuerpo = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", instrucciones))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", contenido)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.2)
        );

        GeminiResponse respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/models/{modelo}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (RestClientResponseException e) {
            throw traducirError(e);
        } catch (ResourceAccessException e) {
            log.warn("No se pudo conectar con Gemini o la respuesta tardó demasiado: {}", e.getMessage());
            throw new LlmNoDisponibleException(
                    "El servicio de IA tardó demasiado en responder. Probá de nuevo en unos minutos.");
        }

        return extraerTexto(respuesta);
    }

    private LlmNoDisponibleException traducirError(RestClientResponseException e) {
        int estado = e.getStatusCode().value();

        if (estado == 429) {
            log.warn("Gemini respondió 429: se alcanzó el límite de uso.");
            return new LlmNoDisponibleException(
                    "El servicio de IA alcanzó su límite de uso. Probá de nuevo más tarde.");
        }
        if (estado == 401 || estado == 403) {
            log.error("Gemini rechazó la clave de API (HTTP {}). Revisá la variable GEMINI_API_KEY.", estado);
            return new LlmNoDisponibleException(MENSAJE_NO_DISPONIBLE);
        }

        log.error("Gemini respondió con un error HTTP {}: {}", estado, e.getResponseBodyAsString());
        return new LlmNoDisponibleException(MENSAJE_NO_DISPONIBLE);
    }

    private String extraerTexto(GeminiResponse respuesta) {
        if (respuesta == null || respuesta.candidates() == null || respuesta.candidates().isEmpty()) {
            throw new LlmRespuestaInvalidaException("La IA no devolvió ninguna respuesta.");
        }

        GeminiResponse.Candidate candidato = respuesta.candidates().get(0);
        if (candidato.content() == null || candidato.content().parts() == null) {
            throw new LlmRespuestaInvalidaException("La respuesta de la IA llegó vacía o fue bloqueada.");
        }

        String texto = candidato.content().parts().stream()
                .map(GeminiResponse.Part::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());

        if (texto.isBlank()) {
            throw new LlmRespuestaInvalidaException("La respuesta de la IA llegó vacía o fue bloqueada.");
        }
        return texto;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Candidate(Content content) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Content(List<Part> parts) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Part(String text) {
        }
    }
}