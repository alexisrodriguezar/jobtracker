package jobtracker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jobtracker.dto.*;
import jobtracker.entity.Estado;
import jobtracker.service.PostulacionService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/postulaciones")
@RequiredArgsConstructor
@Tag(name = "Postulaciones", description = "Gestión de las postulaciones del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
public class PostulacionController {

    private final PostulacionService postulacionService;

    @PostMapping
    @Operation(summary = "Crear una postulación",
            description = "Crea una postulación en estado ENVIADA. Si la empresa no existe, se crea automáticamente. "
                    + "Si no se envía fecha, se usa la de hoy.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Postulación creada",
                    content = @Content(schema = @Schema(implementation = PostulacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<PostulacionResponse> crear(Authentication auth,
                                                     @Valid @RequestBody PostulacionRequest request) {
        PostulacionResponse response = postulacionService.crear(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar postulaciones",
            description = "Devuelve, de forma paginada, solo las postulaciones del usuario autenticado. "
                    + "Se puede filtrar por estado y buscar por texto en el puesto o en el nombre de la empresa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de postulaciones"),
            @ApiResponse(responseCode = "400", description = "Parámetro inválido (por ejemplo, un estado que no existe)")
    })
    public ResponseEntity<Page<PostulacionResponse>> listar(
            Authentication auth,
            @Parameter(description = "Filtrar por estado de la postulación")
            @RequestParam(required = false) Estado estado,
            @Parameter(description = "Texto a buscar en el puesto o en la empresa")
            @RequestParam(required = false) String buscar,
            @ParameterObject
            @PageableDefault(size = 10, sort = "fechaPostulacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postulacionService.listar(auth.getName(), estado, buscar, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una postulación por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Postulación encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe o no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PostulacionResponse> obtener(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(postulacionService.obtener(auth.getName(), id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modificar una postulación",
            description = "Modifica solo los campos que se envían; los demás conservan su valor. "
                    + "Para borrar un texto opcional (link, nota o descripción de la oferta) hay que enviarlo vacío. "
                    + "El estado se cambia con el endpoint específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Postulación modificada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "No existe o no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PostulacionResponse> modificar(Authentication auth, @PathVariable Long id,
                                                         @Valid @RequestBody PostulacionPatchRequest request) {
        return ResponseEntity.ok(postulacionService.modificar(auth.getName(), id, request));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de una postulación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado inválido o ausente"),
            @ApiResponse(responseCode = "404", description = "No existe o no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PostulacionResponse> cambiarEstado(Authentication auth, @PathVariable Long id,
                                                             @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(postulacionService.cambiarEstado(auth.getName(), id, request.estado()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una postulación")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Postulación eliminada"),
            @ApiResponse(responseCode = "404", description = "No existe o no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminar(Authentication auth, @PathVariable Long id) {
        postulacionService.eliminar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}