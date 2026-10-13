package com.futmanager.service;

import com.futmanager.domain.entity.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StudentAndSecurityRulesTest {

    private final ImportExportService importExportService = new ImportExportService(null, null, null, null, null, null);

    @Test
    @DisplayName("Idade do aluno deve ser calculada dinamicamente e nunca estática")
    void testDynamicAgeCalculation() {
        LocalDate birthDate = LocalDate.now().minusYears(10).minusMonths(2);
        Student student = Student.builder()
                .fullName("Enzo Teste")
                .birthDate(birthDate)
                .build();

        assertEquals(10, student.getAge());

        // Idade na data de uma aula há 2 anos
        LocalDate pastDate = LocalDate.now().minusYears(2);
        assertEquals(8, student.getAgeAt(pastDate));
    }

    @Test
    @DisplayName("Deve sanitizar strings com prefixos de injeção de fórmulas CSV (=, +, -, @)")
    void testFormulaInjectionSanitization() {
        assertEquals("Normal Text", importExportService.sanitizeForFormulaInjection("Normal Text"));
        assertEquals("'=cmd|' /C calc'!A0", importExportService.sanitizeForFormulaInjection("=cmd|' /C calc'!A0"));
        assertEquals("'+12345", importExportService.sanitizeForFormulaInjection("+12345"));
        assertEquals("'-SUM(A1:A10)", importExportService.sanitizeForFormulaInjection("-SUM(A1:A10)"));
        assertEquals("'@SUM(A1:A10)", importExportService.sanitizeForFormulaInjection("@SUM(A1:A10)"));
    }

    @Test
    @DisplayName("Deve bloquear tentativa de Path Traversal no carregamento de recursos")
    void testPathTraversalPrevention() {
        StorageService storageService = new StorageService("./uploads", "local");
        com.futmanager.exception.BusinessException ex = assertThrows(
                com.futmanager.exception.BusinessException.class,
                () -> storageService.loadAsResource("../../etc/passwd")
        );
        assertTrue(ex.getMessage().contains("Acesso negado") || ex.getMessage().contains("não encontrado"));
    }

    @Test
    @DisplayName("Validação estrita de e-mail deve aceitar formatos válidos e rejeitar inválidos")
    void testEmailValidation() {
        assertTrue(com.futmanager.util.ValidationUtils.isValidEmail("admin@futmanager.com"));
        assertTrue(com.futmanager.util.ValidationUtils.isValidEmail("user.name+tag@sub.domain.co"));
        assertFalse(com.futmanager.util.ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(com.futmanager.util.ValidationUtils.isValidEmail("@missinguser.com"));
        assertFalse(com.futmanager.util.ValidationUtils.isValidEmail("user@"));
        assertFalse(com.futmanager.util.ValidationUtils.isValidEmail(null));
    }
}
