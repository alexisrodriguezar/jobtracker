package jobtracker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jobtracker.dto.PerfilRequest;
import jobtracker.dto.PerfilResponse;
import jobtracker.service.PerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perfil")
@RequiredArgsConstructor
@Tag(name = "Perfil",
        description = "Perfil de habilidades del usuario autenticado. No incluyas datos personales "
                + "(nombre, email, teléfono): el texto se enviará a un proveedor de IA para analizar ofertas.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping
    @Operation(summary = "Obtener mi perfil",
            description = "Devuelve el perfil de habilidades del usuario autenticado (vacío si todavía no lo cargó).")
    public ResponseEntity<PerfilResponse> obtener(Authentication auth) {
        return ResponseEntity.ok(perfilService.obtener(auth.getName()));
    }

    @PutMapping
    @Operation(summary = "Guardar mi perfil",
            description = "Reemplaza el perfil completo. Enviar un texto vacío lo borra. "
                    + "No incluyas datos personales: el texto se enviará a un proveedor de IA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil guardado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (por ejemplo, supera el largo máximo)")
    })
    public ResponseEntity<PerfilResponse> actualizar(Authentication auth,
                                                     @Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.ok(perfilService.actualizar(auth.getName(), request));
    }
}