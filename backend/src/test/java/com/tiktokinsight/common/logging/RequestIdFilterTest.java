package com.tiktokinsight.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void keepsSafeClientRequestId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "request-20260721");
        var response = new MockHttpServletResponse();
        FilterChain chain = (ignoredRequest, ignoredResponse) -> assertThat(
                RequestIdFilter.currentRequestId()
        ).isEqualTo("request-20260721");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("request-20260721");
        assertThat(RequestIdFilter.currentRequestId()).isEqualTo("unavailable");
    }

    @Test
    void replacesUnsafeClientRequestId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "unsafe header value\n");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME))
                .matches("[a-f0-9]{32}")
                .isNotEqualTo("unsafe header value\n");
    }
}
