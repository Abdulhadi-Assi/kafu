package com.kafu.kafu.solution;

import com.kafu.kafu.exception.ApplicationErrorEnum;
import com.kafu.kafu.exception.BusinessException;
import com.kafu.kafu.problem.Problem;
import com.kafu.kafu.problem.ProblemService;
import com.kafu.kafu.problem.ProblemStatus;
import com.kafu.kafu.solution.dto.SolutionDTO;
import com.kafu.kafu.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolutionService {
    private final SolutionRepository solutionRepository;
    private final ProblemService problemService;
    private final UserService userService;

    public Solution findById(Long id) {
        return solutionRepository.findById(id).orElseThrow(() -> new BusinessException(ApplicationErrorEnum.SOLUTION_NOT_FOUND));
    }

    public List<Solution> findSolutionsProposedByCurrentUser() {
        Long userId = userService.getCurrentUser().getId();
        return solutionRepository.findByProposedByUserId_Id(userId);
    }

    public List<Solution> findByProblemId(Long problemId) {
        return solutionRepository.findByProblem_Id(problemId);
    }

    @Transactional
    public Solution create(SolutionDTO solutionDTO) {

        Problem problem = problemService.findById(solutionDTO.getProblemId());
        if(problem.getStatus() != ProblemStatus.APPROVED || !problem.getForContribution())
        {
            throw new RuntimeException("problem not for contribution");
        }
        Solution solution = new Solution();
        solution.setDescription(solutionDTO.getDescription());
        solution.setEstimatedCost(solutionDTO.getEstimatedCost());
        // Handle relations
        if (solutionDTO.getProblemId() != null) {
            solution.setProblem(problem);
        }

        solution.setProposedByUserId(userService.getCurrentUser());

        if (solutionDTO.getAcceptedByUserId() != null) {
            solution.setAcceptedByUserId(userService.findById(solutionDTO.getAcceptedByUserId()));
        }
        solution.setStatus(SolutionStatus.PENDING_APPROVAL);
        SolutionMapper.updateEntity(solution,solutionDTO);
        solution = solutionRepository.save(solution);
        return solution;
    }

    @Transactional
    public Solution update(Long id, SolutionDTO solutionDTO) {
        Problem problem = problemService.findById(solutionDTO.getProblemId());
        if(problem.getStatus() != ProblemStatus.APPROVED || !problem.getForContribution())
        {
            throw new RuntimeException("problem not for contribution");
        }
        Solution solution = findById(id);
        // Handle relations
        if (solutionDTO.getProblemId() != null) {
            solution.setProblem(problemService.findById(solutionDTO.getProblemId()));
        }
        if (solutionDTO.getProposedByUserId() != null) {
            solution.setProposedByUserId(userService.findById(solutionDTO.getProposedByUserId()));
        }
        if (solutionDTO.getAcceptedByUserId() != null) {
            solution.setAcceptedByUserId(userService.findById(solutionDTO.getAcceptedByUserId()));
        }
        solution = SolutionMapper.toEntity(solutionDTO);
        solution = solutionRepository.save(solution);
        return solution;
    }

    @Transactional
    public void delete(Long id) {
        Solution solution = solutionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.SOLUTION_NOT_FOUND));
        if(solution.getAcceptedReason() !=null || solution.getAcceptedByUserId().getId() != null)
        {
            throw new RuntimeException("Cannot delete solution as it is accepted");
        }
        try {
            solutionRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete solution as it is being referenced by other entities");
        }
    }
}
