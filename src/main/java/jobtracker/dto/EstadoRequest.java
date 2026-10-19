package jobtracker.dto;

import jakarta.validation.constraints.NotNull;
import jobtracker.entity.Estado;

public record EstadoRequest(
        @NotNull(message = "El estado es obligatorio") Estado estado
) {}