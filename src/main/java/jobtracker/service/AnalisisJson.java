package jobtracker.service;

import jobtracker.dto.AnalisisResultado;
import jobtracker.exception.LlmRespuestaInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AnalisisJson {

    private final ObjectMapper objectMapper;

    public AnalisisResultado leer(String json) {
        try {
            AnalisisResultado resultado = objectMapper.readValue(json, AnalisisResultado.class);
            if (resultado == null) {
                throw new LlmRespuestaInvalidaException("La IA devolvió una respuesta vacía.");
            }
            return resultado;
        } catch (JacksonException e) {
            throw new LlmRespuestaInvalidaException("La IA devolvió un formato que no se pudo interpretar.");
        }
    }

    public AnalisisResultado leerGuardado(String json) {
        try {
            return objectMapper.readValue(json, AnalisisResultado.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("El análisis guardado no se pudo leer.", e);
        }
    }

    public String escribir(AnalisisResultado resultado) {
        return objectMapper.writeValueAsString(resultado);
    }
}