package com.kafu.kafu.problemcategory;

import com.kafu.kafu.exception.ApplicationErrorEnum;
import com.kafu.kafu.exception.BusinessException;
import com.kafu.kafu.gov.Gov;
import com.kafu.kafu.gov.GovService;
import com.kafu.kafu.problemcategory.dto.ProblemCategorySearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemCategoryService {
    private final ProblemCategoryRepository problemCategoryRepository;
    private final GovService govService;

    public ProblemCategory findById(Long id) {
        return problemCategoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.CATEGORY_NOT_FOUND));
    }

    @Transactional
    public ProblemCategory create(ProblemCategoryDTO problemCategoryDTO) {
        if (problemCategoryDTO.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new problem category cannot already have an ID");
        }

        // This will throw 404 if gov not found
        Gov gov = govService.findById(problemCategoryDTO.getGovId());

        if (problemCategoryRepository.existsByNameAndGovId(problemCategoryDTO.getName(), problemCategoryDTO.getGovId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Problem category with this name already exists for the given gov");
        }

        ProblemCategory problemCategory = ProblemCategoryMapper.toEntity(problemCategoryDTO);
        problemCategory.setGov(gov);
        problemCategory = problemCategoryRepository.save(problemCategory);
        return problemCategory;
    }

    @Transactional
    public ProblemCategory update(Long id, ProblemCategoryDTO problemCategoryDTO) {
        ProblemCategory problemCategory = problemCategoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.CATEGORY_NOT_FOUND));

        if (!problemCategory.getName().equals(problemCategoryDTO.getName()) && 
            problemCategoryRepository.existsByNameAndGovId(problemCategoryDTO.getName(), problemCategoryDTO.getGovId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Problem category with this name already exists for the given gov");
        }

        // This will throw 404 if gov not found
        Gov gov = govService.findById(problemCategoryDTO.getGovId());

        ProblemCategoryMapper.updateEntity(problemCategory, problemCategoryDTO);
        problemCategory.setGov(gov);
        problemCategory = problemCategoryRepository.save(problemCategory);
        return problemCategory;
    }

    @Transactional
    public void delete(Long id) {
        problemCategoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApplicationErrorEnum.CATEGORY_NOT_FOUND));
        
        try {
            problemCategoryRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete problem category as it is being referenced by other entities");
        }
    }

    public List<ProblemCategory> search(ProblemCategorySearchCriteria criteria) {
        Specification<ProblemCategory> spec = ProblemCategorySpecification.withSearchCriteria(criteria);
        return problemCategoryRepository.findAll(spec);
    }
}
