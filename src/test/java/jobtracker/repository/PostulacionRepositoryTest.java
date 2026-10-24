package jobtracker.repository;

import jobtracker.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PostulacionRepositoryTest {

    private static final Pageable PAGINA = PageRequest.of(0, 10);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PostulacionRepository postulacionRepository;

    private Usuario ana;
    private Usuario beto;
    private Postulacion backendDeAna;
    private Postulacion qaDeAna;
    private Postulacion dataDeAna;

    @BeforeEach
    void cargarDatos() {
        ana = guardarUsuario("ana@mail.com");
        beto = guardarUsuario("beto@mail.com");
        Empresa ey = guardarEmpresa("EY");
        Empresa mercadoLibre = guardarEmpresa("Mercado Libre");

        backendDeAna = guardarPostulacion(ana, ey, "Backend Developer", Estado.ENVIADA, LocalDate.of(2026, 10, 1));
        qaDeAna = guardarPostulacion(ana, mercadoLibre, "QA Analyst", Estado.ENTREVISTA, LocalDate.of(2026, 10, 2));
        dataDeAna = guardarPostulacion(ana, ey, "Data Analyst", Estado.ENVIADA, LocalDate.of(2026, 10, 3));

        guardarPostulacion(beto, ey, "Backend Developer", Estado.ENVIADA, LocalDate.of(2026, 10, 4));
    }

    @Test
    void buscar_sinFiltros_devuelveSoloLasPostulacionesDelUsuario() {
        Page<Postulacion> pagina = postulacionRepository.buscar(ana.getId(), null, null, PAGINA);

        assertEquals(3, pagina.getTotalElements());
        assertTrue(pagina.getContent().stream()
                .allMatch(p -> p.getUsuario().getId().equals(ana.getId())));
    }

    @Test
    void buscar_conEstado_devuelveSoloLasDeEsteEstado() {
        Page<Postulacion> pagina = postulacionRepository.buscar(ana.getId(), Estado.ENVIADA, null, PAGINA);

        assertEquals(2, pagina.getTotalElements());
        assertTrue(pagina.getContent().stream()
                .allMatch(p -> p.getEstado() == Estado.ENVIADA));
    }

    @Test
    void buscar_textoEnElPuesto_ignoraMayusculasYNoTraeLasDeOtroUsuario() {
        Page<Postulacion> pagina = postulacionRepository.buscar(ana.getId(), null, "BACKEND", PAGINA);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(backendDeAna.getId(), pagina.getContent().get(0).getId());
    }

    @Test
    void buscar_textoEnElNombreDeLaEmpresa_laEncuentra() {
        Page<Postulacion> pagina = postulacionRepository.buscar(ana.getId(), null, "mercado", PAGINA);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(qaDeAna.getId(), pagina.getContent().get(0).getId());
    }

    @Test
    void buscar_estadoYTexto_combinaAmbosFiltros() {
        Page<Postulacion> pagina = postulacionRepository.buscar(ana.getId(), Estado.ENVIADA, "analyst", PAGINA);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(dataDeAna.getId(), pagina.getContent().get(0).getId());
    }

    @Test
    void buscar_conPaginacionYOrden_respetaTamanioTotalYOrdenDescendentePorFecha() {
        Pageable primeraPagina = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "fechaPostulacion"));

        Page<Postulacion> pagina = postulacionRepository.buscar(ana.getId(), null, null, primeraPagina);

        assertEquals(2, pagina.getContent().size());
        assertEquals(3, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
        assertEquals(dataDeAna.getId(), pagina.getContent().get(0).getId());
        assertEquals(qaDeAna.getId(), pagina.getContent().get(1).getId());
    }

    @Test
    void findByIdAndUsuarioId_conSuDueno_laEncuentra() {
        Optional<Postulacion> resultado =
                postulacionRepository.findByIdAndUsuarioId(backendDeAna.getId(), ana.getId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void findByIdAndUsuarioId_conOtroUsuario_devuelveVacio() {
        Optional<Postulacion> resultado =
                postulacionRepository.findByIdAndUsuarioId(backendDeAna.getId(), beto.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void usuario_conEmailDuplicado_lanzaExcepcionDeLaBase() {
        Usuario duplicado = nuevoUsuario("ana@mail.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(duplicado));
    }

    @Test
    void empresa_conNombreDuplicado_lanzaExcepcionDeLaBase() {
        Empresa duplicada = nuevaEmpresa("EY");

        assertThrows(DataIntegrityViolationException.class,
                () -> empresaRepository.saveAndFlush(duplicada));
    }

    @Test
    void empresa_mismoNombreConOtraCapitalizacion_tambienLanzaExcepcion() {
        Empresa otraCapitalizacion = nuevaEmpresa("ey");

        assertThrows(DataIntegrityViolationException.class,
                () -> empresaRepository.saveAndFlush(otraCapitalizacion));
    }

    @Test
    void empresa_findByNombreIgnoreCase_laEncuentraSinImportarMayusculas() {
        Optional<Empresa> resultado = empresaRepository.findByNombreIgnoreCase("ey");

        assertTrue(resultado.isPresent());
        assertEquals("EY", resultado.get().getNombre());
    }

    private Usuario nuevoUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Nombre");
        usuario.setApellido("Apellido");
        usuario.setEmail(email);
        usuario.setPasswordHash("hash-falso");
        return usuario;
    }

    private Usuario guardarUsuario(String email) {
        return usuarioRepository.save(nuevoUsuario(email));
    }

    private Empresa nuevaEmpresa(String nombre) {
        Empresa empresa = new Empresa();
        empresa.setNombre(nombre);
        return empresa;
    }

    private Empresa guardarEmpresa(String nombre) {
        return empresaRepository.save(nuevaEmpresa(nombre));
    }

    private Postulacion guardarPostulacion(Usuario usuario, Empresa empresa, String puesto,
                                           Estado estado, LocalDate fecha) {
        Postulacion postulacion = new Postulacion();
        postulacion.setUsuario(usuario);
        postulacion.setEmpresa(empresa);
        postulacion.setPuesto(puesto);
        postulacion.setEstado(estado);
        postulacion.setSeniority(Seniority.JUNIOR);
        postulacion.setFechaPostulacion(fecha);
        return postulacionRepository.save(postulacion);
    }
}