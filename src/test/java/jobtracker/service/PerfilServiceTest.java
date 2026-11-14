package jobtracker.service;

import jobtracker.dto.PerfilRequest;
import jobtracker.dto.PerfilResponse;
import jobtracker.entity.Usuario;
import jobtracker.exception.CredencialesInvalidasException;
import jobtracker.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final String EMAIL = "ana@mail.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PerfilService perfilService;

    @Test
    void obtener_conPerfilGuardado_loDevuelve() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioConPerfil("Java y Spring Boot")));

        PerfilResponse response = perfilService.obtener(EMAIL);

        assertEquals("Java y Spring Boot", response.perfil());
    }

    @Test
    void obtener_sinPerfilCargado_devuelveNull() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioConPerfil(null)));

        PerfilResponse response = perfilService.obtener(EMAIL);

        assertNull(response.perfil());
    }

    @Test
    void actualizar_guardaElTextoSinEspaciosAlrededor() {
        Usuario usuario = usuarioConPerfil(null);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        PerfilResponse response = perfilService.actualizar(EMAIL, new PerfilRequest("  Java, Spring Boot y MySQL  "));

        assertEquals("Java, Spring Boot y MySQL", response.perfil());
        assertEquals("Java, Spring Boot y MySQL", usuario.getPerfil());
    }

    @Test
    void actualizar_reemplazaElPerfilAnterior() {
        Usuario usuario = usuarioConPerfil("Perfil anterior");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        PerfilResponse response = perfilService.actualizar(EMAIL, new PerfilRequest("Perfil nuevo"));

        assertEquals("Perfil nuevo", response.perfil());
        assertEquals("Perfil nuevo", usuario.getPerfil());
    }

    @Test
    void actualizar_textoEnBlanco_borraElPerfil() {
        Usuario usuario = usuarioConPerfil("Perfil anterior");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        PerfilResponse response = perfilService.actualizar(EMAIL, new PerfilRequest("   "));

        assertNull(response.perfil());
        assertNull(usuario.getPerfil());
    }

    @Test
    void actualizar_usuarioInexistente_lanzaCredencialesInvalidas() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> perfilService.actualizar(EMAIL, new PerfilRequest("Java")));
    }

    private Usuario usuarioConPerfil(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");
        usuario.setEmail(EMAIL);
        usuario.setPasswordHash("hash-falso");
        usuario.setPerfil(perfil);
        return usuario;
    }
}