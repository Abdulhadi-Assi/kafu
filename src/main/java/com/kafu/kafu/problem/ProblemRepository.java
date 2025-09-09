package com.kafu.kafu.problem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {
    
    @Query("SELECT p FROM Problem p WHERE p.isReal = true AND p.status != 'PENDING_APPROVAL'")
    Page<Problem> findRealProblemsNotPendingApproval(Pageable pageable);
}
