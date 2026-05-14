package devkor.ontime_back.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLogPolicyTest {

    @Test
    void resolveRequestIdReusesExistingRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestLogPolicy.REQUEST_ID_ATTRIBUTE, "existing-id");
        request.addHeader(RequestLogPolicy.REQUEST_ID_HEADER, "header-id");

        String requestId = RequestLogPolicy.resolveRequestId(request);

        assertThat(requestId).isEqualTo("existing-id");
    }

    @Test
    void resolveRequestIdAcceptsSafeHeaderAndStoresItForTheRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestLogPolicy.REQUEST_ID_HEADER, "safe.id-123:abc");

        String requestId = RequestLogPolicy.resolveRequestId(request);

        assertThat(requestId).isEqualTo("safe.id-123:abc");
        assertThat(request.getAttribute(RequestLogPolicy.REQUEST_ID_ATTRIBUTE)).isEqualTo(requestId);
    }

    @Test
    void resolveRequestIdReplacesUnsafeHeaderWithGeneratedUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestLogPolicy.REQUEST_ID_HEADER, "not safe/value");

        String requestId = RequestLogPolicy.resolveRequestId(request);

        assertThat(requestId).isNotEqualTo("not safe/value");
        assertThat(requestId).matches("[0-9a-f-]{36}");
        assertThat(request.getAttribute(RequestLogPolicy.REQUEST_ID_ATTRIBUTE)).isEqualTo(requestId);
    }

    @Test
    void exposeRequestIdWritesResponseHeaderWhenResponseExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        RequestLogPolicy.exposeRequestId(new ServletRequestAttributes(request, response), "request-123");

        assertThat(response.getHeader(RequestLogPolicy.REQUEST_ID_HEADER)).isEqualTo("request-123");
    }

    @Test
    void fieldLoggingPolicyOnlyAllowsKnownNonSensitiveFields() {
        assertThat(RequestLogPolicy.isSafeFieldForLogging("requestId")).isTrue();
        assertThat(RequestLogPolicy.isSafeFieldForLogging("password")).isFalse();
        assertThat(RequestLogPolicy.isSafeFieldForLogging("unknownField")).isFalse();
    }

    @Test
    void sensitiveFieldNamesAreMatchedCaseInsensitively() {
        assertThat(RequestLogPolicy.isSensitiveFieldName("Authorization")).isTrue();
        assertThat(RequestLogPolicy.isSensitiveFieldName("client_secret")).isTrue();
        assertThat(RequestLogPolicy.isSensitiveFieldName("requestId")).isFalse();
    }
}
