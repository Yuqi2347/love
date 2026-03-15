package com.campus.love.moment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentDatePrepResponse {

    private String dateSceneType;
    private String dateSuggestion;
    private List<IceBreakTopic> iceBreakTopics;
    private String surpriseIdea;
    private String outfitAdvice;
    private String mindsetAdvice;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IceBreakTopic {
        private String title;
        private String opener;
    }
}
