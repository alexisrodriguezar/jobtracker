package jobtracker.service;

import jobtracker.exception.CupoGlobalAgotadoException;
import jobtracker.exception.LimiteUsuarioExcedidoException;
import jobtracker.repository.UsoIaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsoIaServiceTest {

    private static final long LIMITE_POR_USUARIO = 5;
    private static final long LIMITE_GLOBAL = 300;

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate HOY_EN_EL_PACIFICO = LocalDate.of(2026, 11, 15);

    @Mock
    private UsoIaRepository usoIaRepository;

    private UsoIaService usoIaService;

    @BeforeEach
    void preparar() {
        usoIaService = new UsoIaService(usoIaRepository, RELOJ, (int) LIMITE_POR_USUARIO, (int) LIMITE_GLOBAL);
    }

    @Test
    void verificarCupo_conCupoDisponible_noLanzaNada() {
        when(usoIaRepository.totalDelDia(HOY_EN_EL_PACIFICO)).thenReturn(10L);
        when(usoIaRepository.totalDelUsuarioEnElDia(1L, HOY_EN_EL_PACIFICO)).thenReturn(2L);

        assertDoesNotThrow(() -> usoIaService.verificarCupo(1L));
    }

    @Test
    void verificarCupo_usuarioEnSuLimite_lanzaLimiteDeUsuarioConElNumero() {
        when(usoIaRepository.totalDelDia(HOY_EN_EL_PACIFICO)).thenReturn(10L);
        when(usoIaRepository.totalDelUsuarioEnElDia(1L, HOY_EN_EL_PACIFICO)).thenReturn(LIMITE_POR_USUARIO);

        LimiteUsuarioExcedidoException error = assertThrows(LimiteUsuarioExcedidoException.class,
                () -> usoIaService.verificarCupo(1L));

        assertTrue(error.getMessage().contains("5"));
    }

    @Test
    void verificarCupo_usuarioPorDebajoDelLimite_todaviaPuedeAnalizar() {
        when(usoIaRepository.totalDelDia(HOY_EN_EL_PACIFICO)).thenReturn(10L);
        when(usoIaRepository.totalDelUsuarioEnElDia(1L, HOY_EN_EL_PACIFICO)).thenReturn(LIMITE_POR_USUARIO - 1);

        assertDoesNotThrow(() -> usoIaService.verificarCupo(1L));
    }

    @Test
    void verificarCupo_cupoGlobalAgotado_lanzaCupoGlobalYNoConsultaAlUsuario() {
        when(usoIaRepository.totalDelDia(HOY_EN_EL_PACIFICO)).thenReturn(LIMITE_GLOBAL);

        assertThrows(CupoGlobalAgotadoException.class, () -> usoIaService.verificarCupo(1L));

        verify(usoIaRepository, never()).totalDelUsuarioEnElDia(anyLong(), any());
    }

    @Test
    void verificarCupo_mideElDiaEnHoraDelPacifico_noEnUtc() {
        usoIaService.verificarCupo(1L);

        // Se consultó el 15 (Pacífico) y nunca el 16 (la fecha en UTC)
        verify(usoIaRepository).totalDelDia(HOY_EN_EL_PACIFICO);
        verify(usoIaRepository, never()).totalDelDia(LocalDate.of(2026, 11, 16));
    }

    @Test
    void registrarUso_sumaUnAnalisisConElDiaDelPacifico() {
        usoIaService.registrarUso(1L);

        verify(usoIaRepository).registrarUso(1L, HOY_EN_EL_PACIFICO);
    }
}