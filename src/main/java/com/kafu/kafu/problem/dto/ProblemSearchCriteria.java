package com.kafu.kafu.problem.dto;

import com.kafu.kafu.address.City;
import com.kafu.kafu.problem.ProblemStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProblemSearchCriteria {

    @Size(max = 100)
    private String searchText;
    private Boolean forContribution;
    private Boolean forDonation;
    private City city;
    private ProblemStatus status;

    @Min(1) @Max(1000)
    private Long categoryId;
    // Add new fields for gov search
    private Long govId;
    private String govName;
    private Boolean isReal;
}
