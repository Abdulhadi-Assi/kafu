package com.kafu.kafu.problem;

import com.kafu.kafu.address.Address;
import com.kafu.kafu.address.AddressService;
import com.kafu.kafu.exception.ApplicationErrorEnum;
import com.kafu.kafu.exception.BusinessException;
import com.kafu.kafu.problem.dto.ProblemDTO;
import com.kafu.kafu.problem.dto.ProblemSearchCriteria;
import com.kafu.kafu.problemcategory.ProblemCategory;
import com.kafu.kafu.problemcategory.ProblemCategoryService;
import com.kafu.kafu.user.User;
import com.kafu.kafu.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final AddressService addressService;
    private final UserService userService;
    private final ProblemCategoryService problemCategoryService;

    public Page<Problem> search(ProblemSearchCriteria criteria , Pageable pageable) {
        Specification<Problem> spec = ProblemSpecification.withSearchCriteria(criteria);
        return problemRepository.findAll(spec, pageable);
    }

    public Problem findById(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.PROBLEM_NOT_FOUND));
    }

    @Transactional
    public Problem create(ProblemDTO problemDTO) {
        //set submitted uuser id here

        if (problemDTO.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new problem cannot already have an ID");
        }

        // Verify that the submitting user exists
        var user =  userService.getCurrentUser();

        // Get the address if provided
        Address address = null;
        if (problemDTO.getAddressId() != null) {
            address = addressService.findById(problemDTO.getAddressId());
        }

        Problem problem = new Problem();
        problem.setTitle(problemDTO.getTitle());
        problem.setDescription(problemDTO.getDescription());
        problem.setSubmittedByUser(user);
        problem.setAddress(address);
        
        // Set default values
        problem.setIsReal(false);
        problem.setForContribution(false);
        problem.setForDonation(false);
        problem.setSubmissionDate(LocalDateTime.now());
        problem.setStatus(ProblemStatus.PENDING_APPROVAL);
        problem.setRejectionReason("");

        ProblemCategory problemCategory = problemCategoryService.findById(problemDTO.getCategoryId());
        problem.setCategory(problemCategory);

        problem = problemRepository.save(problem);
        return problem;
    }

    @Transactional
    public Problem update(Long id, ProblemDTO problemDTO) {
        Problem problem = findById(id);

        // Verify that the submitting user exists if it's being updated
        if (problemDTO.getSubmittedByUserId() != null) {
            problem.setSubmittedByUser(userService.findById(problemDTO.getSubmittedByUserId()));
        }

        // Verify that the approving user exists if it's being updated
        if (problemDTO.getApprovedByUserId() != null) {
            problem.setApprovedByUser(userService.findById(problemDTO.getApprovedByUserId()));
        }

        // Update the address if provided
        if (problemDTO.getAddressId() != null) {
            problem.setAddress(addressService.findById(problemDTO.getAddressId()));
        }

        // Update the category if provided
        if (problemDTO.getCategoryId() != null) {
            problem.setCategory(problemCategoryService.findById(problemDTO.getAddressId()));
        }

        ProblemMapper.updateEntity(problem, problemDTO);

        problem = problemRepository.save(problem);
        return problem;
    }

    @Transactional
    public void delete(Long id) {
        problemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.PROBLEM_NOT_FOUND));
        
        try {
            problemRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete problem as it is being referenced by other entities");
        }
    }


    @Transactional
    public Problem patch(Long id, ProblemDTO problemDTO) {
        Problem problem = findById(id);
        User currentUser = userService.getCurrentUser();

        // Check if the user has permission to update this problem
        validateUserPermission(problem, currentUser);

        // Handle status-based updates
        if (problemDTO.getStatus() != null) {
            handleStatusUpdate(problem, problemDTO, currentUser);
        }

        // Handle real fields update
        if (problemDTO.getIsReal() != null || 
            problemDTO.getForContribution() != null || 
            problemDTO.getForDonation() != null) {
            handleRealFieldsUpdate(problem, problemDTO);
        }

        // Handle basic details update
        handleDetailsUpdate(problem, problemDTO);

        return problemRepository.save(problem);
    }

    private void validateUserPermission(Problem problem, User currentUser) {
//        boolean isAdmin = // admin check logic
//        boolean isOwner = problem.getSubmittedByUser().getId().equals(currentUser.getId());
//
//        if (!isAdmin && !isOwner) {
//            throw new BusinessException(ApplicationErrorEnum.UNAUTHORIZED_ACTION);
//        }
    }

    private void handleStatusUpdate(Problem problem, ProblemDTO problemDTO, User currentUser) {
        if (problemDTO.getStatus() == null) return;

        switch (problemDTO.getStatus()) {
            case APPROVED -> {
                if (problem.getStatus() != ProblemStatus.PENDING_APPROVAL) {
                    throw new BusinessException(ApplicationErrorEnum.INVALID_PROBLEM_STATUS);
                }
                problem.setStatus(ProblemStatus.APPROVED);
                problem.setApprovedByUser(currentUser);
            }
            case REJECTED -> {
                if (problem.getStatus() != ProblemStatus.PENDING_APPROVAL) {
                    throw new BusinessException(ApplicationErrorEnum.INVALID_PROBLEM_STATUS);
                }
                if (problemDTO.getRejectionReason() == null || problemDTO.getRejectionReason().trim().isEmpty()) {
                    throw new BusinessException(ApplicationErrorEnum.REJECTION_REASON_REQUIRED);
                }
                problem.setStatus(ProblemStatus.REJECTED);
                problem.setRejectionReason(problemDTO.getRejectionReason());
                problem.setApprovedByUser(currentUser);
            }
            case IN_PROGRESS -> {
                if (problem.getStatus() != ProblemStatus.APPROVED) {
                    throw new BusinessException(ApplicationErrorEnum.INVALID_PROBLEM_STATUS);
                }
                problem.setStatus(ProblemStatus.IN_PROGRESS);
            }
            case RESOLVED -> {
                if (problem.getStatus() != ProblemStatus.IN_PROGRESS) {
                    throw new BusinessException(ApplicationErrorEnum.INVALID_PROBLEM_STATUS);
                }
                problem.setStatus(ProblemStatus.RESOLVED);
            }
            default -> throw new BusinessException(ApplicationErrorEnum.INVALID_PROBLEM_STATUS);
        }
    }

    private void handleRealFieldsUpdate(Problem problem, ProblemDTO problemDTO) {
        if (problem.getStatus() != ProblemStatus.PENDING_APPROVAL) {
            throw new BusinessException(ApplicationErrorEnum.INVALID_PROBLEM_STATUS);
        }

        if (problemDTO.getIsReal() != null) {
            problem.setIsReal(problemDTO.getIsReal());
        }
        if (problemDTO.getForContribution() != null) {
            problem.setForContribution(problemDTO.getForContribution());
        }
        if (problemDTO.getForDonation() != null) {
            problem.setForDonation(problemDTO.getForDonation());
        }
    }

    private void handleDetailsUpdate(Problem problem, ProblemDTO problemDTO) {
        if (problem.getStatus() != ProblemStatus.PENDING_APPROVAL) {
            throw new BusinessException(ApplicationErrorEnum.INVALID_PROBLEM_STATUS);
        }

        if (problemDTO.getTitle() != null) {
            problem.setTitle(problemDTO.getTitle());
        }
        if (problemDTO.getDescription() != null) {
            problem.setDescription(problemDTO.getDescription());
        }
        if (problemDTO.getAddressId() != null) {
            problem.setAddress(addressService.findById(problemDTO.getAddressId()));
        }
        if (problemDTO.getCategoryId() != null) {
            problem.setCategory(problemCategoryService.findById(problemDTO.getCategoryId()));
        }
    }

    public List<Problem> findProblemsForCurrentUser() {
        Long userId = userService.getCurrentUser().getId();
        return problemRepository.findBySubmittedByUser_Id(userId);
    }
}
