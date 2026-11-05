package jobtracker.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jobtracker.entity.Seniority;

import java.time.LocalDate;

public record PostulacionPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "La empresa no puede estar en blanco") String empresa,
        @Pattern(regexp = ".*\\S.*", message = "El puesto no puede estar en blanco") String puesto,
        String link,
        @Size(max = 1000, message = "La nota no puede superar los 1000 caracteres") String nota,
        Seniority seniority,
        LocalDate fechaPostulacion,
        @Size(max = 8000, message = "La descripción no puede superar los 8000 caracteres") String descripcionOferta
) {
}