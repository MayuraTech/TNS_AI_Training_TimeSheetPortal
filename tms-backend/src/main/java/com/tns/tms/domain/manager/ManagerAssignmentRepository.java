package com.tns.tms.domain.manager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerAssignmentRepository extends JpaRepository<ManagerAssignment, Long> {

    @Query("SELECT ma FROM ManagerAssignment ma WHERE ma.employee.id = :employeeId AND ma.effectiveTo IS NULL")
    Optional<ManagerAssignment> findActiveByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT ma FROM ManagerAssignment ma WHERE ma.manager.id = :managerId AND ma.effectiveTo IS NULL")
    List<ManagerAssignment> findActiveByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT ma.employee.id FROM ManagerAssignment ma WHERE ma.manager.id = :managerId AND ma.effectiveTo IS NULL")
    List<Long> findDirectReportIds(@Param("managerId") Long managerId);
}
