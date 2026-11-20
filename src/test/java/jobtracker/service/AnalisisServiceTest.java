package jobtracker.service;

import jobtracker.dto.AnalisisResponse;
import jobtracker.dto.AnalisisResultado;
import jobtracker.entity.Postulacion;
import jobtracker.entity.Usuario;
import jobtracker.exception.LimiteUsuarioExcedidoException;
import jobtracker.exception.LlmNoDisponibleException;
import jobtracker.exception.LlmRespuestaInvalidaException;
import jobtracker.exception.RecursoNoEncontradoException;
import jobtracker.exception.SolicitudInvalidaException;
import jobtracker.llm.LlmClient;
import jobtracker.repository.PostulacionRepository;
import jobtracker.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalisisServiceTest {

    private static final String EMAIL = "ana@mail.com";
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T15:30:45.123Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 15, 30, 45);

    private static final String RESPUESTA_OK = """
            {"porcentajeCoincidencia": 72,
             "habilidadesRequeridas": ["Java", "Spring Boot", "MySQL"],
             "habilidadesQueTenes": ["Java", "Spring Boot"],
             "habilidadesFaltantes": ["MySQL"],
             "resumen": "Buen encaje con el puesto.",
             "sugerencias": ["Sumá un proyecto con MySQL"]}
            """;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PostulacionRepository postulacionRepository;
    @Mock
    private UsoIaService usoIaService;
    @Mock
    private LlmClient llmClient;

    private final AnalisisJson analisisJson = new AnalisisJson(JsonMapper.builder().build());
    private AnalisisService analisisService;

    @BeforeEach
    void preparar() {
        analisisService = new AnalisisService(
                usuarioRepository, postulacionRepository, usoIaService, llmClient, analisisJson, RELOJ);
    }

    @Test
    void analizar_conDatosCompletos_guardaElAnalisisRegistraElUsoYLoDevuelve() {
        Postulacion postulacion = prepararCasoFeliz();

        AnalisisResponse response = analisisService.analizar(EMAIL, 100L);

        assertEquals(72, response.porcentajeCoincidencia());
        assertEquals(List.of("MySQL"), response.habilidadesFaltantes());
        assertEquals(AHORA, response.fecha());
        assertEquals(AHORA, postulacion.getAnalisisFecha());
        assertTrue(postulacion.getAnalisisJson().contains("\"porcentajeCoincidencia\":72"));
        verify(postulacionRepository).save(postulacion);

        InOrder orden = inOrder(usoIaService, llmClient);
        orden.verify(usoIaService).verificarCupo(1L);
        orden.verify(llmClient).generarJson(any(), any());
        orden.verify(usoIaService).registrarUso(1L);
    }

    @Test
    void analizar_enviaAlModeloLasInstruccionesYElContenidoDelimitado() {
        prepararCasoFeliz();

        analisisService.analizar(EMAIL, 100L);

        ArgumentCaptor<String> instrucciones = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contenido = ArgumentCaptor.forClass(String.class);
        verify(llmClient).generarJson(instrucciones.capture(), contenido.capture());
        assertEquals(AnalisisPrompt.INSTRUCCIONES, instrucciones.getValue());
        assertTrue(contenido.getValue().contains("<perfil>\nJava, Spring Boot y MySQL\n</perfil>"));
        assertTrue(contenido.getValue().contains("Buscamos dev Java con Spring Boot"));
    }

    @Test
    void analizar_sinDescripcionDeLaOferta_lanzaSolicitudInvalidaYNoLlamaALaIa() {
        Usuario ana = usuarioConPerfil("Java");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L))
                .thenReturn(Optional.of(postulacionConOferta(ana, "   ")));

        assertThrows(SolicitudInvalidaException.class, () -> analisisService.analizar(EMAIL, 100L));

        verify(llmClient, never()).generarJson(any(), any());
        verify(usoIaService, never()).registrarUso(any());
    }

    @Test
    void analizar_sinPerfilCargado_lanzaSolicitudInvalidaYNoLlamaALaIa() {
        Usuario ana = usuarioConPerfil(null);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L))
                .thenReturn(Optional.of(postulacionConOferta(ana, "Buscamos dev Java")));

        assertThrows(SolicitudInvalidaException.class, () -> analisisService.analizar(EMAIL, 100L));

        verify(llmClient, never()).generarJson(any(), any());
    }

    @Test
    void analizar_postulacionDeOtroUsuario_lanzaRecursoNoEncontradoYNoGastaCupo() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioConPerfil("Java")));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> analisisService.analizar(EMAIL, 100L));

        verify(usoIaService, never()).verificarCupo(any());
        verify(llmClient, never()).generarJson(any(), any());
    }

    @Test
    void analizar_sinCupoDisponible_propagaElErrorYNoLlamaALaIa() {
        Usuario ana = usuarioConPerfil("Java");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L))
                .thenReturn(Optional.of(postulacionConOferta(ana, "Buscamos dev Java")));
        doThrow(new LimiteUsuarioExcedidoException("Alcanzaste el límite")).when(usoIaService).verificarCupo(1L);

        assertThrows(LimiteUsuarioExcedidoException.class, () -> analisisService.analizar(EMAIL, 100L));

        verify(llmClient, never()).generarJson(any(), any());
        verify(usoIaService, never()).registrarUso(any());
    }

    @Test
    void analizar_laIaNoEstaDisponible_propagaElErrorYNoGuardaNiDescuentaCupo() {
        prepararCasoFeliz();
        when(llmClient.generarJson(any(), any())).thenThrow(new LlmNoDisponibleException("Servicio caído"));

        assertThrows(LlmNoDisponibleException.class, () -> analisisService.analizar(EMAIL, 100L));

        verify(postulacionRepository, never()).save(any());
        verify(usoIaService, never()).registrarUso(any());
    }

    @Test
    void analizar_laIaNoDevuelveUnJson_lanzaRespuestaInvalidaYNoDescuentaCupo() {
        prepararCasoFeliz();
        when(llmClient.generarJson(any(), any())).thenReturn("esto no es un JSON");

        assertThrows(LlmRespuestaInvalidaException.class, () -> analisisService.analizar(EMAIL, 100L));

        verify(postulacionRepository, never()).save(any());
        verify(usoIaService, never()).registrarUso(any());
    }

    @Test
    void analizar_respuestaQueNoEntraEnLaColumna_lanzaRespuestaInvalidaYNoGuarda() {
        prepararCasoFeliz();
        // Cada comilla se escribe como \" en el JSON, así que estos textos ocupan el doble
        List<String> comillas = Collections.nCopies(12, "\"".repeat(50));
        String respuestaEnorme = analisisJson.escribir(new AnalisisResultado(
                80, comillas, comillas, comillas, "\"".repeat(400), Collections.nCopies(5, "\"".repeat(180))));
        when(llmClient.generarJson(any(), any())).thenReturn(respuestaEnorme);

        assertThrows(LlmRespuestaInvalidaException.class, () -> analisisService.analizar(EMAIL, 100L));

        verify(postulacionRepository, never()).save(any());
        verify(usoIaService, never()).registrarUso(any());
    }

    @Test
    void obtenerUltimo_conAnalisisGuardado_loDevuelveSinLlamarALaIa() {
        Usuario ana = usuarioConPerfil("Java");
        Postulacion postulacion = postulacionConOferta(ana, "Buscamos dev Java");
        postulacion.setAnalisisJson(analisisJson.escribir(new AnalisisResultado(
                65, List.of("Java"), List.of("Java"), List.of(), "Resumen guardado.", List.of("Sugerencia"))));
        postulacion.setAnalisisFecha(AHORA);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.of(postulacion));

        AnalisisResponse response = analisisService.obtenerUltimo(EMAIL, 100L);

        assertEquals(65, response.porcentajeCoincidencia());
        assertEquals("Resumen guardado.", response.resumen());
        assertEquals(AHORA, response.fecha());
        verify(llmClient, never()).generarJson(any(), any());
    }

    @Test
    void obtenerUltimo_sinAnalisisGuardado_lanzaRecursoNoEncontrado() {
        Usuario ana = usuarioConPerfil("Java");
        Postulacion postulacion = postulacionConOferta(ana, "Buscamos dev Java");
        assertNull(postulacion.getAnalisisJson());
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.of(postulacion));

        assertThrows(RecursoNoEncontradoException.class, () -> analisisService.obtenerUltimo(EMAIL, 100L));
    }

    @Test
    void obtenerUltimo_postulacionDeOtroUsuario_lanzaRecursoNoEncontrado() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuarioConPerfil("Java")));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> analisisService.obtenerUltimo(EMAIL, 100L));
    }

    private Postulacion prepararCasoFeliz() {
        Usuario ana = usuarioConPerfil("Java, Spring Boot y MySQL");
        Postulacion postulacion = postulacionConOferta(ana, "Buscamos dev Java con Spring Boot");
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(ana));
        when(postulacionRepository.findByIdAndUsuarioId(100L, 1L)).thenReturn(Optional.of(postulacion));
        when(llmClient.generarJson(any(), any())).thenReturn(RESPUESTA_OK);
        return postulacion;
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

    private Postulacion postulacionConOferta(Usuario usuario, String descripcion) {
        Postulacion postulacion = new Postulacion();
        postulacion.setId(100L);
        postulacion.setUsuario(usuario);
        postulacion.setPuesto("Backend Jr");
        postulacion.setDescripcionOferta(descripcion);
        return postulacion;
    }
}