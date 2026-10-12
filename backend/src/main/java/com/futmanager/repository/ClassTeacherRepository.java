package com.futmanager.repository;

import com.futmanager.domain.entity.ClassTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassTeacherRepository extends JpaRepository<ClassTeacher, UUID> {

    List<ClassTeacher> findBySchoolClassId(UUID classId);

    List<ClassTeacher> findByUserId(UUID userId);

    boolean existsBySchoolClassIdAndUserId(UUID classId, UUID userId);

    Optional<ClassTeacher> findBySchoolClassIdAndUserId(UUID classId, UUID userId);
}
