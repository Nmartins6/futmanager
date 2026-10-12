package com.futmanager.repository;

import com.futmanager.domain.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, UUID> {

    List<ClassSchedule> findBySchoolClassId(UUID classId);

    void deleteBySchoolClassId(UUID classId);
}
