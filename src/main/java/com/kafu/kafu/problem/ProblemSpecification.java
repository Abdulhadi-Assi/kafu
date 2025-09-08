package com.kafu.kafu.problem;

import com.kafu.kafu.problem.dto.ProblemSearchCriteria;
import com.kafu.kafu.problem.dto.UserProblemSearchCriteria;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class ProblemSpecification {
    
    public static Specification<Problem> withSearchCriteria(ProblemSearchCriteria criteria) {
        return (Root<Problem> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (criteria.getSearchText() != null && !criteria.getSearchText().trim().isEmpty()) {
                String searchPattern = "%" + criteria.getSearchText().toLowerCase() + "%";
                predicate = cb.and(predicate,
                    cb.or(
                        cb.like(cb.lower(root.get("title")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern)
                    )
                );
            }

            if (criteria.getForContribution() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("forContribution"), criteria.getForContribution()));
            }

            if (criteria.getForDonation() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("forDonation"), criteria.getForDonation()));
            }

            if (criteria.getCity() != null) {
                Join<Object, Object> addressJoin = root.join("address");
                predicate = cb.and(predicate, cb.equal(addressJoin.get("city"), criteria.getCity()));
            }

            if (criteria.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getCategoryId() != null) {
                Join<Object, Object> categoryJoin = root.join("category");
                predicate = cb.and(predicate, cb.equal(categoryJoin.get("id"), criteria.getCategoryId()));
            }

            // Add gov search criteria
            if (criteria.getGovId() != null) {
                Join<Object, Object> categoryJoin = root.join("category");
                Join<Object, Object> govJoin = categoryJoin.join("gov");
                predicate = cb.and(predicate, cb.equal(govJoin.get("id"), criteria.getGovId()));
            }

            if (criteria.getGovName() != null && !criteria.getGovName().trim().isEmpty()) {
                Join<Object, Object> categoryJoin = root.join("category");
                Join<Object, Object> govJoin = categoryJoin.join("gov");
                String searchPattern = "%" + criteria.getGovName().toLowerCase() + "%";
                predicate = cb.and(predicate,
                    cb.like(cb.lower(govJoin.get("name")), searchPattern)
                );
            }
            if (criteria.getIsReal() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("isReal"), criteria.getIsReal()));
            }

            return predicate;
        };
    }

    public static Specification<Problem> withUserId(Long userId) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (userId != null) {
                Join<Object, Object> userJoin = root.join("submittedByUser");
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(userJoin.get("id"), userId));
            }
            return predicate;
        };
    }

    public static Specification<Problem> fromUserCriteria(UserProblemSearchCriteria criteria) {
        return Specification.where(withSearchCriteria(criteria))
                .and(withUserId(criteria.getUserId()));
    }
}
