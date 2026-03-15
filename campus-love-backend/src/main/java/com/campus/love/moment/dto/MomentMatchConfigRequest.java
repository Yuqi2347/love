package com.campus.love.moment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MomentMatchConfigRequest {

    @NotNull
    @Min(0)
    private Integer baseThreshold;

    @NotNull
    @Min(0)
    private Integer prioritizeOffset;

    @NotNull
    @Min(0)
    private Integer priorityOffset;

    @NotNull
    @Min(0)
    private Integer priorityMaxStack;
}
