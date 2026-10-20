package jobtracker.service;

import jobtracker.dto.PostulacionRequest;
import jobtracker.dto.PostulacionResponse;
import jobtracker.entity.*;
import jobtracker.exception.CredencialesInvalidasException;
import jobtracker.exception.RecursoNoEncontradoException;
import jobtracker.repository.EmpresaRepository;
import jobtracker.repository.PostulacionRepository;
import jobtracker.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostulacionServiceTest {

    private static final String EMAIL_ANA = "ana@mail.com";
    private static final String EMAIL_BETO = "beto@mail.com";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 1);

    @Mock
    private PostulacionRepository postulacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private PostulacionService postulacionService;


    @Test
    void crear_empresaExistente_reutilizaLaEmpresaYGuardaPostulacionEnviada() {
        Usuario ana = ana();
        Empresa ey = ey();
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana));
        when(empresaRepository.findByNombreIgnoreCase("ey")).thenReturn(Optional.of(ey));
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(invocacion -> {
            Postulacion p = invocacion.getArgument(0);
            p.setId(100L);
            return p;
        });

        PostulacionResponse response = postulacionService.crear(EMAIL_ANA, request("ey", FECHA));

        ArgumentCaptor<Postulacion> captor = ArgumentCaptor.forClass(Postulacion.class);
        verify(postulacionRepository).save(captor.capture());
        Postulacion guardada = captor.getValue();

        assertSame(ana, guardada.getUsuario());
        assertSame(ey, guardada.getEmpresa());
        assertEquals(Estado.ENVIADA, guardada.getEstado());
        verify(empresaRepository, never()).save(any());

        assertEquals(100L, response.id());
        assertEquals("EY", response.empresa());
        assertEquals(Estado.ENVIADA, response.estado());
    }

    @Test
    void crear_empresaNueva_laCreaSinEspaciosYLaAsociaALaPostulacion() {
        Usuario ana = ana();
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana));
        when(empresaRepository.findByNombreIgnoreCase("Mercado Libre")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacion -> {
            Empresa e = invocacion.getArgument(0);
            e.setId(20L);
            return e;
        });
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PostulacionResponse response = postulacionService.crear(EMAIL_ANA, request("  Mercado Libre ", FECHA));

        ArgumentCaptor<Empresa> empresaCaptor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(empresaCaptor.capture());
        assertEquals("Mercado Libre", empresaCaptor.getValue().getNombre());

        assertEquals("Mercado Libre", response.empresa());
    }

    @Test
    void crear_sinFecha_usaLaFechaDeHoy() {
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana()));
        when(empresaRepository.findByNombreIgnoreCase("EY")).thenReturn(Optional.of(ey()));
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PostulacionResponse response = postulacionService.crear(EMAIL_ANA, request("EY", null));

        assertEquals(LocalDate.now(), response.fechaPostulacion());
    }

    @Test
    void crear_usuarioInexistente_lanzaCredencialesInvalidasYNoGuardaNada() {
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> postulacionService.crear(EMAIL_ANA, request("EY", FECHA)));

        verify(postulacionRepository, never()).save(any());
        verify(empresaRepository, never()).save(any());
    }

    @Test
    void listar_sinFiltros_devuelveLaPaginaMapeadaAResponse() {
        Usuario ana = ana();
        Pageable pageable = PageRequest.of(0, 10);
        Postulacion postulacion = postulacionDe(ana, ey());

        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana));
        when(postulacionRepository.buscar(1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(postulacion), pageable, 1));

        Page<PostulacionResponse> pagina = postulacionService.listar(EMAIL_ANA, null, null, pageable);

        assertEquals(1, pagina.getTotalElements());
        PostulacionResponse primera = pagina.getContent().get(0);
        assertEquals(100L, primera.id());
        assertEquals("EY", primera.empresa());
        assertEquals("Backend Jr", primera.puesto());
    }

    @Test
    void listar_textoEnBlanco_seTrataComoSinFiltroDeTexto() {
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana()));
        when(postulacionRepository.buscar(1L, null, null, pageable)).thenReturn(Page.empty(pageable));

        postulacionService.listar(EMAIL_ANA, null, "   ", pageable);

        verify(postulacionRepository).buscar(1L, null, null, pageable);
    }

    @Test
    void listar_conFiltros_pasaElEstadoYElTextoSinEspacios() {
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana()));
        when(postulacionRepository.buscar(1L, Estado.ENTREVISTA, "back", pageable))
                .thenReturn(Page.empty(pageable));

        postulacionService.listar(EMAIL_ANA, Estado.ENTREVISTA, "  back ", pageable);

        verify(postulacionRepository).buscar(1L, Estado.ENTREVISTA, "back", pageable);
    }

    @Test
    void obtener_postulacionPropia_devuelveElResponse() {
        Usuario ana = ana();
        Postulacion postulacion = postulacionDe(ana, ey());
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.of(postulacion));

        PostulacionResponse response = postulacionService.obtener(EMAIL_ANA, 100L);

        assertEquals(100L, response.id());
        assertEquals("EY", response.empresa());
        assertEquals("Backend Jr", response.puesto());
        assertEquals(Estado.ENVIADA, response.estado());
        assertEquals(Seniority.JUNIOR, response.seniority());
        assertEquals(FECHA, response.fechaPostulacion());
    }

    @Test
    void obtener_postulacionDeOtroUsuario_lanzaRecursoNoEncontrado() {
        Usuario beto = beto();
        when(usuarioRepository.findByEmail(EMAIL_BETO)).thenReturn(Optional.of(beto));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> postulacionService.obtener(EMAIL_BETO, 100L));

        verify(postulacionRepository).findByIdAndUsuarioId(100L, 2L);
        verify(postulacionRepository, never()).findById(any());
    }

    @Test
    void actualizar_postulacionPropia_cambiaLosDatosYConservaEstadoYFecha() {
        Usuario ana = ana();
        Postulacion postulacion = postulacionDe(ana, ey());
        postulacion.setEstado(Estado.ENTREVISTA);

        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.of(postulacion));
        when(empresaRepository.findByNombreIgnoreCase("Mercado Libre")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PostulacionRequest request = new PostulacionRequest(
                "Mercado Libre", "Developer", "https://nuevo.com", "Nueva nota", Seniority.SEMI_SENIOR, null);
        PostulacionResponse response = postulacionService.actualizar(EMAIL_ANA, 100L, request);

        assertEquals("Mercado Libre", response.empresa());
        assertEquals("Developer", response.puesto());
        assertEquals("https://nuevo.com", response.link());
        assertEquals("Nueva nota", response.nota());
        assertEquals(Seniority.SEMI_SENIOR, response.seniority());

        assertEquals(Estado.ENTREVISTA, response.estado());
        assertEquals(FECHA, response.fechaPostulacion());
    }

    @Test
    void actualizar_postulacionDeOtroUsuario_lanzaRecursoNoEncontradoYNoTocaEmpresas() {
        when(usuarioRepository.findByEmail(EMAIL_BETO)).thenReturn(Optional.of(beto()));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> postulacionService.actualizar(EMAIL_BETO, 100L, request("Mercado Libre", null)));

        verify(empresaRepository, never()).findByNombreIgnoreCase(any());
        verify(empresaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_postulacionPropia_actualizaElEstado() {
        Usuario ana = ana();
        Postulacion postulacion = postulacionDe(ana, ey());
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.of(postulacion));

        PostulacionResponse response = postulacionService.cambiarEstado(EMAIL_ANA, 100L, Estado.ENTREVISTA);

        assertEquals(Estado.ENTREVISTA, response.estado());
        assertEquals(Estado.ENTREVISTA, postulacion.getEstado());
    }

    @Test
    void cambiarEstado_postulacionDeOtroUsuario_lanzaRecursoNoEncontradoYNoHaceNadaMas() {
        when(usuarioRepository.findByEmail(EMAIL_BETO)).thenReturn(Optional.of(beto()));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> postulacionService.cambiarEstado(EMAIL_BETO, 100L, Estado.RECHAZADA));

        verify(postulacionRepository).findByIdAndUsuarioId(100L, 2L);
        verifyNoMoreInteractions(postulacionRepository);
    }

    @Test
    void eliminar_postulacionPropia_laBorra() {
        Usuario ana = ana();
        Postulacion postulacion = postulacionDe(ana, ey());
        when(usuarioRepository.findByEmail(EMAIL_ANA)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.of(postulacion));

        postulacionService.eliminar(EMAIL_ANA, 100L);

        verify(postulacionRepository).delete(postulacion);
    }

    @Test
    void eliminar_postulacionDeOtroUsuario_lanzaRecursoNoEncontradoYNoBorra() {
        when(usuarioRepository.findByEmail(EMAIL_BETO)).thenReturn(Optional.of(beto()));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> postulacionService.eliminar(EMAIL_BETO, 100L));

        verify(postulacionRepository, never()).delete(any());
    }

    private Usuario ana() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");
        usuario.setEmail(EMAIL_ANA);
        usuario.setPasswordHash("hash-falso");
        return usuario;
    }

    private Usuario beto() {
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setNombre("Beto");
        usuario.setApellido("Gomez");
        usuario.setEmail(EMAIL_BETO);
        usuario.setPasswordHash("hash-falso");
        return usuario;
    }

    private Empresa ey() {
        Empresa empresa = new Empresa();
        empresa.setId(10L);
        empresa.setNombre("EY");
        return empresa;
    }

    private Postulacion postulacionDe(Usuario usuario, Empresa empresa) {
        Postulacion postulacion = new Postulacion();
        postulacion.setId(100L);
        postulacion.setUsuario(usuario);
        postulacion.setEmpresa(empresa);
        postulacion.setPuesto("Backend Jr");
        postulacion.setEstado(Estado.ENVIADA);
        postulacion.setSeniority(Seniority.JUNIOR);
        postulacion.setLink("https://ejemplo.com/oferta");
        postulacion.setNota("Nota original");
        postulacion.setFechaPostulacion(FECHA);
        return postulacion;
    }

    private PostulacionRequest request(String empresa, LocalDate fecha) {
        return new PostulacionRequest(
                empresa, "Backend Jr", "https://ejemplo.com/oferta", "Una nota", Seniority.JUNIOR, fecha);
    }
}
