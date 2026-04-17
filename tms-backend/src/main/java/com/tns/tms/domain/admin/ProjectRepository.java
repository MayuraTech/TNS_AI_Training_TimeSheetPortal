package com.tns.tms.domain.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByStatus(ProjectStatus status);

    @Query("""
        SELECT p FROM Project p
        JOIN ProjectAssignment pa ON pa.project.id = p.id
        WHERE pa.user.id = :userId AND p.status = 'ACTIVE'
        """)
    List<Project> findActiveProjectsForUser(@Param("userId") Long userId);

    boolean existsByName(String name);

    boolean existsByCode(String code);
}
