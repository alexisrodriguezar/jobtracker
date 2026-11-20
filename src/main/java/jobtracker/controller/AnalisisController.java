package jobtracker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jobtracker.dto.AnalisisResponse;
import jobtracker.dto.ErrorResponse;
import jobtracker.service.AnalisisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/postulaciones/{id}/analisis")
@RequiredArgsConstructor
@Tag(name = "Análisis con IA",
        description = "Compara la oferta de una postulación con tu perfil. El perfil y el texto de la oferta "
                + "se envían a un proveedor de IA: no incluyas datos personales.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
public class AnalisisController {

    private final AnalisisService analisisService;

    @PostMapping
    @Operation(summary = "Analizar la oferta con IA",
            description = "Genera un análisis nuevo (reemplaza el anterior) y lo guarda. Requiere que la postulación "
                    + "tenga descripción de la oferta y que hayas cargado tu perfil. Consume uno de tus análisis "
                    + "diarios y del cupo global del servicio; si el análisis falla, no se descuenta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Análisis generado y guardado"),
            @ApiResponse(responseCode = "400", description = "Falta la descripción de la oferta o el perfil",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe o no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Alcanzaste tu límite diario de análisis",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "La IA respondió algo inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Servicio de IA no disponible o cupo diario global agotado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalisisResponse> analizar(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(analisisService.analizar(auth.getName(), id));
    }

    @GetMapping
    @Operation(summary = "Obtener el último análisis guardado",
            description = "Devuelve el último análisis sin llamar a la IA, así que no gasta cupo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Último análisis guardado"),
            @ApiResponse(responseCode = "404", description = "La postulación no existe, no es tuya o todavía no tiene análisis",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalisisResponse> obtener(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(analisisService.obtenerUltimo(auth.getName(), id));
    }
}