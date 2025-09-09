package com.kafu.kafu.problem;

import com.kafu.kafu.problem.dto.ProblemDTO;
import com.kafu.kafu.problem.dto.ProblemDetailsDTO;
import com.kafu.kafu.problem.dto.ProblemSearchCriteria;
import com.kafu.kafu.problem.dto.UserProblemSearchCriteria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemController {
    private final ProblemService problemService;

    @GetMapping
    public ResponseEntity<Page<ProblemDTO>> findAll(@ModelAttribute ProblemSearchCriteria criteria,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(problemService.search(criteria,pageable).map(ProblemMapper::toDTO));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProblemDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ProblemMapper.toDTO(problemService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ProblemDTO> create(@Valid @RequestBody ProblemDetailsDTO problemDetailsDTO) {
        return ResponseEntity.ok(ProblemMapper.toDTO(problemService.create(problemDetailsDTO)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProblemDTO> patch(@PathVariable Long id, @RequestBody ProblemDTO problemDTO) {
        return ResponseEntity.ok(ProblemMapper.toDTO(problemService.patch(id, problemDTO)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        problemService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Page<ProblemDTO>> findByUserId(
            @ModelAttribute UserProblemSearchCriteria criteria,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(problemService.searchUserProblems(criteria, pageable)
                .map(ProblemMapper::toDTO));
    }

    @GetMapping("/real")
    public ResponseEntity<Page<ProblemDTO>> findRealProblemsNotPendingApproval(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(problemService.findRealProblemsNotPendingApproval(pageable)
                .map(ProblemMapper::toDTO));
    }
}
