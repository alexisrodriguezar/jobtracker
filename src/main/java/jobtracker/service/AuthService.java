package jobtracker.service;

import jobtracker.dto.RegisterRequest;
import jobtracker.dto.UsuarioResponse;
import jobtracker.entity.Usuario;
import jobtracker.exception.EmailYaRegistradoException;
import jobtracker.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponse register(RegisterRequest request){
        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(email)){
            throw new EmailYaRegistradoException("El correo electrónico ya se encuentra registrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));

        Usuario guardado = usuarioRepository.save(usuario);

        return new UsuarioResponse(guardado.getId(), guardado.getNombre(), guardado.getApellido(), guardado.getEmail());
    }
}
