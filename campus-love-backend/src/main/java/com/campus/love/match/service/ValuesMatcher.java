package com.campus.love.match.service;

import com.campus.love.profile.entity.UserPortrait;
import org.springframework.stereotype.Service;

import java.util.OptionalInt;

@Service
public class ValuesMatcher {

    public OptionalInt calculateValuesScore(UserPortrait selfPortrait, UserPortrait targetPortrait) {
        int selfTier = premaritalSexToTier(selfPortrait != null ? selfPortrait.getPremaritalSex() : null);
        int targetTier = premaritalSexToTier(targetPortrait != null ? targetPortrait.getPremaritalSex() : null);
        if (selfTier == 0 || targetTier == 0) {
            return OptionalInt.empty();
        }

        int diff = Math.abs(selfTier - targetTier);
        return OptionalInt.of(switch (diff) {
            case 0 -> 100;
            case 1 -> 70;
            case 2 -> 40;
            default -> 20;
        });
    }

    private int premaritalSexToTier(String value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 0;
        };
    }
}
