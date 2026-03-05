package com.campus.love.common.model;

import com.campus.love.common.enums.DiZhi;
import com.campus.love.common.enums.TianGan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

/**
 * 四柱数据模型
 * 包含年柱、月柱、日柱、时柱
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FourPillars {

    /**
     * 年柱（代表祖辈/早年）
     */
    private Pillar yearPillar;

    /**
     * 月柱（代表父母/青年）
     */
    private Pillar monthPillar;

    /**
     * 日柱（代表配偶/本人）- 最重要
     */
    private Pillar dayPillar;

    /**
     * 时柱（代表子嗣/晚年）
     */
    private Pillar hourPillar;

    /**
     * 获取所有天干
     */
    public List<TianGan> getAllGans() {
        return Stream.of(yearPillar, monthPillar, dayPillar, hourPillar)
                .filter(p -> p != null && p.getGan() != null)
                .map(Pillar::getGan)
                .toList();
    }

    /**
     * 获取所有地支
     */
    public List<DiZhi> getAllZhis() {
        return Stream.of(yearPillar, monthPillar, dayPillar, hourPillar)
                .filter(p -> p != null && p.getZhi() != null)
                .map(Pillar::getZhi)
                .toList();
    }

    /**
     * 转为列表（遍历用）
     */
    public List<Pillar> toList() {
        return Stream.of(yearPillar, monthPillar, dayPillar, hourPillar)
                .filter(p -> p != null)
                .toList();
    }

    /**
     * 获取日柱天干（日主，代表本人）
     */
    public TianGan getDayMaster() {
        return dayPillar != null ? dayPillar.getGan() : null;
    }

    /**
     * 获取八字字符串表示（如"己卯 丁卯 甲午 乙未"）
     */
    public String getDisplayName() {
        return String.format("%s %s %s %s",
                yearPillar != null ? yearPillar.getDisplayName() : "",
                monthPillar != null ? monthPillar.getDisplayName() : "",
                dayPillar != null ? dayPillar.getDisplayName() : "",
                hourPillar != null ? hourPillar.getDisplayName() : ""
        ).trim();
    }
}
