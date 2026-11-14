package jobtracker.service;

import jobtracker.dto.PerfilRequest;
import jobtracker.dto.PerfilResponse;
import jobtracker.entity.Usuario;
import jobtracker.exception.CredencialesInvalidasException;
import jobtracker.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public PerfilResponse obtener(String email) {
        return new PerfilResponse(buscarUsuario(email).getPerfil());
    }

    @Transactional
    public PerfilResponse actualizar(String email, PerfilRequest request) {
        Usuario usuario = buscarUsuario(email);

        String texto = request.perfil().isBlank() ? null : request.perfil().trim();
        usuario.setPerfil(texto);

        return new PerfilResponse(usuario.getPerfil());
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException("Sesión inválida"));
    }
}