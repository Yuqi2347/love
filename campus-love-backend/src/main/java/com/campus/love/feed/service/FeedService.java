package com.campus.love.feed.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.constants.UserLevelConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.feed.constants.PostTypeConstants;
import com.campus.love.feed.dto.FeedCommentRequest;
import com.campus.love.feed.dto.FeedPostRequest;
import com.campus.love.feed.dto.FeedPostResponse;
import com.campus.love.feed.entity.FeedComment;
import com.campus.love.feed.entity.FeedLike;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.feed.mapper.FeedCommentMapper;
import com.campus.love.feed.mapper.FeedLikeMapper;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.campus.love.follow.service.FollowService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.campus.love.user.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus.love.common.constants.DateTimeConstants;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedPostMapper feedPostMapper;
    private final FeedLikeMapper feedLikeMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final ActivityService activityService;

    /**
     * 创建朋友圈帖子
     */
    public FeedPostResponse createPost(FeedPostRequest request) {
        Long userId = CurrentUser.getId();

        FeedPost post = new FeedPost();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setImages(request.getImages());
        // 默认发到朋友圈
        post.setPostType(request.getPostType() != null ? request.getPostType() : PostTypeConstants.TIMELINE);
        post.setRequiredLevel(UserLevelConstants.POST_FEED_MIN_LEVEL);
        post.setLikeCount(0);
        post.setCommentCount(0);
        feedPostMapper.insert(post);

        // 记录发布活跃度
        activityService.recordActivity(com.campus.love.common.enums.ActivityTypeEnum.POST, post.getId());

        return toResponse(post, userId);
    }

    /**
     * 创建发现模块帖子（需要权限检查）
     */
    public FeedPostResponse createDiscoveryPost(FeedPostRequest request) {
        Long userId = CurrentUser.getId();

        // 检查用户是否有发布权限
        if (!activityService.canPostFeed(userId)) {
            throw new BusinessException(ResultCode.INSUFFICIENT_LEVEL);
        }

        FeedPost post = new FeedPost();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setImages(request.getImages());
        post.setPostType(PostTypeConstants.DISCOVERY);
        post.setRequiredLevel(UserLevelConstants.POST_FEED_MIN_LEVEL);
        post.setLikeCount(0);
        post.setCommentCount(0);
        feedPostMapper.insert(post);

        // 记录发布活跃度
        activityService.recordActivity(com.campus.love.common.enums.ActivityTypeEnum.POST, post.getId());

        return toResponse(post, userId);
    }

    /**
     * 获取发现模块帖子列表（按时间排序）
     * 只显示 DISCOVERY 类型的帖子
     */
    public List<FeedPostResponse> getDiscoveryPosts(int page, int size) {
        Long currentUserId = CurrentUser.getId();

        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getPostType, PostTypeConstants.DISCOVERY)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + (page * size) + "," + size)
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        return posts.stream().map(p -> {
            FeedPostResponse response = toResponse(p, currentUserId, authorMap);
            // 记录浏览活跃度
            activityService.recordActivity(com.campus.love.common.enums.ActivityTypeEnum.VIEW, p.getId());
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 获取朋友圈动态流（互相关注用户的 TIMELINE 类型帖子）
     */
    public List<FeedPostResponse> getTimeline(int page, int size) {
        Long currentUserId = CurrentUser.getId();
        List<Long> mutualIds = followService.getMutualFollowIds(currentUserId);
        mutualIds.add(currentUserId);

        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .in(FeedPost::getUserId, mutualIds)
                        .eq(FeedPost::getPostType, PostTypeConstants.TIMELINE)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + (page * size) + "," + size)
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        return posts.stream().map(p -> toResponse(p, currentUserId, authorMap)).collect(Collectors.toList());
    }

    public List<FeedPostResponse> getUserPosts(Long userId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, userId)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + (page * size) + "," + size)
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        return posts.stream().map(p -> toResponse(p, currentUserId, authorMap)).collect(Collectors.toList());
    }

    /**
     * 获取用户的朋友圈帖子（TIMELINE类型）
     */
    public List<FeedPostResponse> getUserTimelinePosts(Long userId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, userId)
                        .eq(FeedPost::getPostType, PostTypeConstants.TIMELINE)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + (page * size) + "," + size)
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        return posts.stream().map(p -> toResponse(p, currentUserId, authorMap)).collect(Collectors.toList());
    }

    /**
     * 获取用户的发现模块帖子（DISCOVERY类型）
     */
    public List<FeedPostResponse> getUserDiscoveryPosts(Long userId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, userId)
                        .eq(FeedPost::getPostType, PostTypeConstants.DISCOVERY)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + (page * size) + "," + size)
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        return posts.stream().map(p -> toResponse(p, currentUserId, authorMap)).collect(Collectors.toList());
    }

    private Map<Long, User> batchLoadAuthors(List<FeedPost> posts) {
        if (posts == null || posts.isEmpty()) return Map.of();
        List<Long> userIds = posts.stream().map(FeedPost::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) return Map.of();
        List<User> users = userMapper.selectBatchIds(userIds);
        return users != null ? users.stream().filter(Objects::nonNull).collect(toMap(User::getId, u -> u, (a, b) -> a)) : Map.of();
    }

    @Transactional
    public void likePost(Long postId) {
        Long userId = CurrentUser.getId();
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);

        Long count = feedLikeMapper.selectCount(
                new LambdaQueryWrapper<FeedLike>()
                        .eq(FeedLike::getPostId, postId)
                        .eq(FeedLike::getUserId, userId));
        if (count > 0) throw new BusinessException(ResultCode.ALREADY_LIKED);

        FeedLike like = new FeedLike();
        like.setPostId(postId);
        like.setUserId(userId);
        feedLikeMapper.insert(like);

        post.setLikeCount(post.getLikeCount() + 1);
        feedPostMapper.updateById(post);

        // 记录点赞活跃度
        activityService.recordActivity(com.campus.love.common.enums.ActivityTypeEnum.LIKE, postId);
    }

    @Transactional
    public void unlikePost(Long postId) {
        Long userId = CurrentUser.getId();
        feedLikeMapper.delete(
                new LambdaQueryWrapper<FeedLike>()
                        .eq(FeedLike::getPostId, postId)
                        .eq(FeedLike::getUserId, userId));

        FeedPost post = feedPostMapper.selectById(postId);
        if (post != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            feedPostMapper.updateById(post);
        }
    }

    @Transactional
    public void addComment(FeedCommentRequest request) {
        Long userId = CurrentUser.getId();
        FeedPost post = feedPostMapper.selectById(request.getPostId());
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);

        FeedComment comment = new FeedComment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        feedCommentMapper.insert(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        feedPostMapper.updateById(post);
    }

    /**
     * 获取单条帖子详情（含完整评论列表，按时间正序爬楼）
     */
    public FeedPostResponse getPostDetail(Long postId) {
        Long currentUserId = CurrentUser.getId();
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);
        return toResponse(post, currentUserId, null, 200);
    }

    public void deletePost(Long postId) {
        Long userId = CurrentUser.getId();
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);

        // 检查权限：管理员可以删除所有帖子，普通用户只能删除自己的
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);

        boolean isAdmin = Boolean.TRUE.equals(currentUser.getIsAdmin());
        boolean isOwner = post.getUserId().equals(userId);

        if (!isAdmin && !isOwner) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        feedPostMapper.deleteById(postId);
    }

    /** 我发布的帖子收到的新点赞+新评论数量（自上次查看朋友圈活动以来），用于导航红点；从未查看过则返回 0 */
    public int getNewFeedActivityCount(Long userId) {
        User user = userMapper.selectById(userId);
        LocalDateTime since = user != null ? user.getLastFeedActivityViewedAt() : null;
        if (since == null) return 0;
        List<Long> myPostIds = feedPostMapper.selectList(
                        new LambdaQueryWrapper<FeedPost>().eq(FeedPost::getUserId, userId))
                .stream().map(FeedPost::getId).collect(Collectors.toList());
        if (myPostIds.isEmpty()) return 0;
        LambdaQueryWrapper<FeedLike> likeW = new LambdaQueryWrapper<FeedLike>().in(FeedLike::getPostId, myPostIds).gt(FeedLike::getCreatedAt, since);
        LambdaQueryWrapper<FeedComment> commentW = new LambdaQueryWrapper<FeedComment>().in(FeedComment::getPostId, myPostIds).gt(FeedComment::getCreatedAt, since);
        long likeCount = feedLikeMapper.selectCount(likeW);
        long commentCount = feedCommentMapper.selectCount(commentW);
        return (int) (likeCount + commentCount);
    }

    /** 标记朋友圈活动已查看，消除红点 */
    @Transactional
    public void markFeedActivityViewed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        user.setLastFeedActivityViewedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private FeedPostResponse toResponse(FeedPost post, Long currentUserId) {
        return toResponse(post, currentUserId, null, 10);
    }

    private FeedPostResponse toResponse(FeedPost post, Long currentUserId, Map<Long, User> authorMap) {
        return toResponse(post, currentUserId, authorMap, 10);
    }

    private FeedPostResponse toResponse(FeedPost post, Long currentUserId, Map<Long, User> authorMap, int commentLimit) {
        User author = authorMap != null ? authorMap.get(post.getUserId()) : null;
        if (author == null) {
            author = userMapper.selectById(post.getUserId());
        }
        boolean liked = feedLikeMapper.selectCount(
                new LambdaQueryWrapper<FeedLike>()
                        .eq(FeedLike::getPostId, post.getId())
                        .eq(FeedLike::getUserId, currentUserId)) > 0;

        List<FeedComment> comments = feedCommentMapper.selectList(
                new LambdaQueryWrapper<FeedComment>()
                        .eq(FeedComment::getPostId, post.getId())
                        .orderByAsc(FeedComment::getCreatedAt)
                        .last("LIMIT " + commentLimit));

        Map<Long, User> userCache = new HashMap<>();
        List<FeedPostResponse.CommentItem> commentItems = comments.stream().map(c -> {
            User u = userCache.computeIfAbsent(c.getUserId(), userMapper::selectById);
            return FeedPostResponse.CommentItem.builder()
                    .id(c.getId())
                    .userId(c.getUserId())
                    .nickname(u != null ? u.getNickname() : "")
                    .avatarUrl(u != null ? u.getAvatarUrl() : "")
                    .content(c.getContent())
                    .parentId(c.getParentId())
                    .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                    .build();
        }).collect(Collectors.toList());

        return FeedPostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .nickname(author != null ? author.getNickname() : "")
                .avatarUrl(author != null ? author.getAvatarUrl() : "")
                .content(post.getContent())
                .images(post.getImages())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(liked)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                .comments(commentItems)
                .build();
    }
}
