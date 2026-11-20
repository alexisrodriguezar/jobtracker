package jobtracker.service;

import jobtracker.dto.AnalisisResponse;
import jobtracker.dto.AnalisisResultado;
import jobtracker.entity.Postulacion;
import jobtracker.entity.Usuario;
import jobtracker.exception.CredencialesInvalidasException;
import jobtracker.exception.LlmRespuestaInvalidaException;
import jobtracker.exception.RecursoNoEncontradoException;
import jobtracker.exception.SolicitudInvalidaException;
import jobtracker.llm.LlmClient;
import jobtracker.repository.PostulacionRepository;
import jobtracker.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AnalisisService {

    private final UsuarioRepository usuarioRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsoIaService usoIaService;
    private final LlmClient llmClient;
    private final AnalisisJson analisisJson;
    private final Clock clock;

    public AnalisisResponse analizar(String email, Long postulacionId) {
        Usuario usuario = obtenerUsuario(email);
        Postulacion postulacion = buscarPropia(usuario, postulacionId);

        if (estaVacio(postulacion.getDescripcionOferta())) {
            throw new SolicitudInvalidaException(
                    "Esta postulación no tiene descripción de la oferta. Guardala con PATCH /postulaciones/{id}.");
        }
        if (estaVacio(usuario.getPerfil())) {
            throw new SolicitudInvalidaException(
                    "Todavía no cargaste tu perfil de habilidades. Guardalo con PUT /perfil.");
        }

        usoIaService.verificarCupo(usuario.getId());

        String respuestaIa = llmClient.generarJson(
                AnalisisPrompt.INSTRUCCIONES,
                AnalisisPrompt.contenido(usuario.getPerfil(), postulacion.getDescripcionOferta()));

        AnalisisResultado resultado = analisisJson.leer(respuestaIa).sanear();
        String json = analisisJson.escribir(resultado);
        if (json.length() > Postulacion.MAX_LARGO_ANALISIS) {
            throw new LlmRespuestaInvalidaException("La respuesta de la IA es demasiado larga para guardarse. Probá de nuevo.");
        }

        LocalDateTime ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        postulacion.setAnalisisJson(json);
        postulacion.setAnalisisFecha(ahora);
        postulacionRepository.save(postulacion);

        // Recién ahora, con el análisis guardado, se descuenta del cupo
        usoIaService.registrarUso(usuario.getId());

        return AnalisisResponse.de(resultado, ahora);
    }

    @Transactional(readOnly = true)
    public AnalisisResponse obtenerUltimo(String email, Long postulacionId) {
        Usuario usuario = obtenerUsuario(email);
        Postulacion postulacion = buscarPropia(usuario, postulacionId);

        if (postulacion.getAnalisisJson() == null) {
            throw new RecursoNoEncontradoException("Esta postulación todavía no tiene un análisis.");
        }
        AnalisisResultado resultado = analisisJson.leerGuardado(postulacion.getAnalisisJson());
        return AnalisisResponse.de(resultado, postulacion.getAnalisisFecha());
    }

    private Usuario obtenerUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException("Sesión inválida"));
    }

    private Postulacion buscarPropia(Usuario usuario, Long id) {
        return postulacionRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación no encontrada"));
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}