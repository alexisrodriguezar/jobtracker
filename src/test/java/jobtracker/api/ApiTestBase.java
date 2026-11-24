package jobtracker.api;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import jobtracker.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
abstract class ApiTestBase {

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;
    protected static final String AUTH = "Authorization";

    protected static final String RESPUESTA_IA = """
            {"porcentajeCoincidencia": 72,
             "habilidadesRequeridas": ["Java", "Spring Boot", "MySQL"],
             "habilidadesQueTenes": ["Java", "Spring Boot"],
             "habilidadesFaltantes": ["MySQL"],
             "resumen": "Buen encaje con el puesto.",
             "sugerencias": ["Suma un proyecto con MySQL"]}
            """;

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter filtrosDeSeguridad;

    @MockitoBean
    protected LlmClient llmClient;

    protected MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(filtrosDeSeguridad)
                .build();
    }

    protected String emailUnico() {
        return "usuario-" + UUID.randomUUID() + "@mail.com";
    }

    protected String empresaUnica() {
        return "Empresa " + UUID.randomUUID();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected void registrar(String email) throws Exception {
        mvc.perform(post("/auth/register").contentType(JSON).content("""
                        {"nombre":"Ana","apellido":"Perez","email":"%s","password":"password123"}
                        """.formatted(email)))
                .andExpect(status().isCreated());
    }

    protected String loguear(String email, String password) throws Exception {
        MvcResult resultado = mvc.perform(post("/auth/login").contentType(JSON).content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(cuerpo(resultado), "$.token");
        return token;
    }

    protected String registrarYLoguear() throws Exception {
        String email = emailUnico();
        registrar(email);
        return loguear(email, "password123");
    }

    protected void guardarPerfil(String token, String perfil) throws Exception {
        mvc.perform(put("/perfil").header(AUTH, bearer(token)).contentType(JSON).content("""
                        {"perfil":"%s"}
                        """.formatted(perfil)))
                .andExpect(status().isOk());
    }

    protected long crearPostulacion(String token, String descripcionOferta) throws Exception {
        String cuerpoJson = descripcionOferta == null
                ? """
                {"empresa":"%s","puesto":"Backend Jr"}
                """.formatted(empresaUnica())
                : """
                {"empresa":"%s","puesto":"Backend Jr","descripcionOferta":"%s"}
                """.formatted(empresaUnica(), descripcionOferta);

        MvcResult resultado = mvc.perform(post("/postulaciones")
                        .header(AUTH, bearer(token)).contentType(JSON).content(cuerpoJson))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(cuerpo(resultado), "$.id")).longValue();
    }
}