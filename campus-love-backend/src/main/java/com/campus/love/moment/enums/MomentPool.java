package com.campus.love.moment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MomentPool {
    MF("MF", "异性匹配"),
    MM("MM", "同性匹配-M"),
    FF("FF", "同性匹配-F");

    private final String code;
    private final String label;

    /**
     * 根据用户性别和目标性别确定匹配池（支持双池：any 时男→MF+MM，女→MF+FF）
     * @param userGender 用户性别 (1=男, 2=女)
     * @param targetGender 目标性别: "male", "female", "any"
     */
    public static java.util.List<MomentPool> determine(Integer userGender, String targetGender) {
        boolean isMale = userGender != null && userGender == 1;
        boolean isFemale = userGender != null && userGender == 2;

        if ("any".equalsIgnoreCase(targetGender)) {
            if (isMale) return java.util.List.of(MF, MM);
            if (isFemale) return java.util.List.of(MF, FF);
            return java.util.List.of(MF);
        }
        if ("male".equalsIgnoreCase(targetGender)) {
            return java.util.List.of(isMale ? MM : MF);
        }
        if ("female".equalsIgnoreCase(targetGender)) {
            return java.util.List.of(isFemale ? FF : MF);
        }
        return java.util.List.of(MF);
    }
}
