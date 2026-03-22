package com.campus.love.pairdate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PairDateSubmitRequest {

    @NotNull
    @Min(1)
    @Max(3)
    private Integer step;

    /** step=1 时必填，1～3 */
    @Min(1)
    @Max(3)
    private Integer excludedRank;

    /** step=2，时段编码如 SAT_AM */
    private List<String> timeSlots;

    /** step=3：SELF / PARTNER / EITHER */
    private String locationChoice;
}
