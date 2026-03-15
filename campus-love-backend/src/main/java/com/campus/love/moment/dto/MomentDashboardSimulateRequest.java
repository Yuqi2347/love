package com.campus.love.moment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MomentDashboardSimulateRequest {

    @NotBlank
    private String weekTag;

    @NotNull
    @Min(0)
    private Integer threshold;
}
