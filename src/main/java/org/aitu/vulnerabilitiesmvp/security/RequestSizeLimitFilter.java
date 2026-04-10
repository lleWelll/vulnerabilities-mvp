package org.aitu.vulnerabilitiesmvp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.aitu.vulnerabilitiesmvp.exception.ApiErrorResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final long maxRequestSizeBytes;

    public RequestSizeLimitFilter(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.maxRequestSizeBytes = appProperties.getSecurity().getMaxRequestSizeBytes();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (requiresBodyLimit(request)) {
            long contentLength = request.getContentLengthLong();
            if (contentLength > maxRequestSizeBytes) {
                response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                    Instant.now(),
                    HttpStatus.PAYLOAD_TOO_LARGE.value(),
                    HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                    "Request body is too large",
                    request.getRequestURI(),
                    Map.of()
                ));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresBodyLimit(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
            || HttpMethod.PUT.matches(request.getMethod())
            || HttpMethod.PATCH.matches(request.getMethod());
    }
}
