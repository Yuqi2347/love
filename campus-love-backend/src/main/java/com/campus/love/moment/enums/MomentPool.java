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
     * 根据用户性别和目标性别确定匹配池
     * @param userGender 用户性别 (1=男, 2=女)
     * @param targetGender 目标性别: "male", "female", "any"
     */
    public static MomentPool determine(Integer userGender, String targetGender) {
        boolean isMale = userGender != null && userGender == 1;
        boolean isFemale = userGender != null && userGender == 2;

        if ("male".equalsIgnoreCase(targetGender)) {
            return isMale ? MM : MF;
        }
        if ("female".equalsIgnoreCase(targetGender)) {
            return isFemale ? FF : MF;
        }
        // "any" 或其他情况：默认进 MF 池
        return MF;
    }
}
