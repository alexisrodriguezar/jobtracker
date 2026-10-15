package jobtracker.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String email
        ) {
}
