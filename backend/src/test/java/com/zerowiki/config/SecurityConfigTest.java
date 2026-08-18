package com.zerowiki.config;

import com.zerowiki.common.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    @Test
    void security_errors_use_api_envelope_and_request_id() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestIdFilter.REQUEST_ID, "req_test_security");

        MockHttpServletResponse unauthorized = new MockHttpServletResponse();
        SecurityConfig.writeSecurityError(objectMapper, request, unauthorized,
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "authentication is required");

        assertThat(unauthorized.getStatus()).isEqualTo(401);
        assertThat(unauthorized.getContentType()).startsWith("application/json");
        assertThat(unauthorized.getContentAsString()).contains(
                "\"code\":\"UNAUTHORIZED\"", "\"requestId\":\"req_test_security\"");

        MockHttpServletResponse forbidden = new MockHttpServletResponse();
        SecurityConfig.writeSecurityError(objectMapper, request, forbidden,
                HttpStatus.FORBIDDEN, "FORBIDDEN", "access is denied");

        assertThat(forbidden.getStatus()).isEqualTo(403);
        assertThat(forbidden.getContentAsString()).contains(
                "\"code\":\"FORBIDDEN\"", "\"requestId\":\"req_test_security\"");
    }
}
