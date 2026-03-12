package com.campus.love.feed.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FeedShareRequest {

    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    @NotEmpty(message = "接收人不能为空")
    private List<Long> receiverIds;
}