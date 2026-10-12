package com.futmanager.repository;

import com.futmanager.domain.entity.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, UUID> {

    List<StudentGuardian> findByStudentId(UUID studentId);

    List<StudentGuardian> findByGuardianId(UUID guardianId);

    Optional<StudentGuardian> findByStudentIdAndGuardianId(UUID studentId, UUID guardianId);

    boolean existsByStudentIdAndGuardianId(UUID studentId, UUID guardianId);
}
