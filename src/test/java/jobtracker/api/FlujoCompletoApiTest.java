package jobtracker.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FlujoCompletoApiTest extends ApiTestBase {

    @Test
    void recorridoCompleto_desdeElRegistroHastaBorrarLaPostulacion() throws Exception {
        String email = emailUnico();
        String empresa = empresaUnica();

        mvc.perform(post("/auth/register").contentType(JSON).content("""
                        {"nombre":"Ana","apellido":"Perez","email":"%s","password":"password123"}
                        """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String token = loguear(email, "password123");

        guardarPerfil(token, "Java, Spring Boot y MySQL");
        mvc.perform(get("/perfil").header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("Java, Spring Boot y MySQL"));

        MvcResult creada = mvc.perform(post("/postulaciones").header(AUTH, bearer(token)).contentType(JSON)
                        .content("""
                                {"empresa":"%s","puesto":"Backend Jr","seniority":"JUNIOR",
                                 "descripcionOferta":"Buscamos dev Java con Spring Boot y MySQL"}
                                """.formatted(empresa)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andExpect(jsonPath("$.empresa").value(empresa))
                .andReturn();
        long id = ((Number) JsonPath.read(cuerpo(creada), "$.id")).longValue();

        when(llmClient.generarJson(any(), any())).thenReturn(RESPUESTA_IA);
        mvc.perform(post("/postulaciones/{id}/analisis", id).header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeCoincidencia").value(72))
                .andExpect(jsonPath("$.habilidadesFaltantes[0]").value("MySQL"));
        mvc.perform(get("/postulaciones/{id}/analisis", id).header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeCoincidencia").value(72))
                .andExpect(jsonPath("$.fecha").exists());

        mvc.perform(get("/postulaciones").header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].empresa").value(empresa));

        mvc.perform(patch("/postulaciones/{id}", id).header(AUTH, bearer(token)).contentType(JSON)
                        .content("{\"nota\":\"Llame a RRHH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value("Llame a RRHH"))
                .andExpect(jsonPath("$.puesto").value("Backend Jr"))
                .andExpect(jsonPath("$.seniority").value("JUNIOR"));

        mvc.perform(patch("/postulaciones/{id}", id).header(AUTH, bearer(token)).contentType(JSON)
                        .content("{\"nota\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").doesNotExist());

        mvc.perform(patch("/postulaciones/{id}/estado", id).header(AUTH, bearer(token)).contentType(JSON)
                        .content("{\"estado\":\"ENTREVISTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREVISTA"));
        mvc.perform(get("/postulaciones").param("estado", "ENTREVISTA").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/postulaciones").param("estado", "ENVIADA").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(delete("/postulaciones/{id}", id).header(AUTH, bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/postulaciones/{id}", id).header(AUTH, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfil_conTextoVacio_loBorra() throws Exception {
        String token = registrarYLoguear();
        guardarPerfil(token, "Java y Spring Boot");

        mvc.perform(put("/perfil").header(AUTH, bearer(token)).contentType(JSON).content("{\"perfil\":\"\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/perfil").header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").doesNotExist());
    }
}