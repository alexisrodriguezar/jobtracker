package jobtracker.repository;

import jobtracker.entity.UsoIa;
import jobtracker.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class UsoIaRepositoryTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 15);
    private static final LocalDate AYER = LocalDate.of(2026, 11, 14);

    @Autowired
    private UsoIaRepository usoIaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario ana;
    private Usuario beto;

    @BeforeEach
    void cargarUsuarios() {
        ana = usuarioRepository.save(nuevoUsuario("ana@mail.com"));
        beto = usuarioRepository.save(nuevoUsuario("beto@mail.com"));
    }

    @Test
    void registrarUso_primeraVez_creaUnaFilaConCantidadUno() {
        usoIaRepository.registrarUso(ana.getId(), HOY);

        List<UsoIa> filasDeAna = filasDe(ana);
        assertEquals(1, filasDeAna.size());
        assertEquals(1, filasDeAna.get(0).getCantidad());
        assertEquals(HOY, filasDeAna.get(0).getFecha());
    }

    @Test
    void registrarUso_variasVecesElMismoDia_incrementaLaCantidadSinCrearFilasNuevas() {
        usoIaRepository.registrarUso(ana.getId(), HOY);
        usoIaRepository.registrarUso(ana.getId(), HOY);
        usoIaRepository.registrarUso(ana.getId(), HOY);

        assertEquals(3, usoIaRepository.totalDelUsuarioEnElDia(ana.getId(), HOY));
        assertEquals(1, filasDe(ana).size());
    }

    @Test
    void totalDelDia_sumaLosAnalisisDeTodosLosUsuariosDeEseDia() {
        usoIaRepository.registrarUso(ana.getId(), HOY);
        usoIaRepository.registrarUso(ana.getId(), HOY);
        usoIaRepository.registrarUso(beto.getId(), HOY);
        usoIaRepository.registrarUso(beto.getId(), AYER);

        assertEquals(3, usoIaRepository.totalDelDia(HOY));
        assertEquals(1, usoIaRepository.totalDelDia(AYER));
    }

    @Test
    void totalDelUsuarioEnElDia_soloCuentaAlUsuarioYAlDiaIndicados() {
        usoIaRepository.registrarUso(ana.getId(), HOY);
        usoIaRepository.registrarUso(ana.getId(), HOY);
        usoIaRepository.registrarUso(ana.getId(), AYER);
        usoIaRepository.registrarUso(beto.getId(), HOY);

        assertEquals(2, usoIaRepository.totalDelUsuarioEnElDia(ana.getId(), HOY));
        assertEquals(1, usoIaRepository.totalDelUsuarioEnElDia(ana.getId(), AYER));
        assertEquals(1, usoIaRepository.totalDelUsuarioEnElDia(beto.getId(), HOY));
    }

    @Test
    void totales_sinNingunUso_devuelvenCero() {
        assertEquals(0, usoIaRepository.totalDelDia(HOY));
        assertEquals(0, usoIaRepository.totalDelUsuarioEnElDia(ana.getId(), HOY));
    }

    private Usuario nuevoUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Nombre");
        usuario.setApellido("Apellido");
        usuario.setEmail(email);
        usuario.setPasswordHash("hash-falso");
        return usuario;
    }

    private List<UsoIa> filasDe(Usuario usuario) {
        return usoIaRepository.findAll().stream()
                .filter(uso -> uso.getUsuario().getId().equals(usuario.getId()))
                .toList();
    }
}