package jobtracker.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnalisisResponse(
        int porcentajeCoincidencia,
        List<String> habilidadesRequeridas,
        List<String> habilidadesQueTenes,
        List<String> habilidadesFaltantes,
        String resumen,
        List<String> sugerencias,
        LocalDateTime fecha
) {
    public static AnalisisResponse de(AnalisisResultado r, LocalDateTime fecha) {
        return new AnalisisResponse(r.porcentajeCoincidencia(), r.habilidadesRequeridas(),
                r.habilidadesQueTenes(), r.habilidadesFaltantes(), r.resumen(), r.sugerencias(), fecha);
    }
}