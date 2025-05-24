package com.kafu.kafu.problemprogress;

import com.kafu.kafu.exception.ApplicationErrorEnum;
import com.kafu.kafu.exception.BusinessException;
import com.kafu.kafu.problem.Problem;
import com.kafu.kafu.problem.ProblemService;
import com.kafu.kafu.problem.ProblemStatus;
import com.kafu.kafu.problemprogress.dto.ProblemProgressDTO;
import com.kafu.kafu.solution.SolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemProgressService {
    private final ProblemProgressRepository problemProgressRepository;
    private final ProblemService problemService;
    private final SolutionService solutionService;

    public List<ProblemProgress> findByProblemId(Long problemId) {
        return problemProgressRepository.findByProblemId(problemId);
    }

    public ProblemProgress findByIdAndProblemId(Long id, Long problemId) {
        return problemProgressRepository.findByIdAndProblemId(id, problemId)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.PROGRESS_NOT_FOUND));
    }

    public ProblemProgress findById(Long id) {
        return problemProgressRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.PROGRESS_NOT_FOUND));
    }

    public void validateProblemId(ProblemProgress progress, Long problemId) {
        if (!progress.getProblem().getId().equals(problemId)) {
            throw new BusinessException(ApplicationErrorEnum.PROGRESS_NOT_FOUND);
        }
    }

    public ProblemProgress findByIdAndValidateProblem(Long id, Long problemId) {
        ProblemProgress progress = findById(id);
        validateProblemId(progress, problemId);
        return progress;
    }

    @Transactional
    public void deleteByIdAndProblemId(Long id, Long problemId) {
        ProblemProgress progress = findByIdAndValidateProblem(id, problemId);
        problemProgressRepository.deleteById(id);
    }

    @Transactional
    public ProblemProgress create(ProblemProgressDTO dto) {
        Problem problem = problemService.findById(dto.getProblemId());
        if(problem.getStatus() != ProblemStatus.IN_PROGRESS)
        {
            throw new RuntimeException("Problem is not in progress");
        }

        ProblemProgress progress = new ProblemProgress();
        progress.setProblem(problem);
        progress.setSolution(solutionService.findById(dto.getSolutionId()));
        progress.setPercentage(dto.getPercentage());
        progress.setComment(dto.getComment());
        progress.setProgressDate(LocalDateTime.now());
        
        progress = problemProgressRepository.save(progress);
        return progress;
    }



    @Transactional
    public void delete(Long id) {
        problemProgressRepository.deleteById(id);
    }
}