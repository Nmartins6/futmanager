package com.futmanager.util;

import com.futmanager.exception.BusinessException;
import java.util.regex.Pattern;

public class ValidationUtils {

    // Regex rigorosa para validação real de e-mails corporativos e pessoais
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    private ValidationUtils() {}

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static void validateEmail(String email, String fieldName) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(fieldName + " é obrigatório.");
        }
        if (!isValidEmail(email)) {
            throw new BusinessException(fieldName + " possui um formato de e-mail inválido ('" + email + "'). Digite um e-mail válido (exemplo: usuario@dominio.com).");
        }
    }

    public static void validateOptionalEmail(String email, String fieldName) {
        if (email != null && !email.isBlank() && !isValidEmail(email)) {
            throw new BusinessException(fieldName + " possui um formato de e-mail inválido ('" + email + "'). Digite um e-mail válido (exemplo: usuario@dominio.com).");
        }
    }
}
