package jobtracker.controller;

import jakarta.validation.Valid;
import jobtracker.dto.EstadoRequest;
import jobtracker.dto.PostulacionRequest;
import jobtracker.dto.PostulacionResponse;
import jobtracker.entity.Estado;
import jobtracker.service.PostulacionService;
import lombok.RequiredArgsConstructor;
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
public class PostulacionController {

    private final PostulacionService postulacionService;

    @PostMapping
    public ResponseEntity<PostulacionResponse> crear(Authentication auth,
                                                     @Valid @RequestBody PostulacionRequest request) {
        PostulacionResponse response = postulacionService.crear(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PostulacionResponse>> listar(
            Authentication auth,
            @RequestParam(required = false) Estado estado,
            @RequestParam(required = false) String buscar,
            @PageableDefault(size = 10, sort = "fechaPostulacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(postulacionService.listar(auth.getName(), estado, buscar, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostulacionResponse> obtener(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(postulacionService.obtener(auth.getName(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostulacionResponse> actualizar(Authentication auth, @PathVariable Long id,
                                                          @Valid @RequestBody PostulacionRequest request) {
        return ResponseEntity.ok(postulacionService.actualizar(auth.getName(), id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PostulacionResponse> cambiarEstado(Authentication auth, @PathVariable Long id,
                                                             @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(postulacionService.cambiarEstado(auth.getName(), id, request.estado()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication auth, @PathVariable Long id) {
        postulacionService.eliminar(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}