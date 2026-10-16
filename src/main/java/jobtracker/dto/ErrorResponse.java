package jobtracker.dto;

public record ErrorResponse(
        int status,
        String mensaje
) {
}
