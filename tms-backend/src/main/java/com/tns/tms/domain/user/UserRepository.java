package com.tns.tms.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);

    @Query("""
        SELECT u FROM User u
        WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR u.status = :status)
        AND (:department IS NULL OR u.department = :department)
        """)
    Page<User> findWithFilters(
        @Param("search") String search,
        @Param("status") UserStatus status,
        @Param("department") String department,
        Pageable pageable
    );
}
