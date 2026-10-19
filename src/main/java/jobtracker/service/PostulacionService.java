package jobtracker.service;

import jobtracker.dto.PostulacionRequest;
import jobtracker.dto.PostulacionResponse;
import jobtracker.entity.Empresa;
import jobtracker.entity.Estado;
import jobtracker.entity.Postulacion;
import jobtracker.entity.Usuario;
import jobtracker.exception.CredencialesInvalidasException;
import jobtracker.exception.RecursoNoEncontradoException;
import jobtracker.repository.EmpresaRepository;
import jobtracker.repository.PostulacionRepository;
import jobtracker.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PostulacionService {

    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public PostulacionResponse crear(String email, PostulacionRequest request) {
        Postulacion postulacion = new Postulacion();
        postulacion.setUsuario(obtenerUsuario(email));
        postulacion.setEmpresa(obtenerOCrearEmpresa(request.empresa()));
        postulacion.setEstado(Estado.ENVIADA);
        aplicarDatos(postulacion, request);
        return toResponse(postulacionRepository.save(postulacion));
    }

    @Transactional(readOnly = true)
    public Page<PostulacionResponse> listar(String email, Estado estado, String buscar, Pageable pageable) {
        Usuario usuario = obtenerUsuario(email);
        String texto = (buscar == null || buscar.isBlank()) ? null : buscar.trim();
        return postulacionRepository.buscar(usuario.getId(), estado, texto, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PostulacionResponse obtener(String email, Long id) {
        return toResponse(buscarPropia(email, id));
    }

    @Transactional
    public PostulacionResponse actualizar(String email, Long id, PostulacionRequest request) {
        Postulacion postulacion = buscarPropia(email, id);
        postulacion.setEmpresa(obtenerOCrearEmpresa(request.empresa()));
        aplicarDatos(postulacion, request);
        return toResponse(postulacion);
    }

    @Transactional
    public PostulacionResponse cambiarEstado(String email, Long id, Estado nuevoEstado) {
        Postulacion postulacion = buscarPropia(email, id);
        postulacion.setEstado(nuevoEstado);
        return toResponse(postulacion);
    }

    @Transactional
    public void eliminar(String email, Long id) {
        Postulacion postulacion = buscarPropia(email, id);
        postulacionRepository.delete(postulacion);
    }

    private Usuario obtenerUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException("Sesión inválida"));
    }

    private Postulacion buscarPropia(String email, Long id) {
        Usuario usuario = obtenerUsuario(email);
        return postulacionRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación no encontrada"));
    }

    private Empresa obtenerOCrearEmpresa(String nombre) {
        String limpio = nombre.trim();
        return empresaRepository.findByNombreIgnoreCase(limpio)
                .orElseGet(() -> empresaRepository.save(new Empresa(null, limpio)));
    }

    private void aplicarDatos(Postulacion p, PostulacionRequest r) {
        p.setPuesto(r.puesto().trim());
        p.setLink(r.link());
        p.setNota(r.nota());
        p.setSeniority(r.seniority());
        if (r.fechaPostulacion() != null) {
            p.setFechaPostulacion(r.fechaPostulacion());
        } else if (p.getFechaPostulacion() == null) {
            p.setFechaPostulacion(LocalDate.now());
        }
    }

    private PostulacionResponse toResponse(Postulacion p) {
        return new PostulacionResponse(p.getId(), p.getEmpresa().getNombre(), p.getPuesto(),
                p.getEstado(), p.getSeniority(), p.getLink(), p.getNota(), p.getFechaPostulacion());
    }
}