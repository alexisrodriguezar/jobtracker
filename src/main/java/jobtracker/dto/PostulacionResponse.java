package jobtracker.dto;

import jobtracker.entity.Estado;
import jobtracker.entity.Seniority;

import java.time.LocalDate;

public record PostulacionResponse(
        Long id,
        String empresa,
        String puesto,
        Estado estado,
        Seniority seniority,
        String link,
        String nota,
        LocalDate fechaPostulacion
) {}