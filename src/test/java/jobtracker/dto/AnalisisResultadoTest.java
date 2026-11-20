package jobtracker.dto;

import jobtracker.entity.Postulacion;
import jobtracker.exception.LlmRespuestaInvalidaException;
import jobtracker.service.AnalisisJson;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalisisResultadoTest {

    private final AnalisisJson analisisJson = new AnalisisJson(JsonMapper.builder().build());

    @Test
    void sanear_resultadoValido_loDejaIgual() {
        AnalisisResultado resultado = new AnalisisResultado(
                72, List.of("Java", "MySQL"), List.of("Java"), List.of("MySQL"),
                "Buen encaje.", List.of("Sumá un proyecto con MySQL"));

        assertEquals(resultado, resultado.sanear());
    }

    @Test
    void sanear_porcentajeFueraDeRango_loAcotaEntreCeroYCien() {
        AnalisisResultado alto = new AnalisisResultado(150, List.of("Java"), null, null, "Resumen", null);
        AnalisisResultado bajo = new AnalisisResultado(-5, List.of("Java"), null, null, "Resumen", null);

        assertEquals(100, alto.sanear().porcentajeCoincidencia());
        assertEquals(0, bajo.sanear().porcentajeCoincidencia());
    }

    @Test
    void sanear_sinPorcentaje_lanzaRespuestaInvalida() {
        AnalisisResultado resultado = new AnalisisResultado(null, List.of("Java"), null, null, "Resumen", null);

        assertThrows(LlmRespuestaInvalidaException.class, resultado::sanear);
    }

    @Test
    void sanear_listasNulas_quedanVacias() {
        AnalisisResultado resultado = new AnalisisResultado(50, List.of("Java"), null, null, "Resumen", null).sanear();

        assertEquals(List.of(), resultado.habilidadesQueTenes());
        assertEquals(List.of(), resultado.habilidadesFaltantes());
        assertEquals(List.of(), resultado.sugerencias());
    }

    @Test
    void sanear_limpiaEspaciosYVacios_yRecortaCadaTexto() {
        List<String> sucias = Arrays.asList("  Java  ", "   ", null, "x".repeat(80));

        AnalisisResultado resultado = new AnalisisResultado(50, sucias, null, null, "Resumen", null).sanear();

        assertEquals(List.of("Java", "x".repeat(AnalisisResultado.MAX_LARGO_HABILIDAD)),
                resultado.habilidadesRequeridas());
    }

    @Test
    void sanear_masDeDoceHabilidades_dejaSoloLasPrimerasDoce() {
        List<String> muchas = Collections.nCopies(20, "Java");

        AnalisisResultado resultado = new AnalisisResultado(50, muchas, null, null, "Resumen", null).sanear();

        assertEquals(AnalisisResultado.MAX_HABILIDADES, resultado.habilidadesRequeridas().size());
    }

    @Test
    void sanear_resumenLargo_loRecorta() {
        AnalisisResultado resultado = new AnalisisResultado(
                50, List.of("Java"), null, null, "r".repeat(1000), null).sanear();

        assertEquals(AnalisisResultado.MAX_LARGO_RESUMEN, resultado.resumen().length());
    }

    @Test
    void sanear_analisisSinHabilidadesNiResumen_lanzaRespuestaInvalida() {
        AnalisisResultado vacio = new AnalisisResultado(50, List.of(), List.of(), List.of(), "   ", List.of());

        assertThrows(LlmRespuestaInvalidaException.class, vacio::sanear);
    }

    @Test
    void sanear_elPeorCasoEnTamanio_entraEnLaColumnaDeLaBase() {
        List<String> habilidades = Collections.nCopies(AnalisisResultado.MAX_HABILIDADES, "x".repeat(50));
        List<String> sugerencias = Collections.nCopies(AnalisisResultado.MAX_SUGERENCIAS, "x".repeat(180));

        AnalisisResultado peorCaso = new AnalisisResultado(
                100, habilidades, habilidades, habilidades, "x".repeat(400), sugerencias).sanear();

        assertTrue(analisisJson.escribir(peorCaso).length() <= Postulacion.MAX_LARGO_ANALISIS);
    }
}