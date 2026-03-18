package com.campus.love.moment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotNull
    private Boolean autoMatchEnabled;

    @NotNull
    @Min(1)
    private Integer autoMatchDayOfWeek; // 1=Mon..7=Sun

    @NotNull
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "autoMatchTime 必须为 HH:mm")
    private String autoMatchTime;
}
