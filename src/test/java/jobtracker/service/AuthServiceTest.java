package jobtracker.service;

import jobtracker.dto.LoginRequest;
import jobtracker.dto.LoginResponse;
import jobtracker.dto.RegisterRequest;
import jobtracker.dto.UsuarioResponse;
import jobtracker.entity.Usuario;
import jobtracker.exception.CredencialesInvalidasException;
import jobtracker.exception.EmailYaRegistradoException;
import jobtracker.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_datosValidos_guardaUsuarioNormalizadoConHashYDevuelveResponse() {
        RegisterRequest request = new RegisterRequest("Ana", "Perez", "  ANA@Mail.com ", "password123");

        when(usuarioRepository.existsByEmail("ana@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash-falso");

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario usuario = invocacion.getArgument(0);
            usuario.setId(1L);
            return usuario;
        });

        UsuarioResponse response = authService.register(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        assertEquals("ana@mail.com", guardado.getEmail());
        assertEquals("hash-falso", guardado.getPasswordHash());
        assertNotEquals("password123", guardado.getPasswordHash());

        assertEquals(1L, response.id());
        assertEquals("Ana", response.nombre());
        assertEquals("Perez", response.apellido());
        assertEquals("ana@mail.com", response.email());
    }

    @Test
    void register_emailYaRegistrado_lanzaExcepcionYNoGuarda() {
        RegisterRequest request = new RegisterRequest("Ana", "Perez", "ana@mail.com", "password123");
        when(usuarioRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThrows(EmailYaRegistradoException.class, () -> authService.register(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_credencialesValidas_devuelveToken() {
        Usuario usuario = usuarioDePrueba();
        LoginRequest request = new LoginRequest("  ANA@Mail.com ", "password123");

        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", "hash-falso")).thenReturn(true);
        when(jwtService.generarToken("ana@mail.com")).thenReturn("token-falso");

        LoginResponse response = authService.login(request);

        assertEquals("token-falso", response.token());
    }

    @Test
    void login_emailInexistente_lanzaCredencialesInvalidas() {
        LoginRequest request = new LoginRequest("nadie@mail.com", "password123");
        when(usuarioRepository.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
        verify(jwtService, never()).generarToken(any());
    }

    @Test
    void login_passwordIncorrecta_lanzaCredencialesInvalidas() {
        Usuario usuario = usuarioDePrueba();
        LoginRequest request = new LoginRequest("ana@mail.com", "clave-mala");

        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-mala", "hash-falso")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
        verify(jwtService, never()).generarToken(any());
    }

    @Test
    void login_emailInexistenteYPasswordIncorrecta_devuelvenElMismoMensaje() {
        Usuario usuario = usuarioDePrueba();
        LoginRequest emailInexistente = new LoginRequest("nadie@mail.com", "password123");
        LoginRequest passwordIncorrecta = new LoginRequest("ana@mail.com", "clave-mala");

        when(usuarioRepository.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-mala", "hash-falso")).thenReturn(false);

        CredencialesInvalidasException error1 =
                assertThrows(CredencialesInvalidasException.class, () -> authService.login(emailInexistente));
        CredencialesInvalidasException error2 =
                assertThrows(CredencialesInvalidasException.class, () -> authService.login(passwordIncorrecta));

        assertEquals(error1.getMessage(), error2.getMessage());
    }

    private Usuario usuarioDePrueba() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");
        usuario.setEmail("ana@mail.com");
        usuario.setPasswordHash("hash-falso");
        return usuario;
    }
}