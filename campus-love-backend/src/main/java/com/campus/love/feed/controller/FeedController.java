package com.campus.love.feed.controller;

import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.result.Result;
import com.campus.love.common.result.ResultCode;
import com.campus.love.common.service.FileUploadService;
import com.campus.love.feed.dto.FeedCommentRequest;
import com.campus.love.feed.dto.FeedPostRequest;
import com.campus.love.feed.dto.FeedPostResponse;
import com.campus.love.feed.dto.FeedShareRequest;
import com.campus.love.feed.service.FeedService;
import com.campus.love.user.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "朋友圈", description = "动态发布与互动")
@Slf4j
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final ActivityService activityService;
    private final FileUploadService fileUploadService;

    @Operation(summary = "发布动态")
    @PostMapping
    public Result<FeedPostResponse> createPost(@Valid @RequestBody FeedPostRequest request) {
        return Result.success(feedService.createPost(request));
    }

    @Operation(summary = "发布发现模块动态")
    @PostMapping("/discovery")
    public Result<FeedPostResponse> createDiscoveryPost(@Valid @RequestBody FeedPostRequest request) {
        return Result.success(feedService.createDiscoveryPost(request));
    }

    @Operation(summary = "获取朋友圈动态流", description = "互相关注用户的动态")
    @GetMapping("/timeline")
    public Result<List<FeedPostResponse>> getTimeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getTimeline(page, size));
    }

    @Operation(summary = "用户动态摘要（总数+最近图片，用于个人主页入口卡片）")
    @GetMapping("/user/{userId}/summary")
    public Result<java.util.Map<String, Object>> getUserSummary(@PathVariable Long userId) {
        return Result.success(feedService.getUserPostsSummary(userId));
    }

    @Operation(summary = "获取用户动态列表")
    @GetMapping("/user/{userId}")
    public Result<List<FeedPostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getUserPosts(userId, page, size));
    }

    @Operation(summary = "获取用户的朋友圈帖子")
    @GetMapping("/user/{userId}/timeline")
    public Result<List<FeedPostResponse>> getUserTimelinePosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getUserTimelinePosts(userId, page, size));
    }

    @Operation(summary = "获取用户的发现模块帖子")
    @GetMapping("/user/{userId}/discovery")
    public Result<List<FeedPostResponse>> getUserDiscoveryPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(feedService.getUserDiscoveryPosts(userId, page, size));
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

    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        feedService.deleteComment(commentId);
        return Result.success();
    }

    @Operation(summary = "删除动态")
    @DeleteMapping("/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        feedService.deletePost(postId);
        return Result.success();
    }

    @Operation(summary = "更新动态 AI 标签（仅作者可修改）")
    @PatchMapping("/{postId}/ai-tags")
    public Result<Void> updatePostAiTags(
            @PathVariable Long postId,
            @RequestParam String aiTags) {
        feedService.updatePostAiTags(postId, aiTags);
        return Result.success();
    }

    @Operation(summary = "重新生成 AI 标签（作者可触发，用于无标签的帖子）")
    @PostMapping("/retag/{postId}")
    public Result<Void> retagPost(@PathVariable Long postId) {
        feedService.retagPost(postId);
        return Result.success();
    }

    @Operation(summary = "获取我点赞的帖子列表")
    @GetMapping("/liked-posts")
    public Result<List<FeedPostResponse>> getLikedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(feedService.getLikedPosts(page, size));
    }

    @Operation(summary = "获取帖子详情", description = "单条帖子及完整评论列表（爬楼式）")
    @GetMapping("/{postId}")
    public Result<FeedPostResponse> getPostDetail(@PathVariable Long postId) {
        return Result.success(feedService.getPostDetail(postId));
    }

    @Operation(summary = "获取发现模块帖子列表", description = "按时间排序，展示管理员和高级用户发布的帖子")
    @GetMapping("/discovery")
    public Result<List<FeedPostResponse>> getDiscoveryPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(feedService.getDiscoveryPosts(page, size, keyword));
    }

    @Operation(summary = "获取我的等级信息")
    @GetMapping("/level-info")
    public Result<ActivityService.UserLevelInfo> getLevelInfo() {
        return Result.success(activityService.getUserLevelInfo(null));
    }

    @Operation(summary = "我发布的帖子收到的新点赞/评论数量（用于导航红点）")
    @GetMapping("/activity/new-count")
    public Result<Integer> getNewFeedActivityCount() {
        return Result.success(feedService.getNewFeedActivityCount(CurrentUser.getId()));
    }

    @Operation(summary = "标记朋友圈活动已查看，消除红点")
    @PutMapping("/activity/mark-viewed")
    public Result<Void> markFeedActivityViewed() {
        feedService.markFeedActivityViewed(CurrentUser.getId());
        return Result.success();
    }

    @Operation(summary = "上传朋友圈图片")
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileUploadService.uploadImage(file, "feed_img_");
            return Result.success("上传成功", url);
        } catch (IllegalArgumentException e) {
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return Result.error(ResultCode.INTERNAL_ERROR, "文件上传失败，请稍后重试");
        }
    }

    @Operation(summary = "上传朋友圈视频")
    @PostMapping("/upload/video")
    public Result<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileUploadService.uploadVideo(file, "feed_video_");
            return Result.success("上传成功", url);
        } catch (IllegalArgumentException e) {
            return Result.error(ResultCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return Result.error(ResultCode.INTERNAL_ERROR, "文件上传失败，请稍后重试");
        }
    }

    @Operation(summary = "获取社交通知（赞/评论/@）")
    @GetMapping("/social-notifications")
    public Result<List<java.util.Map<String, Object>>> getSocialNotifications() {
        return Result.success(feedService.getSocialNotifications(CurrentUser.getId()));
    }

    @Operation(summary = "分享帖子", description = "将帖子分享给一个或多个互关朋友")
    @PostMapping("/share")
    public Result<Void> sharePost(@Valid @RequestBody FeedShareRequest request) {
        feedService.sharePost(request.getPostId(), request.getReceiverIds());
        return Result.success();
    }
}
