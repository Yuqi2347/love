package com.campus.love.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReportRequest {

    @NotBlank(message = "举报类型不能为空")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    /** 违规类型列表（多选） */
    private List<String> violationTypes;

    /** 举报理由（选填） */
    @Size(max = 500, message = "举报理由不能超过500字")
    private String reason;
}
