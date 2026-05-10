package org.aitu.vulnerabilitiesmvp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.aitu.vulnerabilitiesmvp.config.AppProperties;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentExportFormat;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentHistoryQuery;
import org.aitu.vulnerabilitiesmvp.dto.payment.PaymentResponse;
import org.aitu.vulnerabilitiesmvp.security.AppUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class PaymentExportService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExportService.class);
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentService paymentService;
    private final InputNormalizationService inputNormalizationService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public PaymentExportService(
        PaymentService paymentService,
        InputNormalizationService inputNormalizationService,
        ObjectMapper objectMapper,
        AppProperties appProperties
    ) {
        this.paymentService = paymentService;
        this.inputNormalizationService = inputNormalizationService;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    public ExportedPayload exportHistory(
        AppUserPrincipal principal,
        PaymentHistoryQuery query,
        PaymentExportFormat format,
        String requestedFileName
    ) {
        List<PaymentResponse> payments = paymentService.getPaymentHistoryEntries(principal, query);
        String baseName = inputNormalizationService.normalizeExportBaseName(requestedFileName, "payment-history");
        // String fileName = buildFileName(baseName, format);
        String fileName = buildDownloadFileName(baseName, format);
        Path exportDir = resolveExportDirectory();
        //if (!exportPath.startsWith(exportDir)) {
        //            throw new InvalidInputException("Resolved export path is outside the allowed directory");
        //        }
        Path exportPath = createTempExportPath(exportDir, baseName, format);

        try {
            writeExportFile(exportPath, format, payments);
            byte[] bytes = Files.readAllBytes(exportPath);
            log.info("payment export created actor={} fileName={} format={} records={}",
                principal.getUsername(), fileName, format, payments.size());
            return new ExportedPayload(fileName, resolveMediaType(format), bytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate export", ex);
        } finally {
            try {
                Files.deleteIfExists(exportPath);
            } catch (IOException ex) {
                log.warn("Failed to delete temporary export file {}", exportPath.getFileName());
            }
        }
    }

    private Path resolveExportDirectory() {
        try {
            Path directory = Path.of(appProperties.getExports().getBaseDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            return directory;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize export directory", ex);
        }
    }

    private String buildDownloadFileName(String baseName, PaymentExportFormat format) {
        String timestamp = FILE_TIMESTAMP_FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC));
        return baseName + "-" + timestamp + "." + resolveExtension(format);
    }

    private Path createTempExportPath(Path exportDir, String baseName, PaymentExportFormat format) {
        try {
            // Create the server-side export file inside the approved directory only.
            return Files.createTempFile(exportDir, baseName + "-", "." + resolveExtension(format));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to allocate temporary export file", ex);
        }
    }

    private MediaType resolveMediaType(PaymentExportFormat format) {
        return switch (format) {
            case CSV -> new MediaType("text", "csv", StandardCharsets.UTF_8);
            case JSON -> MediaType.APPLICATION_JSON;
            case XML -> MediaType.APPLICATION_XML;
        };
    }

    private String resolveExtension(PaymentExportFormat format) {
        return switch (format) {
            case CSV -> "csv";
            case JSON -> "json";
            case XML -> "xml";
        };
    }

    private void writeExportFile(Path exportPath, PaymentExportFormat format, List<PaymentResponse> payments) throws IOException {
        switch (format) {
            case CSV -> writeCsv(exportPath, payments);
            case JSON -> writeJson(exportPath, payments);
            case XML -> writeXml(exportPath, payments);
        }
    }

    private void writeCsv(Path exportPath, List<PaymentResponse> payments) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8)) {
            writer.write("id,sourceAccountId,receiverAccountId,receiverUsername,amount,currency,status,flagged,description,createdAt,confirmedAt");
            writer.newLine();
            for (PaymentResponse payment : payments) {
                writer.write(String.join(",",
                    csvCell(payment.id()),
                    csvCell(payment.sourceAccountId()),
                    csvCell(payment.receiverAccountId()),
                    csvCell(payment.receiverUsername()),
                    csvCell(payment.amount()),
                    csvCell(payment.currency() == null ? null : payment.currency().name()),
                    csvCell(payment.status() == null ? null : payment.status().name()),
                    csvCell(payment.flagged()),
                    csvCell(payment.description()),
                    csvCell(payment.createdAt()),
                    csvCell(payment.confirmedAt())
                ));
                writer.newLine();
            }
        }
    }

    private void writeJson(Path exportPath, List<PaymentResponse> payments) throws IOException {
        try (var outputStream = Files.newOutputStream(exportPath)) {
            objectMapper.writeValue(outputStream, payments);
        }
    }

    private void writeXml(Path exportPath, List<PaymentResponse> payments) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            writer.write("<payments>");
            writer.newLine();
            for (PaymentResponse payment : payments) {
                writer.write("  <payment>");
                writer.newLine();
                xmlElement(writer, "id", String.valueOf(payment.id()));
                xmlElement(writer, "sourceAccountId", String.valueOf(payment.sourceAccountId()));
                xmlElement(writer, "receiverAccountId", String.valueOf(payment.receiverAccountId()));
                xmlElement(writer, "receiverUsername", payment.receiverUsername());
                xmlElement(writer, "amount", payment.amount() == null ? null : payment.amount().toPlainString());
                xmlElement(writer, "currency", payment.currency() == null ? null : payment.currency().name());
                xmlElement(writer, "status", payment.status() == null ? null : payment.status().name());
                xmlElement(writer, "flagged", String.valueOf(payment.flagged()));
                xmlElement(writer, "description", payment.description());
                xmlElement(writer, "createdAt", payment.createdAt() == null ? null : payment.createdAt().toString());
                xmlElement(writer, "confirmedAt", payment.confirmedAt() == null ? null : payment.confirmedAt().toString());
                writer.write("  </payment>");
                writer.newLine();
            }
            writer.write("</payments>");
            writer.newLine();
        }
    }

    private void xmlElement(BufferedWriter writer, String name, String value) throws IOException {
        writer.write("    <" + name + ">");
        writer.write(escapeXml(value));
        writer.write("</" + name + ">");
        writer.newLine();
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String raw = value instanceof BigDecimal decimal ? decimal.toPlainString() : value.toString();
        String sanitized = escapeCsvFormula(raw).replace("\"", "\"\"");
        return "\"" + sanitized + "\"";
    }

    private String escapeCsvFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char firstChar = value.charAt(0);
        if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@') {
            return "'" + value;
        }
        return value;
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    public record ExportedPayload(String fileName, MediaType mediaType, byte[] content) {
    }
}
