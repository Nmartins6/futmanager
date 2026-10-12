package com.futmanager.service;

import com.futmanager.domain.entity.*;
import com.futmanager.domain.enums.EnrollmentStatus;
import com.futmanager.domain.enums.StudentStatus;
import com.futmanager.dto.ImportExportDtos.*;
import com.futmanager.dto.StudentDtos.StudentResponse;
import com.futmanager.exception.BusinessException;
import com.futmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportExportService {

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final StudentService studentService;
    private final UserService userService;

    private static final String[] TEMPLATE_HEADERS = {
            "Nome do Aluno",
            "Data de Nascimento (AAAA-MM-DD)",
            "Data de Ingresso (AAAA-MM-DD)",
            "Nome do Responsavel",
            "Telefone do Responsavel",
            "Email do Responsavel",
            "Parentesco",
            "Nome da Turma",
            "Observacoes"
    };

    /**
     * Gera modelo CSV para importação.
     */
    public byte[] generateCsvTemplate() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(TEMPLATE_HEADERS).build())) {

            // Exemplo de preenchimento
            printer.printRecord(
                    "Lucas Gabriel de Souza",
                    "2016-05-15",
                    "2026-02-01",
                    "Fernanda Souza",
                    "(11) 98765-4321",
                    "fernanda@email.com",
                    "Mãe",
                    "Sub-11 Tarde (Competição)",
                    "Atestado entregue. Lateral direito."
            );
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Erro ao gerar modelo CSV.");
        }
    }

    /**
     * Gera modelo XLSX para importação com formatação e instruções.
     */
    public byte[] generateXlsxTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Modelo de Importacao");

            // Estilo do cabeçalho
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Linha de exemplo
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("Lucas Gabriel de Souza");
            exampleRow.createCell(1).setCellValue("2016-05-15");
            exampleRow.createCell(2).setCellValue("2026-02-01");
            exampleRow.createCell(3).setCellValue("Fernanda Souza");
            exampleRow.createCell(4).setCellValue("(11) 98765-4321");
            exampleRow.createCell(5).setCellValue("fernanda@email.com");
            exampleRow.createCell(6).setCellValue("Mãe");
            exampleRow.createCell(7).setCellValue("Sub-11 Tarde (Competição)");
            exampleRow.createCell(8).setCellValue("Exemplo de preenchimento");

            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Erro ao gerar modelo XLSX.");
        }
    }

    /**
     * Lê arquivo CSV ou XLSX enviado e gera pré-visualização com validação por linha.
     */
    @Transactional(readOnly = true)
    public ImportPreviewResponse previewImport(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BusinessException("Arquivo inválido.");
        }

        List<ImportRowDto> rows;
        if (filename.toLowerCase().endsWith(".csv")) {
            rows = parseCsv(file);
        } else if (filename.toLowerCase().endsWith(".xlsx")) {
            rows = parseXlsx(file);
        } else {
            throw new BusinessException("Formato de arquivo não suportado. Envie um arquivo CSV ou XLSX.");
        }

        int validCount = 0;
        int invalidCount = 0;
        int duplicateWarnings = 0;

        for (ImportRowDto row : rows) {
            if (row.valid()) {
                validCount++;
            } else {
                invalidCount++;
            }
            if (row.possibleDuplicate()) {
                duplicateWarnings++;
            }
        }

        return new ImportPreviewResponse(rows.size(), validCount, invalidCount, duplicateWarnings, rows);
    }

    /**
     * Efetiva a importação transacionalmente após confirmação explícita.
     */
    @Transactional
    public ImportResultResponse confirmImport(ImportConfirmRequest request) {
        if (request.rows() == null || request.rows().isEmpty()) {
            throw new BusinessException("Nenhuma linha fornecida para importação.");
        }

        int studentsImported = 0;
        int guardiansImported = 0;
        int enrollmentsCreated = 0;

        User currentUser = userService.getCurrentUserEntity();
        Tenant tenant = currentUser.getTenant();

        Map<String, SchoolClass> classMap = new HashMap<>();
        schoolClassRepository.findByTenantId(tenant.getId()).forEach(c -> classMap.put(c.getName().toLowerCase().trim(), c));

        for (ImportRowDto row : request.rows()) {
            if (!row.valid()) {
                continue; // Ignora linhas inválidas
            }

            // 1. Criar Aluno
            Student student = Student.builder()
                    .tenant(tenant)
                    .fullName(row.studentName().trim())
                    .birthDate(row.parsedBirthDate())
                    .enrollmentDate(row.parsedEnrollmentDate() != null ? row.parsedEnrollmentDate() : LocalDate.now())
                    .status(StudentStatus.ATIVO)
                    .notes(row.notes())
                    .build();

            student = studentRepository.save(student);
            studentsImported++;

            // 2. Criar ou Vincular Responsável por Telefone Único
            Guardian guardian = null;
            if (row.guardianName() != null && !row.guardianName().isBlank()) {
                String phone = row.guardianPhone() != null ? row.guardianPhone().trim() : "";
                Optional<Guardian> existingGuardian = guardianRepository.findByTenantIdAndPhone(
                        tenant.getId(), phone
                );

                if (existingGuardian.isPresent()) {
                    guardian = existingGuardian.get();
                } else {
                    guardian = Guardian.builder()
                            .tenant(tenant)
                            .fullName(row.guardianName().trim())
                            .phone(phone)
                            .email(row.guardianEmail() != null && !row.guardianEmail().isBlank() ? row.guardianEmail().trim().toLowerCase() : null)
                            .build();
                    guardian = guardianRepository.save(guardian);
                    guardiansImported++;
                }

                StudentGuardian link = StudentGuardian.builder()
                        .student(student)
                        .guardian(guardian)
                        .relationship(row.relationship() != null && !row.relationship().isBlank() ? row.relationship() : "Responsável")
                        .mainContact(true)
                        .emergencyContact(true)
                        .build();

                student.getGuardians().add(link);
                studentRepository.save(student);
            }

            // 3. Vincular à Turma com validação de categoria e idade
            if (row.className() != null && !row.className().isBlank()) {
                SchoolClass sc = classMap.get(row.className().toLowerCase().trim());
                if (sc != null) {
                    try {
                        SchoolClassService.validateStudentAgeForClassCategory(student, sc);
                        ClassEnrollment enrollment = ClassEnrollment.builder()
                                .student(student)
                                .schoolClass(sc)
                                .entryDate(student.getEnrollmentDate())
                                .status(EnrollmentStatus.ATIVO)
                                .build();

                        classEnrollmentRepository.save(enrollment);
                        enrollmentsCreated++;
                    } catch (BusinessException e) {
                        log.warn("Aluno {} não pôde ser matriculado na turma {}: {}", student.getFullName(), sc.getName(), e.getMessage());
                    }
                }
            }
        }

        log.info("Importação em lote concluída com sucesso: {} alunos, {} responsáveis, {} matrículas.",
                studentsImported, guardiansImported, enrollmentsCreated);

        return new ImportResultResponse(
                studentsImported,
                guardiansImported,
                enrollmentsCreated,
                String.format("Importação finalizada: %d alunos e %d matrículas processados com sucesso.",
                        studentsImported, enrollmentsCreated)
        );
    }

    /**
     * Exportação de alunos para CSV com proteção contra CSV/Formula Injection.
     */
    @Transactional(readOnly = true)
    public byte[] exportStudentsCsv(String query, StudentStatus status, UUID classId) {
        UUID tenantId = userService.getCurrentUserEntity().getTenant().getId();
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> s.getTenant() != null && s.getTenant().getId().equals(tenantId))
                .toList();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(
                     "ID", "Nome Completo", "Data Nascimento", "Idade", "Situacao", "Data Ingresso", "Turmas Ativas"
             ).build())) {

            for (Student s : students) {
                if (status != null && s.getStatus() != status) continue;

                String activeClasses = s.getEnrollments().stream()
                        .filter(e -> e.getStatus() == EnrollmentStatus.ATIVO)
                        .map(e -> e.getSchoolClass().getName())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Nenhuma");

                printer.printRecord(
                        s.getId(),
                        sanitizeForFormulaInjection(s.getFullName()),
                        s.getBirthDate(),
                        s.getAge(),
                        s.getStatus(),
                        s.getEnrollmentDate(),
                        sanitizeForFormulaInjection(activeClasses)
                );
            }
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Erro ao exportar alunos para CSV.");
        }
    }

    /**
     * Exportação de alunos para XLSX.
     */
    @Transactional(readOnly = true)
    public byte[] exportStudentsXlsx(String query, StudentStatus status, UUID classId) {
        UUID tenantId = userService.getCurrentUserEntity().getTenant().getId();
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> s.getTenant() != null && s.getTenant().getId().equals(tenantId))
                .toList();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Alunos");

            String[] headers = {"ID", "Nome Completo", "Data Nascimento", "Idade", "Situacao", "Data Ingresso", "Turmas Ativas"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Student s : students) {
                if (status != null && s.getStatus() != status) continue;

                String activeClasses = s.getEnrollments().stream()
                        .filter(e -> e.getStatus() == EnrollmentStatus.ATIVO)
                        .map(e -> e.getSchoolClass().getName())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Nenhuma");

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getId().toString());
                row.createCell(1).setCellValue(sanitizeForFormulaInjection(s.getFullName()));
                row.createCell(2).setCellValue(s.getBirthDate().toString());
                row.createCell(3).setCellValue(s.getAge());
                row.createCell(4).setCellValue(s.getStatus().name());
                row.createCell(5).setCellValue(s.getEnrollmentDate().toString());
                row.createCell(6).setCellValue(sanitizeForFormulaInjection(activeClasses));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Erro ao exportar alunos para XLSX.");
        }
    }

    /**
     * Prevenção contra Fórmula Injection (CWE-1236):
     * Se o valor começar com =, +, -, @, insere aspa simples no início para impedir execução de comandos.
     */
    public String sanitizeForFormulaInjection(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            return "'" + trimmed;
        }
        return trimmed;
    }

    private List<ImportRowDto> parseCsv(MultipartFile file) {
        List<ImportRowDto> result = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

            int line = 2;
            for (CSVRecord record : parser) {
                String studentName = getRecordValue(record, 0, "Nome do Aluno");
                String birthDateStr = getRecordValue(record, 1, "Data de Nascimento (AAAA-MM-DD)");
                String enrollDateStr = getRecordValue(record, 2, "Data de Ingresso (AAAA-MM-DD)");
                String guardianName = getRecordValue(record, 3, "Nome do Responsavel");
                String guardianPhone = getRecordValue(record, 4, "Telefone do Responsavel");
                String guardianEmail = getRecordValue(record, 5, "Email do Responsavel");
                String relationship = getRecordValue(record, 6, "Parentesco");
                String className = getRecordValue(record, 7, "Nome da Turma");
                String notes = getRecordValue(record, 8, "Observacoes");

                result.add(validateRow(line++, studentName, birthDateStr, enrollDateStr, guardianName, guardianPhone, guardianEmail, relationship, className, notes));
            }
        } catch (Exception e) {
            throw new BusinessException("Falha na leitura do arquivo CSV: " + e.getMessage());
        }
        return result;
    }

    private List<ImportRowDto> parseXlsx(MultipartFile file) {
        List<ImportRowDto> result = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum();

            DataFormatter formatter = new DataFormatter();

            for (int r = 1; r <= totalRows; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String studentName = formatter.formatCellValue(row.getCell(0));
                if (studentName == null || studentName.isBlank()) continue;

                String birthDateStr = formatter.formatCellValue(row.getCell(1));
                String enrollDateStr = formatter.formatCellValue(row.getCell(2));
                String guardianName = formatter.formatCellValue(row.getCell(3));
                String guardianPhone = formatter.formatCellValue(row.getCell(4));
                String guardianEmail = formatter.formatCellValue(row.getCell(5));
                String relationship = formatter.formatCellValue(row.getCell(6));
                String className = formatter.formatCellValue(row.getCell(7));
                String notes = formatter.formatCellValue(row.getCell(8));

                result.add(validateRow(r + 1, studentName, birthDateStr, enrollDateStr, guardianName, guardianPhone, guardianEmail, relationship, className, notes));
            }
        } catch (Exception e) {
            throw new BusinessException("Falha na leitura do arquivo XLSX: " + e.getMessage());
        }
        return result;
    }

    private ImportRowDto validateRow(
            int lineNumber,
            String studentName,
            String birthDateStr,
            String enrollDateStr,
            String guardianName,
            String guardianPhone,
            String guardianEmail,
            String relationship,
            String className,
            String notes) {

        List<String> errors = new ArrayList<>();

        if (studentName == null || studentName.isBlank()) {
            errors.add("Nome do aluno é obrigatório.");
        }

        LocalDate parsedBirthDate = parseDate(birthDateStr);
        if (parsedBirthDate == null) {
            errors.add("Data de nascimento inválida ou ausente (use AAAA-MM-DD ou DD/MM/AAAA).");
        } else if (parsedBirthDate.isAfter(LocalDate.now())) {
            errors.add("Data de nascimento não pode estar no futuro.");
        }

        LocalDate parsedEnrollDate = parseDate(enrollDateStr);
        if (parsedEnrollDate == null && enrollDateStr != null && !enrollDateStr.isBlank()) {
            errors.add("Formato de data de ingresso inválido.");
        }

        if (guardianName == null || guardianName.isBlank()) {
            errors.add("Nome do responsável é obrigatório.");
        }
        if (guardianPhone == null || guardianPhone.isBlank()) {
            errors.add("Telefone do responsável é obrigatório.");
        }

        // Validação da categoria da turma vs idade do aluno
        if (className != null && !className.isBlank() && parsedBirthDate != null) {
            try {
                UUID tenantId = userService.getCurrentUserEntity().getTenant().getId();
                List<SchoolClass> tenantClasses = schoolClassRepository.findByTenantId(tenantId);
                Optional<SchoolClass> matched = tenantClasses.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(className.trim()))
                        .findFirst();
                if (matched.isPresent()) {
                    Student dummyStudent = Student.builder()
                            .fullName(studentName != null ? studentName.trim() : "Aluno")
                            .birthDate(parsedBirthDate)
                            .build();
                    SchoolClassService.validateStudentAgeForClassCategory(dummyStudent, matched.get());
                }
            } catch (BusinessException be) {
                errors.add(be.getMessage());
            } catch (Exception ignored) {
            }
        }

        // Verificação de homônimo / duplicidade existente no banco
        boolean possibleDuplicate = false;
        String duplicateReason = null;

        if (studentName != null && !studentName.isBlank()) {
            List<Student> homonyms = studentRepository.findByFullNameIgnoreCase(studentName.trim());
            if (!homonyms.isEmpty()) {
                possibleDuplicate = true;
                duplicateReason = String.format("Aviso: Já existe(m) %d aluno(s) cadastrado(s) com este mesmo nome.", homonyms.size());
            }
        }

        return new ImportRowDto(
                lineNumber,
                studentName,
                birthDateStr,
                parsedBirthDate,
                enrollDateStr,
                parsedEnrollDate,
                guardianName,
                guardianPhone,
                guardianEmail,
                relationship,
                className,
                notes,
                errors.isEmpty(),
                errors,
                possibleDuplicate,
                duplicateReason
        );
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        text = text.trim();

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );

        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(text, dtf);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String getRecordValue(CSVRecord record, int index, String header) {
        if (record.isMapped(header)) {
            return record.get(header);
        }
        if (index < record.size()) {
            return record.get(index);
        }
        return null;
    }
}
