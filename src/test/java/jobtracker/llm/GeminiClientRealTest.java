package jobtracker.llm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiClientRealTest {

    @Test
    void generarJson_conLaApiReal_devuelveUnJson() {
        String modelo = System.getenv("GEMINI_MODEL");
        if (modelo == null || modelo.isBlank()) {
            modelo = "gemini-3.6-flash";
        }

        GeminiClient cliente = new GeminiClient(
                System.getenv("GEMINI_API_KEY"),
                modelo,
                "https://generativelanguage.googleapis.com/v1beta",
                30);

        String respuesta = cliente.generarJson(
                "Respondé únicamente con un JSON válido.",
                "Devolvé un objeto con una sola clave \"saludo\" y un saludo corto en español.");

        System.out.println("Respuesta de la IA: " + respuesta);

        assertFalse(respuesta.isBlank());
        assertTrue(respuesta.contains("saludo"));
    }
}