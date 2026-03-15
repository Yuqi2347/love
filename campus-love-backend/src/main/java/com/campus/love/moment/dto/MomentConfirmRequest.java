package com.campus.love.moment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MomentConfirmRequest {

    @NotBlank
    @Pattern(regexp = "YUE|GUANZHU", message = "choice 仅支持 YUE 或 GUANZHU")
    private String choice;
}
