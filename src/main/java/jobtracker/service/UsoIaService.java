package jobtracker.service;

import jobtracker.exception.CupoGlobalAgotadoException;
import jobtracker.exception.LimiteUsuarioExcedidoException;
import jobtracker.repository.UsoIaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class UsoIaService {

    static final ZoneId ZONA_DEL_CUPO = ZoneId.of("America/Los_Angeles");

    private final UsoIaRepository usoIaRepository;
    private final Clock clock;
    private final int limitePorUsuario;
    private final int limiteGlobal;

    public UsoIaService(UsoIaRepository usoIaRepository,
                        Clock clock,
                        @Value("${analisis.limite-diario-por-usuario}") int limitePorUsuario,
                        @Value("${analisis.limite-diario-global}") int limiteGlobal) {
        this.usoIaRepository = usoIaRepository;
        this.clock = clock;
        this.limitePorUsuario = limitePorUsuario;
        this.limiteGlobal = limiteGlobal;
    }

    @Transactional(readOnly = true)
    public void verificarCupo(Long usuarioId) {
        LocalDate hoy = hoy();

        if (usoIaRepository.totalDelDia(hoy) >= limiteGlobal) {
            throw new CupoGlobalAgotadoException(
                    "El cupo diario de análisis del servicio se agotó. Volvé a intentar mañana.");
        }
        if (usoIaRepository.totalDelUsuarioEnElDia(usuarioId, hoy) >= limitePorUsuario) {
            throw new LimiteUsuarioExcedidoException(
                    "Alcanzaste el límite de " + limitePorUsuario + " análisis por día. Volvé a intentar mañana.");
        }
    }

    @Transactional
    public void registrarUso(Long usuarioId) {
        usoIaRepository.registrarUso(usuarioId, hoy());
    }

    private LocalDate hoy() {
        return LocalDate.now(clock.withZone(ZONA_DEL_CUPO));
    }
}