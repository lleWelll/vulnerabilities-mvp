package org.aitu.vulnerabilitiesmvp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.aitu.vulnerabilitiesmvp.exception.ApiErrorResponse;
import org.aitu.vulnerabilitiesmvp.exception.RequestBodyTooLargeException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final long maxRequestSizeBytes;
    private final long maxFileSizeBytes;

    public RequestSizeLimitFilter(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.maxRequestSizeBytes = appProperties.getSecurity().getMaxRequestSizeBytes();
        this.maxFileSizeBytes = appProperties.getSecurity().getMaxFileSizeBytes();
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!requiresBodyLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (isMultipartRequest(request)) {
            if (contentLength > maxFileSizeBytes) {
                writeError(response, request, HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large");
            } else {
                writeError(response, request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File uploads are not supported by this API");
            }
            return;
        }

        if (contentLength > maxRequestSizeBytes) {
            writeError(response, request, HttpStatus.PAYLOAD_TOO_LARGE, "Request body is too large");
            return;
        }

        HttpServletRequest boundedRequest = request;
        if (shouldWrapBody(request)) {
            boundedRequest = new RequestBodyLimitingWrapper(request, maxRequestSizeBytes);
        }

        try {
            filterChain.doFilter(boundedRequest, response);
        } catch (RequestBodyTooLargeException ex) {
            if (!response.isCommitted()) {
                writeError(response, request, HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
                return;
            }
            throw ex;
        }
    }

    private boolean requiresBodyLimit(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
            || HttpMethod.PUT.matches(request.getMethod())
            || HttpMethod.PATCH.matches(request.getMethod());
    }

    private boolean shouldWrapBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)
                || mediaType.getSubtype().toLowerCase(Locale.ROOT).endsWith("+json");
        } catch (InvalidMediaTypeException ex) {
            return true;
        }
    }

    private boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        try {
            return MediaType.MULTIPART_FORM_DATA.isCompatibleWith(MediaType.parseMediaType(contentType));
        } catch (InvalidMediaTypeException ex) {
            return contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
        }
    }

    private void writeError(
        HttpServletResponse response,
        HttpServletRequest request,
        HttpStatus status,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            Map.of()
        ));
    }

    private static final class RequestBodyLimitingWrapper extends HttpServletRequestWrapper {

        private final long maxRequestSizeBytes;
        private ServletInputStream inputStream;
        private BufferedReader reader;

        private RequestBodyLimitingWrapper(HttpServletRequest request, long maxRequestSizeBytes) {
            super(request);
            this.maxRequestSizeBytes = maxRequestSizeBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (reader != null) {
                throw new IllegalStateException("getReader() has already been called for this request");
            }
            if (inputStream == null) {
                inputStream = new LimitedServletInputStream(super.getInputStream(), maxRequestSizeBytes);
            }
            return inputStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if (reader == null) {
                Charset charset = resolveCharset(getCharacterEncoding());
                reader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
            }
            return reader;
        }

        private static Charset resolveCharset(String encoding) {
            if (encoding == null || encoding.isBlank()) {
                return StandardCharsets.UTF_8;
            }
            try {
                return Charset.forName(encoding);
            } catch (Exception ex) {
                return StandardCharsets.UTF_8;
            }
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxBytes;
        private long bytesRead;

        private LimitedServletInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int nextByte = delegate.read();
            if (nextByte != -1) {
                incrementBytesRead(1);
            }
            return nextByte;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int count = delegate.read(b, off, len);
            if (count > 0) {
                incrementBytesRead(count);
            }
            return count;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        private void incrementBytesRead(int delta) {
            bytesRead += delta;
            if (bytesRead > maxBytes) {
                throw new RequestBodyTooLargeException("Request body is too large");
            }
        }
    }
}
