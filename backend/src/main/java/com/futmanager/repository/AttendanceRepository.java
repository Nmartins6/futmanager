package com.futmanager.repository;

import com.futmanager.domain.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByLessonId(UUID lessonId);

    boolean existsByLessonId(UUID lessonId);
}
