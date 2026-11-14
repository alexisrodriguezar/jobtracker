package jobtracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jobtracker.entity.Usuario;

public record PerfilRequest(
        @Schema(description = "Resumen de tus habilidades y experiencia. No incluyas datos personales (nombre, email, teléfono).",
                example = "Estudiante de Ingeniería en Sistemas. Java 21, Spring Boot, Spring Security (JWT), MySQL, Docker, JUnit y Mockito. Proyecto: API REST con tests de integración y CI con GitHub Actions.")
        @NotNull(message = "El perfil es obligatorio (enviá un texto vacío para borrarlo)")
        @Size(max = Usuario.MAX_LARGO_PERFIL, message = "El perfil no puede superar los {max} caracteres")
        String perfil
) {
}