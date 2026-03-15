package com.campus.love.moment.service;

import com.campus.love.moment.entity.MomentProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ComplementaryModeService {

    public List<String> resolveModes(MomentProfile a, MomentProfile b) {
        List<ModeHit> hits = new ArrayList<>();
        addSocialPersonalityModes(hits, a, b);
        addEmotionModes(hits, a, b);
        addLifeRhythmModes(hits, a, b);
        addCompanionshipModes(hits, a, b);
        return hits.stream()
                .sorted(Comparator.comparingInt(ModeHit::priority).reversed())
                .map(ModeHit::title)
                .distinct()
                .toList();
    }

    public String resolvePrimaryTitle(MomentProfile a, MomentProfile b) {
        return resolveModes(a, b).stream().findFirst().orElse("刚好对频");
    }

    private void addSocialPersonalityModes(List<ModeHit> hits, MomentProfile a, MomentProfile b) {
        String typeA = resolveSocialPersonalityType(a);
        String typeB = resolveSocialPersonalityType(b);
        if ("C".equals(typeA) || "C".equals(typeB)) {
            hits.add(new ModeHit("张弛有度", 300));
            return;
        }
        if (isPair(typeA, typeB, "A", "B")) {
            hits.add(new ModeHit("光源×深水", 340));
        } else if (isPair(typeA, typeB, "A", "A")) {
            hits.add(new ModeHit("双向能量", 320));
        } else if (isPair(typeA, typeB, "B", "B")) {
            hits.add(new ModeHit("安静同频", 310));
        }
    }

    private void addEmotionModes(List<ModeHit> hits, MomentProfile a, MomentProfile b) {
        String left = a != null ? a.getEmotionStyle() : null;
        String right = b != null ? b.getEmotionStyle() : null;
        if (isPair(left, right, "A", "B")) {
            hits.add(new ModeHit("表达者×倾听者", 420));
        } else if (isPair(left, right, "A", "A")) {
            hits.add(new ModeHit("情绪共鸣", 410));
        } else if (isPair(left, right, "B", "B")) {
            hits.add(new ModeHit("默契无声", 400));
        }
    }

    private void addLifeRhythmModes(List<ModeHit> hits, MomentProfile a, MomentProfile b) {
        String left = a != null ? a.getLifeRhythm() : null;
        String right = b != null ? b.getLifeRhythm() : null;
        if (isPair(left, right, "A", "B")) {
            hits.add(new ModeHit("秩序×自由", 120));
        } else if (isPair(left, right, "A", "A")) {
            hits.add(new ModeHit("同频节拍", 110));
        } else if (isPair(left, right, "B", "B")) {
            hits.add(new ModeHit("随遇而安", 100));
        }
    }

    private void addCompanionshipModes(List<ModeHit> hits, MomentProfile a, MomentProfile b) {
        String left = a != null ? a.getCompanionshipStyle() : null;
        String right = b != null ? b.getCompanionshipStyle() : null;
        if (isPair(left, right, "A", "B")) {
            hits.add(new ModeHit("靠近×呼吸", 220));
        } else if (isPair(left, right, "A", "A")) {
            hits.add(new ModeHit("紧密相伴", 210));
        } else if (isPair(left, right, "B", "B")) {
            hits.add(new ModeHit("自在同行", 200));
        }
    }

    private String resolveSocialPersonalityType(MomentProfile profile) {
        if (profile == null) {
            return null;
        }
        if (Objects.equals(profile.getSocialStyle(), "C") || Objects.equals(profile.getPersonalityBase(), "C")) {
            return "C";
        }
        if (Objects.equals(profile.getSocialStyle(), "A") || Objects.equals(profile.getPersonalityBase(), "A")) {
            return "A";
        }
        if (Objects.equals(profile.getSocialStyle(), "B") || Objects.equals(profile.getPersonalityBase(), "B")) {
            return "B";
        }
        return null;
    }

    private boolean isPair(String left, String right, String expectedLeft, String expectedRight) {
        return (Objects.equals(left, expectedLeft) && Objects.equals(right, expectedRight))
                || (Objects.equals(left, expectedRight) && Objects.equals(right, expectedLeft));
    }

    private record ModeHit(String title, int priority) {
    }
}
