package com.campus.love.common.model;

import com.campus.love.common.enums.DiZhi;
import com.campus.love.common.enums.TianGan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 柱（天干+地支）
 * 八字中的单柱，如"甲子"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pillar {

    /**
     * 天干
     */
    private TianGan gan;

    /**
     * 地支
     */
    private DiZhi zhi;

    /**
     * 获取柱的字符串表示（如"甲子"）
     */
    public String getDisplayName() {
        if (gan == null || zhi == null) return "";
        return gan.getChineseChar() + zhi.getChineseChar();
    }

    /**
     * 创建指定天干地支的柱
     */
    public static Pillar of(TianGan gan, DiZhi zhi) {
        return new Pillar(gan, zhi);
    }
}
