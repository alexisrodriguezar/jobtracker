package jobtracker.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadApiTest extends ApiTestBase {

    @Test
    void rutaProtegida_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/postulaciones"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaProtegida_conTokenInvalido_devuelve401() throws Exception {
        mvc.perform(get("/postulaciones").header(AUTH, "Bearer esto.no.es.un.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registro_conEmailRepetido_devuelve409() throws Exception {
        String email = emailUnico();
        registrar(email);

        mvc.perform(post("/auth/register").contentType(JSON).content("""
                        {"nombre":"Otra","apellido":"Persona","email":"%s","password":"password123"}
                        """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registro_conDatosInvalidos_devuelve400ConUnErrorPorCampo() throws Exception {
        mvc.perform(post("/auth/register").contentType(JSON).content("""
                        {"nombre":"","apellido":"Perez","email":"no-es-un-email","password":"corta"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void login_conClaveIncorrectaOEmailInexistente_devuelve401ConElMismoMensaje() throws Exception {
        String email = emailUnico();
        registrar(email);

        MvcResult claveIncorrecta = mvc.perform(post("/auth/login").contentType(JSON).content("""
                        {"email":"%s","password":"clave-incorrecta"}
                        """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult emailInexistente = mvc.perform(post("/auth/login").contentType(JSON).content("""
                        {"email":"%s","password":"password123"}
                        """.formatted(emailUnico())))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String mensaje1 = JsonPath.read(cuerpo(claveIncorrecta), "$.mensaje");
        String mensaje2 = JsonPath.read(cuerpo(emailInexistente), "$.mensaje");
        assertEquals(mensaje1, mensaje2);
    }

    @Test
    void cuerpoQueNoEsJson_devuelve400() throws Exception {
        mvc.perform(post("/auth/login").contentType(JSON).content("{esto no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unUsuarioNoPuedeVerModificarNiBorrarLasPostulacionesDeOtro() throws Exception {
        String tokenAna = registrarYLoguear();
        String tokenBeto = registrarYLoguear();
        long idDeAna = crearPostulacion(tokenAna, "Oferta de Ana");

        mvc.perform(get("/postulaciones/{id}", idDeAna).header(AUTH, bearer(tokenBeto)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/postulaciones/{id}", idDeAna).header(AUTH, bearer(tokenBeto)).contentType(JSON)
                        .content("{\"puesto\":\"Hackeado\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/postulaciones/{id}/estado", idDeAna).header(AUTH, bearer(tokenBeto)).contentType(JSON)
                        .content("{\"estado\":\"RECHAZADA\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/postulaciones/{id}", idDeAna).header(AUTH, bearer(tokenBeto)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/postulaciones/{id}/analisis", idDeAna).header(AUTH, bearer(tokenBeto)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/postulaciones/{id}/analisis", idDeAna).header(AUTH, bearer(tokenBeto)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/postulaciones").header(AUTH, bearer(tokenBeto)))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/postulaciones/{id}", idDeAna).header(AUTH, bearer(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puesto").value("Backend Jr"))
                .andExpect(jsonPath("$.estado").value("ENVIADA"));
    }

    @Test
    void cadaUsuarioTieneSuPropioPerfil() throws Exception {
        String tokenAna = registrarYLoguear();
        String tokenBeto = registrarYLoguear();
        guardarPerfil(tokenAna, "Perfil de Ana");

        mvc.perform(get("/perfil").header(AUTH, bearer(tokenBeto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").doesNotExist());
        mvc.perform(get("/perfil").header(AUTH, bearer(tokenAna)))
                .andExpect(jsonPath("$.perfil").value("Perfil de Ana"));
    }

    @Test
    void patch_conPuestoEnBlanco_devuelve400() throws Exception {
        String token = registrarYLoguear();
        long id = crearPostulacion(token, null);

        mvc.perform(patch("/postulaciones/{id}", id).header(AUTH, bearer(token)).contentType(JSON)
                        .content("{\"puesto\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.puesto").exists());
    }

    @Test
    void crearPostulacion_sinPuesto_devuelve400() throws Exception {
        String token = registrarYLoguear();

        mvc.perform(post("/postulaciones").header(AUTH, bearer(token)).contentType(JSON)
                        .content("{\"empresa\":\"" + empresaUnica() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.puesto").exists());
    }
}