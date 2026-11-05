package jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jobtracker.entity.Seniority;

import java.time.LocalDate;

public record PostulacionRequest(
        @NotBlank(message = "La empresa es obligatoria") String empresa,
        @NotBlank(message = "El puesto es obligatorio") String puesto,
        String link,
        @Size(max = 1000, message = "La nota no puede superar los 1000 caracteres") String nota,
        Seniority seniority,
        LocalDate fechaPostulacion,
        @Size(max = 8000, message = "La descripción no puede superar los 8000 caracteres") String descripcionOferta
) {}