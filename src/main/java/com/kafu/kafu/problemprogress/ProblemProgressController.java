package com.kafu.kafu.problemprogress;

import com.kafu.kafu.problemprogress.dto.ProblemProgressDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/problem/{problemId}/progress")
@RequiredArgsConstructor
public class ProblemProgressController {
    private final ProblemProgressService problemProgressService;

    @GetMapping
    public ResponseEntity<List<ProblemProgressDTO>> findByProblemId(@PathVariable Long problemId) {
        return ResponseEntity.ok(problemProgressService.findByProblemId(problemId).stream().map(ProblemProgressMapper::toDTO).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemProgressDTO> findById(@PathVariable Long problemId, @PathVariable Long id) {
        return ResponseEntity.ok(ProblemProgressMapper.toDTO(problemProgressService.findByIdAndProblemId(id, problemId)));
    }

    @PostMapping
    public ResponseEntity<ProblemProgressDTO> create(
            @PathVariable Long problemId,
            @Valid @RequestBody ProblemProgressDTO progressDTO) {
        progressDTO.setProblemId(problemId);
        return ResponseEntity.ok(ProblemProgressMapper.toDTO(problemProgressService.create(progressDTO)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long problemId, @PathVariable Long id) {
        problemProgressService.deleteByIdAndProblemId(id, problemId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemProgressDTO> update(
            @PathVariable Long problemId,
            @PathVariable Long id,
            @Valid @RequestBody ProblemProgressDTO progressDTO) {
        return ResponseEntity.ok(ProblemProgressMapper.toDTO(problemProgressService.update(problemId,id, progressDTO)));
    }
}