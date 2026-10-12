package com.futmanager.service;

import com.futmanager.domain.entity.SchoolClass;
import com.futmanager.domain.entity.Student;
import com.futmanager.domain.enums.AttendanceStatus;
import com.futmanager.domain.enums.Role;
import com.futmanager.dto.SchoolConfigDto.SchoolConfigResponse;
import com.futmanager.dto.StatisticsDtos.*;
import com.futmanager.exception.BusinessException;
import com.futmanager.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final StatisticsService statisticsService;
    private final SchoolConfigService schoolConfigService;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Relatório de Frequência por Turma em PDF.
     */
    @Transactional(readOnly = true)
    public byte[] generateClassFrequencyPdf(UUID classId, LocalDate startDate, LocalDate endDate) {
        SchoolClass sc = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Turma não encontrada: " + classId));

        List<StudentFrequencySummaryDto> studentStats = statisticsService.calculateAllStudentsFrequency(classId, startDate, endDate, null);
        SchoolConfigResponse config = schoolConfigService.getConfigs();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addReportHeader(doc, config, "Relatório de Frequência por Turma",
                    String.format("Turma: %s (%s) | Período: %s a %s",
                            sc.getName(), sc.getCategory(), formatOptDate(startDate), formatOptDate(endDate)));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.5f, 1.2f, 1.2f, 1.2f, 1.2f, 1.5f, 1.5f});
            table.setSpacingBefore(15);

            addTableHeader(table, "Aluno", "Idade", "Aulas", "Pres.", "Faltas", "F. Just.", "Freq. (%)");

            for (StudentFrequencySummaryDto s : studentStats) {
                String freqStr = s.attendancePercentage() != null ? String.format("%.1f%%", s.attendancePercentage()) : "Sem dados";
                Color rowBg = s.belowThreshold() && s.totalLessons() > 0 ? new Color(254, 226, 226) : Color.WHITE;

                addTableCell(table, s.studentName(), Element.ALIGN_LEFT, rowBg);
                addTableCell(table, String.valueOf(s.currentAge()), Element.ALIGN_CENTER, rowBg);
                addTableCell(table, String.valueOf(s.totalLessons()), Element.ALIGN_CENTER, rowBg);
                addTableCell(table, String.valueOf(s.presences()), Element.ALIGN_CENTER, rowBg);
                addTableCell(table, String.valueOf(s.unexcusedAbsences()), Element.ALIGN_CENTER, rowBg);
                addTableCell(table, String.valueOf(s.excusedAbsences()), Element.ALIGN_CENTER, rowBg);
                addTableCell(table, freqStr, Element.ALIGN_CENTER, rowBg);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar PDF de turma: ", e);
            throw new BusinessException("Erro na geração do relatório PDF.");
        }
    }

    /**
     * Relatório Individual de Aluno em PDF.
     */
    @Transactional(readOnly = true)
    public byte[] generateStudentIndividualPdf(UUID studentId, UUID classId, LocalDate startDate, LocalDate endDate) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("Aluno não encontrado: " + studentId));

        StudentFrequencySummaryDto summary = statisticsService.getStudentFrequency(studentId, classId, startDate, endDate);
        List<StudentAttendanceHistoryItemDto> history = statisticsService.getStudentAttendanceHistory(studentId, classId, startDate, endDate);
        SchoolConfigResponse config = schoolConfigService.getConfigs();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addReportHeader(doc, config, "Ficha Individual de Frequência",
                    String.format("Aluno: %s | Idade Atual: %d anos | Ingresso: %s",
                            student.getFullName(), student.getAge(), student.getEnrollmentDate().format(DATE_FMT)));

            // Resumo
            PdfPTable summaryTable = new PdfPTable(5);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(10);
            summaryTable.setSpacingAfter(15);
            addTableHeader(summaryTable, "Aulas Computadas", "Presenças", "Faltas", "Faltas Justificadas", "Frequência Total");

            String pct = summary.attendancePercentage() != null ? String.format("%.1f%%", summary.attendancePercentage()) : "Sem dados";
            addTableCell(summaryTable, String.valueOf(summary.totalLessons()), Element.ALIGN_CENTER, Color.WHITE);
            addTableCell(summaryTable, String.valueOf(summary.presences()), Element.ALIGN_CENTER, Color.WHITE);
            addTableCell(summaryTable, String.valueOf(summary.unexcusedAbsences()), Element.ALIGN_CENTER, Color.WHITE);
            addTableCell(summaryTable, String.valueOf(summary.excusedAbsences()), Element.ALIGN_CENTER, Color.WHITE);
            addTableCell(summaryTable, pct, Element.ALIGN_CENTER, summary.belowThreshold() ? new Color(254, 226, 226) : Color.WHITE);
            doc.add(summaryTable);

            // Tabela com histórico aula a aula
            Paragraph p = new Paragraph("Histórico Aula a Aula:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            p.setSpacingAfter(6);
            doc.add(p);

            PdfPTable historyTable = new PdfPTable(5);
            historyTable.setWidthPercentage(100);
            historyTable.setWidths(new float[]{2f, 3f, 1.5f, 2f, 3.5f});
            addTableHeader(historyTable, "Data", "Turma", "Idade na Aula", "Situação", "Justificativa / Obs.");

            for (StudentAttendanceHistoryItemDto h : history) {
                String sitLabel = switch (h.status()) {
                    case "PRESENTE" -> "Presente";
                    case "AUSENTE" -> "Ausente";
                    case "FALTA_JUSTIFICADA" -> "Falta Justificada";
                    default -> h.status();
                };

                addTableCell(historyTable, h.lessonDate().format(DATE_FMT), Element.ALIGN_CENTER, Color.WHITE);
                addTableCell(historyTable, h.className(), Element.ALIGN_LEFT, Color.WHITE);
                addTableCell(historyTable, h.ageOnLessonDate() + " anos", Element.ALIGN_CENTER, Color.WHITE);
                addTableCell(historyTable, sitLabel, Element.ALIGN_CENTER, Color.WHITE);
                addTableCell(historyTable, h.notes() != null ? h.notes() : "-", Element.ALIGN_LEFT, Color.WHITE);
            }

            doc.add(historyTable);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar PDF individual: ", e);
            throw new BusinessException("Erro na geração do relatório individual.");
        }
    }

    /**
     * Relatório de Alunos com Baixa Frequência em PDF.
     */
    @Transactional(readOnly = true)
    public byte[] generateLowFrequencyPdf(UUID classId, LocalDate startDate, LocalDate endDate) {
        List<StudentFrequencySummaryDto> allStats = statisticsService.calculateAllStudentsFrequency(classId, startDate, endDate, null);
        List<StudentFrequencySummaryDto> lowFreq = allStats.stream()
                .filter(s -> s.belowThreshold() && s.totalLessons() > 0)
                .toList();

        SchoolConfigResponse config = schoolConfigService.getConfigs();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addReportHeader(doc, config, "Alunos com Baixa Frequência",
                    String.format("Limite de Alerta: < %.1f%% | Total em Alerta: %d",
                            config.lowFrequencyThreshold(), lowFreq.size()));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.5f, 1.2f, 1.5f, 1.5f, 1.5f, 1.8f});
            table.setSpacingBefore(15);
            addTableHeader(table, "Aluno", "Idade", "Aulas", "Presenças", "Faltas", "Frequência");

            for (StudentFrequencySummaryDto s : lowFreq) {
                String freq = String.format("%.1f%%", s.attendancePercentage());
                Color redBg = new Color(254, 226, 226);

                addTableCell(table, s.studentName(), Element.ALIGN_LEFT, redBg);
                addTableCell(table, String.valueOf(s.currentAge()), Element.ALIGN_CENTER, redBg);
                addTableCell(table, String.valueOf(s.totalLessons()), Element.ALIGN_CENTER, redBg);
                addTableCell(table, String.valueOf(s.presences()), Element.ALIGN_CENTER, redBg);
                addTableCell(table, String.valueOf(s.unexcusedAbsences()), Element.ALIGN_CENTER, redBg);
                addTableCell(table, freq, Element.ALIGN_CENTER, redBg);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar PDF baixa frequência: ", e);
            throw new BusinessException("Erro na geração do relatório PDF de baixa frequência.");
        }
    }

    /**
     * Relatório de Resumo Mensal em XLSX.
     */
    @Transactional(readOnly = true)
    public byte[] generateMonthlySummaryXlsx(UUID classId, Integer year) {
        List<MonthlyEvolutionDto> list = statisticsService.getMonthlyEvolution(classId, year);
        SchoolConfigResponse config = schoolConfigService.getConfigs();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Resumo Mensal");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Mês/Ano", "Aulas Realizadas", "Presenças", "Faltas", "Faltas Justificadas", "Frequência (%)"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (MonthlyEvolutionDto m : list) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(m.monthYear());
                r.createCell(1).setCellValue(m.totalLessons());
                r.createCell(2).setCellValue(m.presences());
                r.createCell(3).setCellValue(m.absences());
                r.createCell(4).setCellValue(m.excusedAbsences());
                r.createCell(5).setCellValue(m.attendancePercentage() != null ? m.attendancePercentage() : 0.0);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Erro ao gerar relatório XLSX mensal.");
        }
    }

    private void addReportHeader(Document doc, SchoolConfigResponse config, String title, String subtitle) throws DocumentException {
        Paragraph schoolName = new Paragraph(config.schoolName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(15, 23, 42)));
        schoolName.setAlignment(Element.ALIGN_CENTER);
        doc.add(schoolName);

        Paragraph info = new Paragraph(String.format("CNPJ: %s | %s | %s", config.cnpj(), config.phone(), config.address()),
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
        info.setAlignment(Element.ALIGN_CENTER);
        doc.add(info);

        Paragraph line = new Paragraph("_______________________________________________________________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 8, Color.LIGHT_GRAY));
        line.setAlignment(Element.ALIGN_CENTER);
        line.setSpacingAfter(10);
        doc.add(line);

        Paragraph reportTitle = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 41, 59)));
        reportTitle.setAlignment(Element.ALIGN_CENTER);
        doc.add(reportTitle);

        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph sub = new Paragraph(subtitle, FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY));
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(8);
            doc.add(sub);
        }

        Paragraph generatedAt = new Paragraph("Emitido em: " + LocalDate.now().format(DATE_FMT) + " (Fuso: " + config.timezone() + ")",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY));
        generatedAt.setAlignment(Element.ALIGN_RIGHT);
        generatedAt.setSpacingAfter(8);
        doc.add(generatedAt);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Color headerColor = new Color(30, 58, 138); // Azul escuro esportivo

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text, int align, Color bg) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(bg != null ? bg : Color.WHITE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String formatOptDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "Início/Fim";
    }
}
