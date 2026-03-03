package com.campus.love.feed.controller;

import com.campus.love.common.result.Result;
import com.campus.love.feed.dto.FeedCommentRequest;
import com.campus.love.feed.dto.FeedPostRequest;
import com.campus.love.feed.dto.FeedPostResponse;
import com.campus.love.feed.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "朋友圈", description = "动态发布与互动")
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "发布动态")
    @PostMapping
    public Result<FeedPostResponse> createPost(@Valid @RequestBody FeedPostRequest request) {
        return Result.success(feedService.createPost(request));
    }

    @Operation(summary = "获取朋友圈动态流", description = "互相关注用户的动态")
    @GetMapping("/timeline")
    public Result<List<FeedPostResponse>> getTimeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getTimeline(page, size));
    }

    @Operation(summary = "获取用户动态列表")
    @GetMapping("/user/{userId}")
    public Result<List<FeedPostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getUserPosts(userId, page, size));
    }

    @Operation(summary = "点赞")
    @PostMapping("/like/{postId}")
    public Result<Void> like(@PathVariable Long postId) {
        feedService.likePost(postId);
        return Result.success();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/like/{postId}")
    public Result<Void> unlike(@PathVariable Long postId) {
        feedService.unlikePost(postId);
        return Result.success();
    }

    @Operation(summary = "评论")
    @PostMapping("/comment")
    public Result<Void> comment(@Valid @RequestBody FeedCommentRequest request) {
        feedService.addComment(request);
        return Result.success();
    }

    @Operation(summary = "删除动态")
    @DeleteMapping("/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        feedService.deletePost(postId);
        return Result.success();
    }
}
