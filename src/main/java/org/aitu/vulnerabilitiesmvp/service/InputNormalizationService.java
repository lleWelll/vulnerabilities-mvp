package org.aitu.vulnerabilitiesmvp.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.aitu.vulnerabilitiesmvp.exception.InvalidInputException;
import org.springframework.stereotype.Service;

@Service
public class InputNormalizationService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,50}$");

    /*
     OWASP-10: Mishandling of Exceptional Conditions - имена "."/".." или без букв/цифр проходили allowlist
     и могли ломать export filename handling. Исправление: basename обязан содержать букву или цифру.

        private static final Pattern FILE_BASENAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
     */
    private static final Pattern FILE_BASENAME_PATTERN = Pattern.compile("^(?=.*[A-Za-z0-9])[A-Za-z0-9._-]{1,64}$");

    public String normalizeUsername(String value, String fieldName) {
        String normalized = normalizeUnicode(value);
        if (normalized == null || normalized.isBlank()) {
            throw new InvalidInputException(fieldName + " must not be blank");
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new InvalidInputException(fieldName + " contains unsupported characters");
        }
        return normalized;
    }

    public String normalizeUsernameFilter(String value, String fieldName) {
        String normalized = normalizeUnicode(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new InvalidInputException(fieldName + " contains unsupported characters");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public String normalizeFreeText(String value, int maxLength, String fieldName) {
        String normalized = normalizeUnicode(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        normalized = normalized.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
        normalized = normalized.replaceAll("[\\r\\n\\t]+", " ");
        normalized = normalized.replaceAll(" {2,}", " ").trim();
        if (normalized.length() > maxLength) {
            throw new InvalidInputException(fieldName + " exceeds maximum length");
        }
        return normalized;
    }

    public String normalizeExportBaseName(String value, String defaultName) {
        String normalized = normalizeUnicode(value);
        if (normalized == null || normalized.isBlank()) {
            return defaultName;
        }
        if (!FILE_BASENAME_PATTERN.matcher(normalized).matches()) {
            throw new InvalidInputException("fileName contains unsupported characters");
        }
        return normalized;
    }

    private String normalizeUnicode(String value) {
        if (value == null) {
            return null;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
    }
}
