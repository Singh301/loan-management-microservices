package com.loanmanagement.common.logging;

import com.loanmanagement.common.constants.ApiConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPropagateExistingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(ApiConstants.HEADER_CORRELATION_ID, "req-123");

        FilterChain chain = (req, res) -> {
            assertEquals("req-123", MDC.get("requestId"));
        };

        filter.doFilter(request, response, chain);

        assertEquals("req-123", response.getHeader(ApiConstants.HEADER_CORRELATION_ID));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldGenerateRequestIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] idFromChain = new String[1];
        FilterChain chain = (req, res) -> idFromChain[0] = MDC.get("requestId");

        filter.doFilter(request, response, chain);

        String responseId = response.getHeader(ApiConstants.HEADER_CORRELATION_ID);
        assertNotNull(responseId);
        assertTrue(!responseId.isBlank());
        assertEquals(responseId, idFromChain[0]);
        assertNull(MDC.get("requestId"));
    }
}
