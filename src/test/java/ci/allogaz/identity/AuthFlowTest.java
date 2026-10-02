package ci.allogaz.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import ci.allogaz.AbstractIntegrationTest;
import ci.allogaz.support.RecordingSmsSender;

class AuthFlowTest extends AbstractIntegrationTest {

    @Autowired
    RecordingSmsSender sms;

    @Test
    void signup_login_refresh_rotation_and_reuse_detection() throws Exception {
        String phone = uniquePhone();
        requestOtp(phone).andExpect(status().isAccepted()).andExpect(jsonPath("$.expiresInSeconds").value(300));

        // Second envoi immédiat : délai anti-abus
        requestOtp(phone).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));

        String code = sms.lastCode(phone);
        String wrong = code.equals("000000") ? "111111" : "000000";
        verify(phone, wrong).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("OTP_INVALID"));

        String body = verify(phone, code).andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.user.phone").value(phone))
                .andExpect(jsonPath("$.user.roles[0]").value("BUYER"))
                .andReturn().getResponse().getContentAsString();
        String access = JsonPath.read(body, "$.accessToken");
        String refresh = JsonPath.read(body, "$.refreshToken");

        // Le code est à usage unique
        verify(phone, code).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("OTP_EXPIRED"));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value(phone));

        String refreshed = refresh(refresh).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newRefresh = JsonPath.read(refreshed, "$.refreshToken");
        assertThat(newRefresh).isNotEqualTo(refresh);

        // Réutilisation de l'ancien jeton : toute la famille est révoquée
        refresh(refresh).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
        refresh(newRefresh).andExpect(status().isUnauthorized());
    }

    @Test
    void too_many_wrong_codes_burn_the_otp() throws Exception {
        String phone = uniquePhone();
        requestOtp(phone).andExpect(status().isAccepted());
        String code = sms.lastCode(phone);
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 4; i++) {
            verify(phone, wrong).andExpect(status().isUnauthorized());
        }
        verify(phone, wrong).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_TOO_MANY_ATTEMPTS"));
        verify(phone, code).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("OTP_EXPIRED"));
    }

    @Test
    void invalid_phone_is_rejected() throws Exception {
        requestOtp("+33612345678").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE"));
    }

    private org.springframework.test.web.servlet.ResultActions requestOtp(String phone) throws Exception {
        return mvc.perform(post("/api/v1/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions verify(String phone, String code) throws Exception {
        return mvc.perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String token) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }
}
