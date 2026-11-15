package jobtracker.llm;

import jobtracker.exception.LlmNoDisponibleException;
import jobtracker.exception.LlmRespuestaInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private static final String URL = "http://gemini.test/v1beta/models/modelo-prueba:generateContent";

    private static final String RESPUESTA_OK =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"saludo\\\":\\\"hola\\\"}\"}]},\"finishReason\":\"STOP\"}]}";

    private MockRestServiceServer servidor;
    private GeminiClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://gemini.test/v1beta")
                .defaultHeader("x-goog-api-key", "clave-de-prueba");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new GeminiClient(builder.build(), "modelo-prueba");
    }

    @Test
    void generarJson_respuestaValida_devuelveElTextoDelModelo() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "clave-de-prueba"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("Instrucciones de prueba"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("Contenido de prueba"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

        String resultado = cliente.generarJson("Instrucciones de prueba", "Contenido de prueba");

        assertEquals("{\"saludo\":\"hola\"}", resultado);
        servidor.verify();
    }

    @Test
    void generarJson_limiteDeUsoExcedido_lanzaNoDisponibleConMensajeDeLimite() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        LlmNoDisponibleException error = assertThrows(LlmNoDisponibleException.class,
                () -> cliente.generarJson("instrucciones", "contenido"));

        assertTrue(error.getMessage().contains("límite"));
    }

    @Test
    void generarJson_claveRechazada_lanzaNoDisponibleSinRevelarDetalles() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        LlmNoDisponibleException error = assertThrows(LlmNoDisponibleException.class,
                () -> cliente.generarJson("instrucciones", "contenido"));

        assertTrue(error.getMessage().contains("no está disponible"));
    }

    @Test
    void generarJson_errorDelServidor_lanzaNoDisponible() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(LlmNoDisponibleException.class,
                () -> cliente.generarJson("instrucciones", "contenido"));
    }

    @Test
    void generarJson_errorDeConexion_lanzaNoDisponible() {
        servidor.expect(requestTo(URL))
                .andRespond(request -> {
                    throw new IOException("Read timed out");
                });

        assertThrows(LlmNoDisponibleException.class,
                () -> cliente.generarJson("instrucciones", "contenido"));
    }

    @Test
    void generarJson_sinCandidatos_lanzaRespuestaInvalida() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(LlmRespuestaInvalidaException.class,
                () -> cliente.generarJson("instrucciones", "contenido"));
    }

    @Test
    void generarJson_textoEnBlanco_lanzaRespuestaInvalida() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"   \"}]}}]}",
                        MediaType.APPLICATION_JSON));

        assertThrows(LlmRespuestaInvalidaException.class,
                () -> cliente.generarJson("instrucciones", "contenido"));
    }
}