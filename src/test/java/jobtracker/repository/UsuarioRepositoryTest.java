package jobtracker.repository;

import jakarta.persistence.EntityManager;
import jobtracker.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardar_perfilConElLargoMaximo_loGuardaYLoLeeCompleto() {
        String perfil = "x".repeat(Usuario.MAX_LARGO_PERFIL);
        Usuario usuario = nuevoUsuario("ana@mail.com");
        usuario.setPerfil(perfil);

        usuarioRepository.saveAndFlush(usuario);
        entityManager.clear();

        Usuario leido = usuarioRepository.findByEmail("ana@mail.com").orElseThrow();
        assertEquals(perfil, leido.getPerfil());
    }

    @Test
    void guardar_perfilMasLargoQueElMaximo_lanzaExcepcionDeLaBase() {
        Usuario usuario = nuevoUsuario("beto@mail.com");
        usuario.setPerfil("x".repeat(Usuario.MAX_LARGO_PERFIL + 1));

        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(usuario));
    }

    private Usuario nuevoUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Nombre");
        usuario.setApellido("Apellido");
        usuario.setEmail(email);
        usuario.setPasswordHash("hash-falso");
        return usuario;
    }
}