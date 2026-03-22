package com.campus.love.pairdate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PairDateYueRequest {
    @NotNull
    private Long matchResultId;
}
