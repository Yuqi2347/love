package com.campus.love.report.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportReviewRequest {

    @Size(max = 500, message = "管理员备注不能超过500字")
    private String adminNote;

    private String status;
}
