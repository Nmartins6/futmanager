package com.futmanager.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RepositoryQueryValidationTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceItemRepository attendanceItemRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Test
    @DisplayName("Deve inicializar o contexto JPA e validar a sintaxe de todas as consultas JPQL customizadas")
    void validateAllCustomQueriesSyntax() {
        assertNotNull(studentRepository);
        assertNotNull(attendanceItemRepository);
        assertNotNull(lessonRepository);
        assertNotNull(schoolClassRepository);

        // Executar chamadas reais aos métodos @Query para assegurar validação completa do parser HQL
        UUID fakeId = UUID.randomUUID();
        LocalDate now = LocalDate.now();

        studentRepository.searchStudents(fakeId, "teste", null, null, LocalDate.of(1900, 1, 1), LocalDate.of(2099, 12, 31), PageRequest.of(0, 10));
        attendanceItemRepository.findFinalizedItems(fakeId, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        attendanceItemRepository.findFinalizedItemsForStudent(fakeId, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        lessonRepository.findPendingAttendanceLessons(fakeId, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        lessonRepository.searchLessons(fakeId, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, PageRequest.of(0, 10));
    }
}
