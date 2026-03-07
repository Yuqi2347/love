package com.campus.love.moment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MomentEnrollRequest {

    @NotNull(message = "自评颜值分不能为空")
    @Min(value = 1, message = "自评颜值分最低1分")
    @Max(value = 10, message = "自评颜值分最高10分")
    private Integer selfScore;

    @NotBlank(message = "期望匹配性别不能为空")
    private String targetGender; // male / female / any

    // Q3-Q15 问卷答案
    @NotBlank private String socialStyle;          // Q3: A/B/C
    @NotBlank private String lifeRhythm;           // Q4: A/B/C
    @NotBlank private String companionshipStyle;   // Q5: A/B/C
    @NotBlank private String appearanceRequirement; // Q6: A/B/C
    @NotBlank private String partnerPersonality;   // Q7: A/B/C
    @NotBlank private String majorPreference;      // Q8: A/B/C
    @NotBlank private String ageRangePreference;   // Q9: A/B/C/D (可多选逗号分隔)
    @NotBlank private String dateStyle;            // Q10: A/B/C
    @NotBlank private String intimacyPace;         // Q11: A/B/C
    @NotBlank private String loyaltyValue;         // Q12: A/B/C
    @NotBlank private String premaritalCohabitation; // Q13: A/B/C
    @NotBlank private String futureLifestyle;      // Q14: A/B/C
    @NotBlank private String relationshipCoreValue; // Q15: A/B/C/D
}
