package ci.allogaz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ApplicationSmokeTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void health_is_up() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void openapi_documentation_is_published() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AlloGaz API"));
    }

    @Test
    void protected_endpoints_require_authentication() throws Exception {
        mvc.perform(get("/api/v1/anything")).andExpect(status().isUnauthorized());
    }
}
