package com.campus.love.ai.agent;

final class MomentAiJsonSupport {

    private MomentAiJsonSupport() {
    }

    static String clean(String content) {
        if (content == null) {
            return "";
        }
        String json = content.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        json = json.replace("「", "").replace("」", "")
                .replace("『", "").replace("』", "")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"");
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        return json.trim();
    }
}
