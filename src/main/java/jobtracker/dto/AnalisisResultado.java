package jobtracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jobtracker.exception.LlmRespuestaInvalidaException;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalisisResultado(
        Integer porcentajeCoincidencia,
        List<String> habilidadesRequeridas,
        List<String> habilidadesQueTenes,
        List<String> habilidadesFaltantes,
        String resumen,
        List<String> sugerencias
) {
    public static final int MAX_HABILIDADES = 12;
    public static final int MAX_LARGO_HABILIDAD = 50;
    public static final int MAX_LARGO_RESUMEN = 400;
    public static final int MAX_SUGERENCIAS = 5;
    public static final int MAX_LARGO_SUGERENCIA = 180;

    public AnalisisResultado sanear() {
        if (porcentajeCoincidencia == null) {
            throw new LlmRespuestaInvalidaException("La IA no indicó el porcentaje de coincidencia.");
        }
        int porcentaje = Math.max(0, Math.min(100, porcentajeCoincidencia));

        List<String> requeridas = limpiar(habilidadesRequeridas, MAX_HABILIDADES, MAX_LARGO_HABILIDAD);
        List<String> queTenes = limpiar(habilidadesQueTenes, MAX_HABILIDADES, MAX_LARGO_HABILIDAD);
        List<String> faltantes = limpiar(habilidadesFaltantes, MAX_HABILIDADES, MAX_LARGO_HABILIDAD);
        List<String> sugerenciasLimpias = limpiar(sugerencias, MAX_SUGERENCIAS, MAX_LARGO_SUGERENCIA);
        String resumenLimpio = recortar(resumen == null ? "" : resumen.trim(), MAX_LARGO_RESUMEN);

        if (requeridas.isEmpty() && resumenLimpio.isBlank()) {
            throw new LlmRespuestaInvalidaException("La IA devolvió un análisis vacío.");
        }
        return new AnalisisResultado(porcentaje, requeridas, queTenes, faltantes, resumenLimpio, sugerenciasLimpias);
    }

    private static List<String> limpiar(List<String> textos, int maxItems, int maxLargo) {
        if (textos == null) {
            return List.of();
        }
        return textos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(texto -> !texto.isBlank())
                .map(texto -> recortar(texto, maxLargo))
                .limit(maxItems)
                .toList();
    }

    private static String recortar(String texto, int max) {
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}