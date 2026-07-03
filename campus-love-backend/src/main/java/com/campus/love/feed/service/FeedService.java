package com.campus.love.feed.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.love.auth.security.CurrentUser;
import com.campus.love.common.constants.UserLevelConstants;
import com.campus.love.common.exception.BusinessException;
import com.campus.love.common.result.ResultCode;
import com.campus.love.feed.constants.PostTypeConstants;
import com.campus.love.feed.constants.VisibilityConstants;
import com.campus.love.feed.dto.FeedCommentRequest;
import com.campus.love.feed.constants.FeedConstants;
import com.campus.love.feed.dto.InviteFeedCard;
import com.campus.love.feed.dto.FeedPostRequest;
import com.campus.love.feed.dto.FeedPostResponse;
import com.campus.love.feed.entity.FeedComment;
import com.campus.love.feed.entity.FeedCommentLike;
import com.campus.love.feed.entity.FeedLike;
import com.campus.love.feed.entity.FeedPost;
import com.campus.love.ai.skill.FeedTaggingSkill;
import com.campus.love.ai.rag.EmbeddingService;
import com.campus.love.tracking.BehaviorTracker;
import com.campus.love.tracking.mapper.UserBehaviorLogMapper;
import com.campus.love.feed.entity.FeedContentVector;
import com.campus.love.feed.mapper.FeedCommentLikeMapper;
import com.campus.love.feed.mapper.FeedCommentMapper;
import com.campus.love.feed.mapper.FeedContentVectorMapper;
import com.campus.love.feed.mapper.FeedLikeMapper;
import com.campus.love.feed.cache.DiscoveryFeedResultCache;
import com.campus.love.feed.mapper.FeedPostMapper;
import com.campus.love.feed.util.FeedImageThumbPaths;
import com.campus.love.follow.service.FollowService;
import com.campus.love.profile.entity.UserProfileVector;
import com.campus.love.profile.mapper.UserProfileVectorMapper;
import com.campus.love.notification.service.NotificationService;
import com.campus.love.invite.entity.Invite;
import com.campus.love.invite.mapper.InviteMapper;
import com.campus.love.invite.service.InviteQueryService;
import com.campus.love.user.entity.User;
import com.campus.love.user.mapper.UserMapper;
import com.campus.love.user.service.ActivityService;
import com.campus.love.chat.mapper.MessageMapper;
import com.campus.love.chat.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.campus.love.common.constants.DateTimeConstants;
import com.campus.love.common.enums.MsgTypeEnum;
import com.campus.love.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedPostMapper feedPostMapper;
    private final FeedLikeMapper feedLikeMapper;
    private final FeedCommentLikeMapper feedCommentLikeMapper;
    private final FeedTaggingSkill feedTaggingSkill;
    private final FeedCommentMapper feedCommentMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final ActivityService activityService;
    private final MessageMapper messageMapper;
    private final NotificationService notificationService;
    private final BehaviorTracker behaviorTracker;
    private final UserBehaviorLogMapper userBehaviorLogMapper;
    private final FeedContentVectorMapper feedContentVectorMapper;
    private final EmbeddingService embeddingService;
    private final UserProfileVectorMapper userProfileVectorMapper;
    private final InviteMapper inviteMapper;
    private final InviteQueryService inviteQueryService;
    private final DiscoveryFeedResultCache discoveryFeedResultCache;

    @Value("${app.feed.discovery-timing-log:false}")
    private boolean discoveryTimingLog;

    /**
     * 创建朋友圈帖子
     */
    public FeedPostResponse createPost(FeedPostRequest request) {
        return createPostInternal(request, PostTypeConstants.TIMELINE);
    }

    /**
     * 创建发现模块帖子（无等级限制）
     */
    public FeedPostResponse createDiscoveryPost(FeedPostRequest request) {
        return createPostInternal(request, PostTypeConstants.DISCOVERY);
    }

    private FeedPostResponse createPostInternal(FeedPostRequest request, String postType) {
        Long userId = CurrentUser.getId();
        assertPostImagesWithinLimit(request.getImages());
        FeedPost post = new FeedPost();
        post.setUserId(userId);
        post.setContent(request.getContent());
        post.setImages(request.getImages());
        post.setVideos(request.getVideos());
        post.setLinkUrl(null);
        post.setLinkTitle(null);
        post.setLinkImage(null);
        post.setInviteId(null);
        if (request.getInviteId() != null) {
            inviteQueryService.assertUserCanReferenceInviteInFeed(userId, request.getInviteId());
            post.setInviteId(request.getInviteId());
        }
        post.setPostType(postType);
        post.setVisibility(resolvePostVisibility(userId, request.getVisibility()));
        post.setRequiredLevel(UserLevelConstants.getMinLevel());
        post.setLikeCount(0);
        post.setCommentCount(0);
        feedPostMapper.insert(post);
        activityService.recordActivity(com.campus.love.common.enums.ActivityTypeEnum.POST, post.getId());
        log.info("用户{}发布动态: postId={}, postType={}", userId, post.getId(), postType);
        final Long pid = post.getId();
        Thread.ofVirtual().start(() -> {
            try { feedTaggingSkill.tagPost(pid); }
            catch (Exception e) { log.warn("Async tagging failed for post {}: {}", pid, e.getMessage()); }
        });
        return toResponse(post, userId);
    }

    /** 请求未带 visibility 时，用用户资料里的默认「谁可以看我的动态」 */
    private String resolvePostVisibility(Long userId, String requestVisibility) {
        if (requestVisibility != null && !requestVisibility.isBlank()) {
            return requestVisibility.trim();
        }
        User u = userMapper.selectById(userId);
        if (u != null && u.getFeedVisibility() != null && !u.getFeedVisibility().isBlank()) {
            return u.getFeedVisibility().trim();
        }
        return VisibilityConstants.ALL;
    }

    private void assertPostImagesWithinLimit(String images) {
        if (images == null || images.isBlank()) {
            return;
        }
        int n = 0;
        for (String part : images.split(",")) {
            if (!part.trim().isEmpty()) {
                n++;
            }
        }
        if (n > FeedConstants.POST_IMAGES_MAX) {
            throw new BusinessException(ResultCode.FEED_IMAGES_LIMIT);
        }
    }

    /**
     * 获取发现模块帖子列表
     * 只显示 DISCOVERY 类型的帖子
     * @param sort recommend=按兴趣推荐（默认）, hot=按热度, time=按时间
     * @param keyword 可选，按内容模糊搜索
     */
    public List<FeedPostResponse> getDiscoveryPosts(int page, int size, String sort, String keyword) {
        Long currentUserId = CurrentUser.getId();
        String sortNorm = sort == null ? "recommend" : sort.trim().toLowerCase(Locale.ROOT);
        String kw = keyword == null ? "" : keyword.trim();
        String cacheKey = (currentUserId != null ? currentUserId : 0L) + "|" + page + "|" + size + "|" + sortNorm + "|" + kw;
        return discoveryFeedResultCache.getOrCompute(cacheKey, k -> loadDiscoveryPostsWithTiming(page, size, sort, keyword, currentUserId));
    }

    private List<FeedPostResponse> loadDiscoveryPostsWithTiming(int page, int size, String sort, String keyword, Long currentUserId) {
        long t0 = System.nanoTime();
        boolean isRecommend = sort == null || "recommend".equalsIgnoreCase(sort);

        LambdaQueryWrapper<FeedPost> wrapper = new LambdaQueryWrapper<FeedPost>()
                .eq(FeedPost::getPostType, PostTypeConstants.DISCOVERY)
                .and(w -> w.eq(FeedPost::getVisibility, VisibilityConstants.ALL).or().isNull(FeedPost::getVisibility));
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(FeedPost::getContent, keyword.trim());
        }

        List<FeedPost> posts;
        long msSql;
        long msRank = 0;
        if (isRecommend) {
            int candidateSize = Math.max(size * 5, 60);
            wrapper.last("ORDER BY pinned_at IS NOT NULL DESC, COALESCE(created_at, FROM_UNIXTIME(0)) DESC, id DESC LIMIT " + candidateSize);
            long tSql0 = System.nanoTime();
            List<FeedPost> candidates = feedPostMapper.selectList(wrapper);
            long tSql1 = System.nanoTime();
            msSql = (tSql1 - tSql0) / 1_000_000L;
            long tRank0 = System.nanoTime();
            User currentUser = userMapper.selectById(currentUserId);
            posts = rankByMultiFactor(candidates, currentUser, page, size);
            msRank = (System.nanoTime() - tRank0) / 1_000_000L;
        } else {
            String orderClause = "hot".equalsIgnoreCase(sort)
                    ? "ORDER BY pinned_at IS NOT NULL DESC, (COALESCE(like_count,0) + COALESCE(comment_count,0)) DESC, COALESCE(created_at, FROM_UNIXTIME(0)) DESC, id DESC"
                    : "ORDER BY pinned_at IS NOT NULL DESC, COALESCE(created_at, FROM_UNIXTIME(0)) DESC, id DESC";
            long tSql0 = System.nanoTime();
            posts = feedPostMapper.selectList(wrapper.last(orderClause + " LIMIT " + (page * size) + "," + size));
            msSql = (System.nanoTime() - tSql0) / 1_000_000L;
        }

        long tEnrich0 = System.nanoTime();
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        Map<Long, InviteFeedCard> inviteCardMap = batchInviteCards(posts);
        List<FeedPostResponse> out = toResponseListPosts(posts, currentUserId, authorMap, null, inviteCardMap);
        long msEnrich = (System.nanoTime() - tEnrich0) / 1_000_000L;
        long msTotal = (System.nanoTime() - t0) / 1_000_000L;
        if (discoveryTimingLog) {
            log.info(
                    "discovery_timing userId={} page={} size={} sort={} recommend={} ms_total={} ms_sql_posts={} ms_rank={} ms_list_enrich={} postsOut={}",
                    currentUserId, page, size, sort == null ? "recommend" : sort, isRecommend,
                    msTotal, msSql, msRank, msEnrich, out.size());
        }
        return out;
    }

    /**
     * 多因子推荐打分排序（RAG 语义 + 关键词 + 热度 + 新鲜度 + 已看过降权）
     * 置顶帖始终排最前；其余按综合分 DESC 分页
     */
    private List<FeedPost> rankByMultiFactor(List<FeedPost> candidates, User user, int page, int size) {
        if (candidates.isEmpty()) return new ArrayList<>();

        Long userId = user != null ? user.getId() : null;

        // 1. 用户兴趣标签
        Set<String> userTags = new HashSet<>();
        if (user != null && user.getInterests() != null) {
            for (String t : user.getInterests().split(",")) {
                String trimmed = t.trim().toLowerCase();
                if (!trimmed.isEmpty()) userTags.add(trimmed);
            }
        }

        // 2. 近 7 天已看过的帖子 ID
        Set<Long> viewedIds = Collections.emptySet();
        if (userId != null) {
            try {
                List<Long> viewed = userBehaviorLogMapper.selectViewedTargetIds(
                        userId, "FEED_VIEW", LocalDateTime.now().minusDays(7), 500);
                viewedIds = new HashSet<>(viewed);
            } catch (Exception e) {
                log.debug("Load viewed post IDs failed: {}", e.getMessage());
            }
        }

        // 3. 用户画像向量（可选）
        float[] userVec = null;
        if (userId != null) {
            try {
                UserProfileVector pv = userProfileVectorMapper.selectById(userId);
                if (pv != null && pv.getProfileVector() != null) {
                    userVec = embeddingService.parseVector(pv.getProfileVector());
                }
            } catch (Exception e) {
                log.debug("Load user profile vector failed: {}", e.getMessage());
            }
        }

        // 4. 批量加载帖子内容向量
        Map<Long, FeedContentVector> vectorMap = new HashMap<>();
        try {
            List<Long> postIds = candidates.stream().map(FeedPost::getId).collect(Collectors.toList());
            List<FeedContentVector> vectors = feedContentVectorMapper.selectList(
                    new LambdaQueryWrapper<FeedContentVector>().in(FeedContentVector::getFeedId, postIds));
            if (vectors != null) {
                vectorMap = vectors.stream().collect(toMap(FeedContentVector::getFeedId, v -> v, (a, b) -> a));
            }
        } catch (Exception e) {
            log.debug("Load content vectors failed: {}", e.getMessage());
        }

        // 5. 最大热度归一化基准
        int maxHotness = candidates.stream()
                .mapToInt(p -> (p.getLikeCount() != null ? p.getLikeCount() : 0)
                               + (p.getCommentCount() != null ? p.getCommentCount() : 0))
                .max().orElse(1);
        if (maxHotness == 0) maxHotness = 1;

        // 6. 置顶与普通帖分离
        List<FeedPost> pinned = candidates.stream().filter(p -> p.getPinnedAt() != null).collect(Collectors.toList());
        List<FeedPost> normal = candidates.stream().filter(p -> p.getPinnedAt() == null).collect(Collectors.toList());

        final Set<Long> finalViewedIds = viewedIds;
        final float[] finalUserVec = userVec;
        final Map<Long, FeedContentVector> finalVectorMap = vectorMap;
        final int finalMaxHotness = maxHotness;
        final LocalDateTime now = LocalDateTime.now();

        normal.sort((a, b) -> Double.compare(
                computePostScore(b, userTags, finalViewedIds, finalUserVec, finalVectorMap, finalMaxHotness, now),
                computePostScore(a, userTags, finalViewedIds, finalUserVec, finalVectorMap, finalMaxHotness, now)));

        List<FeedPost> sorted = new ArrayList<>(pinned);
        sorted.addAll(normal);

        int from = page * size;
        int to = Math.min(from + size, sorted.size());
        return from >= sorted.size() ? new ArrayList<>() : sorted.subList(from, to);
    }

    private double computePostScore(FeedPost post, Set<String> userTags, Set<Long> viewedIds,
                                     float[] userVec, Map<Long, FeedContentVector> vectorMap,
                                     int maxHotness, LocalDateTime now) {
        // 画像匹配分（0~1）：有向量时用余弦相似度，否则关键词重叠
        double matchScore;
        FeedContentVector cv = vectorMap.get(post.getId());
        if (userVec != null && cv != null && cv.getContentVector() != null) {
            try {
                float[] postVec = embeddingService.parseVector(cv.getContentVector());
                matchScore = postVec != null ? embeddingService.cosineSimilarity(userVec, postVec) : keywordMatchScore(post, userTags);
            } catch (Exception e) {
                matchScore = keywordMatchScore(post, userTags);
            }
        } else {
            matchScore = keywordMatchScore(post, userTags);
        }

        // 热度分（0~1）
        int hotness = (post.getLikeCount() != null ? post.getLikeCount() : 0)
                      + (post.getCommentCount() != null ? post.getCommentCount() : 0);
        double hotnessScore = (double) hotness / maxHotness;

        // 新鲜度分（0~1，48h 半衰）
        double freshnessScore = 0.5;
        if (post.getCreatedAt() != null) {
            long hoursOld = ChronoUnit.HOURS.between(post.getCreatedAt(), now);
            freshnessScore = 1.0 / (1.0 + hoursOld / 48.0);
        } else {
            // created_at 为 null 时用 id 降序兜底
            freshnessScore = post.getId() != null ? Math.min(1.0, post.getId() / 1e9) : 0.5;
        }

        double total = matchScore * 0.40 + hotnessScore * 0.25 + freshnessScore * 0.15;

        // 已看过降权 70%（保留 30%，让用户有机会再次看到）
        if (viewedIds.contains(post.getId())) {
            total *= 0.30;
        }
        return total;
    }

    private double keywordMatchScore(FeedPost post, Set<String> userTags) {
        if (post.getAiTags() == null || userTags.isEmpty()) return 0.5;
        int matches = 0;
        int total = 0;
        for (String tag : post.getAiTags().split(",")) {
            total++;
            if (userTags.contains(tag.trim().toLowerCase())) matches++;
        }
        if (total == 0) return 0.5;
        return 0.5 + (double) matches / total * 0.5;
    }

    /**
     * 获取「关注」流：互相关注用户发布的<strong>朋友圈</strong>与<strong>探索动态</strong>（按每条帖子的 visibility 过滤）。
     * 作者个人资料里的「谁可以看我的动态」不直接改旧帖，仅作新发默认；时间范围仍用 feedVisibilityTime。
     * @param sort hot=按热度，time=按时间（默认）
     */
    public List<FeedPostResponse> getTimeline(int page, int size, String sort) {
        Long currentUserId = CurrentUser.getId();
        List<Long> mutualIds = followService.getMutualFollowIds(currentUserId);
        mutualIds.add(currentUserId);

        LambdaQueryWrapper<FeedPost> wrapper = new LambdaQueryWrapper<FeedPost>()
                .in(FeedPost::getUserId, mutualIds)
                .and(w -> w.eq(FeedPost::getPostType, PostTypeConstants.TIMELINE)
                        .or()
                        .eq(FeedPost::getPostType, PostTypeConstants.DISCOVERY));
        String orderClause = "hot".equalsIgnoreCase(sort)
                ? "ORDER BY pinned_at IS NOT NULL DESC, (COALESCE(like_count,0) + COALESCE(comment_count,0)) DESC, COALESCE(created_at, FROM_UNIXTIME(0)) DESC, id DESC"
                : "ORDER BY pinned_at IS NOT NULL DESC, COALESCE(created_at, FROM_UNIXTIME(0)) DESC, id DESC";
        List<FeedPost> posts = feedPostMapper.selectList(
                wrapper.last(orderClause + " LIMIT " + Math.min(300, (page + 1) * size * FeedConstants.TIMELINE_FETCH_MULTIPLIER))
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        List<FeedPost> filtered = posts.stream()
                .filter(p -> canSeePost(p, currentUserId, authorMap) && withinVisibilityTime(p, authorMap))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
        Map<Long, InviteFeedCard> inviteCardMap = batchInviteCards(filtered);
        return toResponseListPosts(filtered, currentUserId, authorMap, null, inviteCardMap);
    }

    /** 按帖子 visibility 判断当前用户是否可见（以帖子字段为准，兼容旧数据无 visibility 时视为 ALL） */
    private boolean canSeePost(FeedPost post, Long viewerId, Map<Long, User> authorMap) {
        if (post == null || viewerId == null) return false;
        Long authorId = post.getUserId();
        if (authorId.equals(viewerId)) return true;
        String vis = post.getVisibility();
        if (vis == null || vis.isEmpty()) vis = VisibilityConstants.ALL;
        if (VisibilityConstants.ALL.equals(vis)) return true;
        if (VisibilityConstants.SELF.equals(vis)) return false;
        if (VisibilityConstants.FOLLOWERS.equals(vis)) return followService.isFollowed(authorId, viewerId);
        if (VisibilityConstants.FOLLOWING.equals(vis)) return followService.isFollowed(viewerId, authorId);
        if (VisibilityConstants.FRIENDS.equals(vis)) return followService.isMutual(authorId, viewerId);
        return true;
    }

    /** 按作者 feedVisibilityTime 过滤：-1=全部，3/30/180=近N天 */
    private boolean withinVisibilityTime(FeedPost post, Map<Long, User> authorMap) {
        if (post == null || post.getCreatedAt() == null) return true;
        User author = authorMap != null ? authorMap.get(post.getUserId()) : null;
        Integer days = author != null ? author.getFeedVisibilityTime() : null;
        if (days == null || days <= 0) return true;
        LocalDateTime cutoff = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return !post.getCreatedAt().isBefore(cutoff);
    }

    public Map<String, Object> getUserPostsSummary(Long userId) {
        Long currentUserId = CurrentUser.getId();
        List<FeedPost> all = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, userId)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + FeedConstants.USER_POSTS_SUMMARY_LIMIT));
        Map<Long, User> authorMap = batchLoadAuthors(all);
        List<FeedPost> visible = all.stream()
                .filter(p -> canSeePost(p, currentUserId, authorMap) && withinVisibilityTime(p, authorMap))
                .toList();
        List<String> recentImageUrls = new ArrayList<>();
        for (FeedPost p : visible) {
            if (p.getImages() != null && !p.getImages().isEmpty()) {
                for (String img : p.getImages().split(",")) {
                    if (img != null && !img.trim().isEmpty() && recentImageUrls.size() < 3) {
                        recentImageUrls.add(img.trim());
                    }
                }
            }
        }
        return Map.of(
                "total", (long) visible.size(),
                "recentImageUrls", recentImageUrls
        );
    }

    public List<FeedPostResponse> getUserPosts(Long userId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, userId)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + ((page + 1) * size * FeedConstants.TIMELINE_FETCH_MULTIPLIER))
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        List<FeedPost> pagePosts = posts.stream()
                .filter(p -> canSeePost(p, currentUserId, authorMap) && withinVisibilityTime(p, authorMap))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
        Map<Long, InviteFeedCard> inviteCardMap = batchInviteCards(pagePosts);
        return toResponseListPosts(pagePosts, currentUserId, authorMap, null, inviteCardMap);
    }

    /**
     * 获取用户的朋友圈帖子（TIMELINE类型）
     * 按每条帖子的 visibility 过滤：ALL/FOLLOWERS/FRIENDS/SELF
     */
    public List<FeedPostResponse> getUserTimelinePosts(Long userId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, userId)
                        .eq(FeedPost::getPostType, PostTypeConstants.TIMELINE)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + ((page + 1) * size * FeedConstants.TIMELINE_FETCH_MULTIPLIER))
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        List<FeedPost> pagePosts = posts.stream()
                .filter(p -> canSeePost(p, currentUserId, authorMap) && withinVisibilityTime(p, authorMap))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
        Map<Long, InviteFeedCard> inviteCardMap = batchInviteCards(pagePosts);
        return toResponseListPosts(pagePosts, currentUserId, authorMap, null, inviteCardMap);
    }

    /**
     * 获取用户的发现模块帖子（DISCOVERY类型）
     * 按帖子 visibility 过滤，发现模块仅展示 visibility=ALL 的帖子
     */
    public List<FeedPostResponse> getUserDiscoveryPosts(Long userId, int page, int size) {
        Long currentUserId = CurrentUser.getId();
        List<FeedPost> posts = feedPostMapper.selectList(
                new LambdaQueryWrapper<FeedPost>()
                        .eq(FeedPost::getUserId, userId)
                        .eq(FeedPost::getPostType, PostTypeConstants.DISCOVERY)
                        .orderByDesc(FeedPost::getCreatedAt)
                        .last("LIMIT " + ((page + 1) * size * FeedConstants.TIMELINE_FETCH_MULTIPLIER))
        );
        Map<Long, User> authorMap = batchLoadAuthors(posts);
        List<FeedPost> pagePosts = posts.stream()
                .filter(p -> canSeePost(p, currentUserId, authorMap) && withinVisibilityTime(p, authorMap))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
        Map<Long, InviteFeedCard> inviteCardMap = batchInviteCards(pagePosts);
        return toResponseListPosts(pagePosts, currentUserId, authorMap, null, inviteCardMap);
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
        behaviorTracker.trackFeedLike(userId, postId);

        // V39：帖子点赞通知
        User liker = userMapper.selectById(userId);
        String likerNickname = liker != null ? liker.getNickname() : null;
        notificationService.notifyPostLike(post.getUserId(), userId, postId, likerNickname);

        // 探索列表缓存了带当前用户 liked/likeCount 的 DTO，点赞后必须失效，否则刷新仍见旧状态并触发「已点赞」等误报
        invalidateDiscoveryFeedCacheAfterCommit();
    }

    @Transactional
    public void likeComment(Long commentId) {
        Long userId = CurrentUser.getId();
        FeedComment comment = feedCommentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException(ResultCode.NOT_FOUND);

        Long count = feedCommentLikeMapper.selectCount(
                new LambdaQueryWrapper<FeedCommentLike>()
                        .eq(FeedCommentLike::getCommentId, commentId)
                        .eq(FeedCommentLike::getUserId, userId));
        if (count > 0) throw new BusinessException(ResultCode.ALREADY_LIKED_COMMENT);

        FeedCommentLike like = new FeedCommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        feedCommentLikeMapper.insert(like);

        int newCount = (comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1;
        comment.setLikeCount(newCount);
        feedCommentMapper.updateById(comment);

        FeedPost post = feedPostMapper.selectById(comment.getPostId());
        if (post != null) {
            User liker = userMapper.selectById(userId);
            String likerNickname = liker != null ? liker.getNickname() : null;
            notificationService.notifyCommentLike(comment.getUserId(), userId, post.getId(), commentId, likerNickname);
        }
    }

    @Transactional
    public void unlikeComment(Long commentId) {
        Long userId = CurrentUser.getId();
        feedCommentLikeMapper.delete(
                new LambdaQueryWrapper<FeedCommentLike>()
                        .eq(FeedCommentLike::getCommentId, commentId)
                        .eq(FeedCommentLike::getUserId, userId));

        FeedComment comment = feedCommentMapper.selectById(commentId);
        if (comment != null && comment.getLikeCount() != null && comment.getLikeCount() > 0) {
            comment.setLikeCount(comment.getLikeCount() - 1);
            feedCommentMapper.updateById(comment);
        }
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

        invalidateDiscoveryFeedCacheAfterCommit();
    }

    /**
     * 在事务提交后再清空探索流缓存，避免并发请求在提交前读到旧库并回填错误缓存。
     */
    private void invalidateDiscoveryFeedCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            discoveryFeedResultCache.invalidateAll();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                discoveryFeedResultCache.invalidateAll();
            }
        });
    }

    @Transactional
    public void addComment(FeedCommentRequest request) {
        Long userId = CurrentUser.getId();
        String content = request.getContent() != null ? request.getContent().trim() : "";
        String images = request.getImages() != null ? request.getImages().trim() : "";
        if ((content.isEmpty() && images.isEmpty())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容或图片不能为空");
        }
        FeedPost post = feedPostMapper.selectById(request.getPostId());
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);

        // 被回复用户ID：优先使用客户端传入的值（支持回复子回复场景），否则从parentId推导
        Long repliedUserId = request.getRepliedUserId();
        if (repliedUserId == null && request.getParentId() != null) {
            FeedComment parentComment = feedCommentMapper.selectById(request.getParentId());
            if (parentComment != null) {
                repliedUserId = parentComment.getUserId();
            }
        }

        FeedComment comment = new FeedComment();
        comment.setPostId(request.getPostId());
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setImages(images.isEmpty() ? null : images);
        comment.setParentId(request.getParentId());
        comment.setRepliedUserId(repliedUserId);
        feedCommentMapper.insert(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        feedPostMapper.updateById(post);

        User sender = userMapper.selectById(userId);
        String senderNickname = sender != null ? sender.getNickname() : null;
        String contentPreview = StringUtils.truncate(content, 50);

        if (repliedUserId != null && !repliedUserId.equals(userId)) {
            notificationService.notifyCommentReply(repliedUserId, userId, post.getId(), senderNickname, contentPreview);
        } else if (request.getParentId() == null && !post.getUserId().equals(userId)) {
            notificationService.notifyCommentOnPost(post.getUserId(), userId, post.getId(), senderNickname, contentPreview);
        }
    }

    /**
     * 获取单条帖子详情（含完整评论列表）
     * @param commentSort hot=按热度(like_count DESC, created_at DESC)，time=按时间(created_at ASC)
     * 按帖子 visibility 校验可见性
     */
    public FeedPostResponse getPostDetail(Long postId, String commentSort) {
        Long currentUserId = CurrentUser.getId();
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);
        if (!canSeePost(post, currentUserId, batchLoadAuthors(List.of(post)))) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        behaviorTracker.trackFeedView(currentUserId, postId);
        return toResponse(post, currentUserId, null, FeedConstants.DETAIL_COMMENT_LIMIT, commentSort, null);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Long currentUserId = CurrentUser.getId();
        FeedComment comment = feedCommentMapper.selectById(commentId);
        // 已删除或重复请求：幂等返回，避免接口返回 code=404 被前端当成失败（实际已删成功）
        if (comment == null) {
            return;
        }

        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        boolean isAdmin = Boolean.TRUE.equals(currentUser.getIsAdmin());
        boolean isOwner = comment.getUserId().equals(currentUserId);
        if (!isAdmin && !isOwner) throw new BusinessException(ResultCode.FORBIDDEN);

        FeedPost post = feedPostMapper.selectById(comment.getPostId());
        if (post == null) return;

        long subCount = feedCommentMapper.selectCount(
                new LambdaQueryWrapper<FeedComment>().eq(FeedComment::getParentId, commentId));
        int toDec = 1;
        if (subCount > 0) {
            comment.setEraseFlag(FeedComment.DELETED);
            comment.setContent("该评论已删除");
            comment.setImages(null);
            feedCommentMapper.updateById(comment);
        } else {
            feedCommentMapper.deleteById(commentId);
            Long parentId = comment.getParentId();
            if (parentId != null) {
                FeedComment parent = feedCommentMapper.selectById(parentId);
                if (parent != null && parent.getEraseFlag() != null && parent.getEraseFlag() == FeedComment.DELETED) {
                    long otherReplies = feedCommentMapper.selectCount(
                            new LambdaQueryWrapper<FeedComment>()
                                    .eq(FeedComment::getParentId, parentId)
                                    .ne(FeedComment::getId, commentId));
                    if (otherReplies == 0) {
                        feedCommentMapper.deleteById(parentId);
                        toDec = 2;
                    }
                }
            }
        }
        if (post.getCommentCount() != null && post.getCommentCount() >= toDec) {
            post.setCommentCount(post.getCommentCount() - toDec);
            feedPostMapper.updateById(post);
        }
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

    /** V39：置顶帖子（仅管理员） */
    @Transactional
    public void pinPost(Long postId) {
        Long userId = CurrentUser.getId();
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null || !Boolean.TRUE.equals(currentUser.getIsAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);
        post.setPinnedAt(LocalDateTime.now());
        post.setPinnedBy(userId);
        feedPostMapper.updateById(post);
        discoveryFeedResultCache.invalidateAll();
    }

    /** V39：取消置顶（仅管理员）—— 必须用 LambdaUpdateWrapper 显式写 NULL，updateById 不会更新 null 字段 */
    @Transactional
    public void unpinPost(Long postId) {
        Long userId = CurrentUser.getId();
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null || !Boolean.TRUE.equals(currentUser.getIsAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);
        feedPostMapper.update(null, new LambdaUpdateWrapper<FeedPost>()
                .eq(FeedPost::getId, postId)
                .set(FeedPost::getPinnedAt, null)
                .set(FeedPost::getPinnedBy, null));
        discoveryFeedResultCache.invalidateAll();
    }

    /** 重新生成 AI 标签（作者可触发，用于无标签或需刷新的帖子） */
    public void retagPost(Long postId) {
        Long userId = CurrentUser.getId();
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);
        if (!post.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        feedTaggingSkill.tagPost(postId);
    }

    /** 更新帖子 AI 标签（仅作者可修改） */
    public void updatePostAiTags(Long postId, String aiTags) {
        Long userId = CurrentUser.getId();
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) throw new BusinessException(ResultCode.FEED_NOT_FOUND);
        if (!post.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        post.setAiTags(aiTags != null ? aiTags.trim() : null);
        feedPostMapper.updateById(post);
    }

    /** 我发布的帖子/评论收到的新点赞+新评论数量（自上次查看以来），用于导航红点；从未查看过则返回 0 */
    public int getNewFeedActivityCount(Long userId) {
        User user = userMapper.selectById(userId);
        LocalDateTime since = user != null ? user.getLastFeedActivityViewedAt() : null;
        if (since == null) return 0;
        List<Long> myPostIds = feedPostMapper.selectList(
                        new LambdaQueryWrapper<FeedPost>()
                                .eq(FeedPost::getUserId, userId)
                                .select(FeedPost::getId))
                .stream()
                .map(FeedPost::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        long likeCount = 0;
        long commentCount = 0;
        if (!myPostIds.isEmpty()) {
            // 排除自己给自己点赞
            LambdaQueryWrapper<FeedLike> likeW = new LambdaQueryWrapper<FeedLike>()
                    .in(FeedLike::getPostId, myPostIds)
                    .ne(FeedLike::getUserId, userId)
                    .gt(FeedLike::getCreatedAt, since);
            // 排除自己评论自己的动态
            LambdaQueryWrapper<FeedComment> commentW = new LambdaQueryWrapper<FeedComment>()
                    .in(FeedComment::getPostId, myPostIds)
                    .ne(FeedComment::getUserId, userId)
                    .gt(FeedComment::getCreatedAt, since)
                    .and(w -> w.isNull(FeedComment::getEraseFlag).or().ne(FeedComment::getEraseFlag, FeedComment.DELETED));
            likeCount = feedLikeMapper.selectCount(likeW);
            commentCount = feedCommentMapper.selectCount(commentW);
        }

        // V39: 统计评论被点赞数（来自 t_notification COMMENT_LIKE）
        long commentLikeCount = notificationService.countCommentLikesSince(userId, since);

        return (int) (likeCount + commentCount + commentLikeCount);
    }

    /** 标记朋友圈活动已查看，消除红点 */
    @Transactional
    public void markFeedActivityViewed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        user.setLastFeedActivityViewedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 获取社交通知（点赞、评论、@提及）
     */
    public List<Map<String, Object>> getSocialNotifications(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<Long> myPostIds = feedPostMapper.selectList(
                        new LambdaQueryWrapper<FeedPost>()
                                .eq(FeedPost::getUserId, userId)
                                .select(FeedPost::getId))
                .stream()
                .map(FeedPost::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<FeedLike> likes = List.of();
        if (!myPostIds.isEmpty()) {
            likes = feedLikeMapper.selectList(
                    new LambdaQueryWrapper<FeedLike>()
                            .in(FeedLike::getPostId, myPostIds)
                            .ne(FeedLike::getUserId, userId)
                            .orderByDesc(FeedLike::getCreatedAt)
                            .last("LIMIT " + FeedConstants.SOCIAL_NOTIFICATION_LIMIT));
        }

        LambdaQueryWrapper<FeedComment> commentWrapper = new LambdaQueryWrapper<FeedComment>()
                .ne(FeedComment::getUserId, userId)
                .and(w -> w.isNull(FeedComment::getEraseFlag).or().ne(FeedComment::getEraseFlag, FeedComment.DELETED));
        if (!myPostIds.isEmpty()) {
            commentWrapper.and(w -> w.in(FeedComment::getPostId, myPostIds).or().eq(FeedComment::getRepliedUserId, userId));
        } else {
            commentWrapper.eq(FeedComment::getRepliedUserId, userId);
        }
        List<FeedComment> comments = feedCommentMapper.selectList(
                commentWrapper
                        .orderByDesc(FeedComment::getCreatedAt)
                        .last("LIMIT " + FeedConstants.SOCIAL_NOTIFICATION_LIMIT));

        List<com.campus.love.notification.entity.Notification> commentLikeNotifs =
                notificationService.getCommentLikeNotifications(userId, FeedConstants.SOCIAL_NOTIFICATION_LIMIT);

        Set<Long> senderIds = new HashSet<>();
        for (FeedLike like : likes) {
            if (like.getUserId() != null) {
                senderIds.add(like.getUserId());
            }
        }
        for (FeedComment c : comments) {
            if (c.getUserId() != null) {
                senderIds.add(c.getUserId());
            }
        }
        for (com.campus.love.notification.entity.Notification n : commentLikeNotifs) {
            if (n.getSenderId() != null) {
                senderIds.add(n.getSenderId());
            }
        }
        Map<Long, User> senderMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(senderIds);
            if (users != null) {
                for (User u : users) {
                    if (u != null) {
                        senderMap.put(u.getId(), u);
                    }
                }
            }
        }

        for (FeedLike like : likes) {
            User sender = senderMap.get(like.getUserId());
            if (sender == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", like.getId());
            item.put("senderId", sender.getId());
            item.put("senderNickname", sender.getNickname());
            item.put("senderAvatarUrl", sender.getAvatarUrl());
            item.put("type", "LIKE");
            item.put("content", "赞了你的动态");
            item.put("postId", like.getPostId());
            item.put("createdAt", like.getCreatedAt() != null ? like.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "");
            result.add(item);
        }

        for (FeedComment comment : comments) {
            User sender = senderMap.get(comment.getUserId());
            if (sender == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", comment.getId() + 100000);
            item.put("senderId", sender.getId());
            item.put("senderNickname", sender.getNickname());
            item.put("senderAvatarUrl", sender.getAvatarUrl());
            boolean isMention = comment.getRepliedUserId() != null && comment.getRepliedUserId().equals(userId);
            item.put("type", isMention ? "MENTION" : "COMMENT");
            String prefix = isMention ? "在评论中@了你: " : "评论了你的动态: ";
            String content = StringUtils.truncate(comment.getContent(), FeedConstants.NOTIFICATION_CONTENT_PREVIEW_LEN);
            item.put("content", prefix + content);
            item.put("postId", comment.getPostId());
            item.put("createdAt", comment.getCreatedAt() != null ? comment.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "");
            result.add(item);
        }

        for (com.campus.love.notification.entity.Notification n : commentLikeNotifs) {
            User sender = senderMap.get(n.getSenderId());
            if (sender == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", -(n.getId()));
            item.put("senderId", sender.getId());
            item.put("senderNickname", sender.getNickname());
            item.put("senderAvatarUrl", sender.getAvatarUrl());
            item.put("type", "COMMENT_LIKE");
            item.put("content", "赞了你的评论");
            item.put("postId", n.getPostId());
            item.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "");
            result.add(item);
        }

        result.sort((a, b) -> String.valueOf(b.get("createdAt")).compareTo(String.valueOf(a.get("createdAt"))));
        return result;
    }

    /**
     * 详情页评论：先按排序取一批根评论，再拉取候选子回复并归属到这些根下，避免「按时间 LIMIT 200 条扁平行」导致老主楼丢失、头像/楼层错乱。
     */
    private List<FeedComment> loadCommentsForPostDetail(Long postId, String commentSort) {
        String sort = (commentSort == null || commentSort.isBlank()) ? "time" : commentSort;
        LambdaQueryWrapper<FeedComment> rootWrapper = new LambdaQueryWrapper<FeedComment>()
                .eq(FeedComment::getPostId, postId)
                .isNull(FeedComment::getParentId);
        if ("hot".equalsIgnoreCase(sort)) {
            rootWrapper.last("ORDER BY COALESCE(like_count,0) DESC, id DESC LIMIT " + FeedConstants.DETAIL_COMMENT_ROOT_LIMIT);
        } else {
            rootWrapper.last("ORDER BY COALESCE(created_at, FROM_UNIXTIME(0)) DESC, id DESC LIMIT " + FeedConstants.DETAIL_COMMENT_ROOT_LIMIT);
        }
        List<FeedComment> roots = feedCommentMapper.selectList(rootWrapper);
        if (roots.isEmpty()) {
            return List.of();
        }
        Set<Long> allowedRootIds = roots.stream().map(FeedComment::getId).collect(Collectors.toSet());

        LambdaQueryWrapper<FeedComment> repWrapper = new LambdaQueryWrapper<FeedComment>()
                .eq(FeedComment::getPostId, postId)
                .isNotNull(FeedComment::getParentId)
                .last("ORDER BY COALESCE(created_at, FROM_UNIXTIME(0)) DESC, id DESC LIMIT " + FeedConstants.DETAIL_COMMENT_REPLY_FETCH_LIMIT);
        List<FeedComment> candidateReplies = feedCommentMapper.selectList(repWrapper);

        Map<Long, FeedComment> byId = new HashMap<>();
        for (FeedComment r : roots) {
            byId.put(r.getId(), r);
        }
        for (FeedComment c : candidateReplies) {
            byId.put(c.getId(), c);
        }
        expandMissingCommentParents(byId);

        List<FeedComment> filtered = new ArrayList<>();
        for (FeedComment c : candidateReplies) {
            Long rootId = ultimateCommentRootId(c, byId);
            if (rootId != null && allowedRootIds.contains(rootId)) {
                filtered.add(c);
            }
        }
        filtered.sort(Comparator.comparing(FeedComment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        List<FeedComment> merged = new ArrayList<>(roots.size() + filtered.size());
        merged.addAll(roots);
        merged.addAll(filtered);
        return merged;
    }

    /**
     * 业务软删的评论：仅当数据库里仍存在直接子评论时才返回（用于占位「该评论已删除」）。
     * 无子评论的软删行不入列表，避免占楼层、占位置（与 deleteComment 物理删叶子一致，并兼容历史脏数据）。
     */
    private List<FeedComment> dropErasedCommentsWithoutReplies(List<FeedComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return comments;
        }
        List<Long> erasedIds = comments.stream()
                .filter(c -> c.getEraseFlag() != null && c.getEraseFlag() == FeedComment.DELETED)
                .map(FeedComment::getId)
                .distinct()
                .collect(Collectors.toList());
        if (erasedIds.isEmpty()) {
            return comments;
        }
        List<FeedComment> rowsWithErasedParent = feedCommentMapper.selectList(
                new LambdaQueryWrapper<FeedComment>().in(FeedComment::getParentId, erasedIds));
        Set<Long> erasedIdsHavingChild = rowsWithErasedParent.stream()
                .map(FeedComment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return comments.stream()
                .filter(c -> {
                    if (c.getEraseFlag() == null || c.getEraseFlag() != FeedComment.DELETED) {
                        return true;
                    }
                    return erasedIdsHavingChild.contains(c.getId());
                })
                .collect(Collectors.toList());
    }

    /** 补齐链式回复的中间节点，便于向上追溯到根评论 */
    private void expandMissingCommentParents(Map<Long, FeedComment> byId) {
        for (int pass = 0; pass < 8; pass++) {
            Set<Long> missing = new HashSet<>();
            for (FeedComment c : new ArrayList<>(byId.values())) {
                Long pid = c.getParentId();
                if (pid == null) {
                    continue;
                }
                if (!byId.containsKey(pid)) {
                    missing.add(pid);
                }
            }
            if (missing.isEmpty()) {
                return;
            }
            List<FeedComment> more = feedCommentMapper.selectList(
                    new LambdaQueryWrapper<FeedComment>().in(FeedComment::getId, missing));
            if (more.isEmpty()) {
                return;
            }
            for (FeedComment m : more) {
                byId.putIfAbsent(m.getId(), m);
            }
        }
    }

    private Long ultimateCommentRootId(FeedComment start, Map<Long, FeedComment> byId) {
        FeedComment cur = start;
        int depth = 0;
        while (cur != null && cur.getParentId() != null && depth++ < 48) {
            FeedComment p = byId.get(cur.getParentId());
            if (p == null) {
                return null;
            }
            cur = p;
        }
        if (cur == null || cur.getParentId() != null) {
            return null;
        }
        return cur.getId();
    }

    /** 当前用户对一批帖子的点赞（1 次查询） */
    private Set<Long> batchLoadLikedPostIds(Long userId, Collection<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Set.of();
        }
        List<FeedLike> rows = feedLikeMapper.selectList(
                new LambdaQueryWrapper<FeedLike>()
                        .eq(FeedLike::getUserId, userId)
                        .in(FeedLike::getPostId, postIds));
        return rows.stream().map(FeedLike::getPostId).collect(Collectors.toSet());
    }

    /** 当前用户对一批评论的点赞（1 次查询） */
    private Set<Long> batchLoadLikedCommentIds(Long userId, Collection<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return Set.of();
        }
        List<FeedCommentLike> rows = feedCommentLikeMapper.selectList(
                new LambdaQueryWrapper<FeedCommentLike>()
                        .eq(FeedCommentLike::getUserId, userId)
                        .in(FeedCommentLike::getCommentId, commentIds));
        return rows.stream().map(FeedCommentLike::getCommentId).collect(Collectors.toSet());
    }

    /** 列表页：多帖评论一次 SQL（窗口函数），再按 postId 分组 */
    private Map<Long, List<FeedComment>> loadCommentsForListPageBatch(List<Long> postIds, int commentLimit, String commentSort) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        boolean sortHot = "hot".equalsIgnoreCase(commentSort);
        List<FeedComment> rows = feedCommentMapper.selectTopNPerPostForList(postIds, commentLimit, sortHot);
        Map<Long, List<FeedComment>> map = new LinkedHashMap<>();
        for (Long pid : postIds) {
            map.put(pid, new ArrayList<>());
        }
        if (rows != null) {
            for (FeedComment c : rows) {
                if (c.getPostId() != null) {
                    map.computeIfAbsent(c.getPostId(), k -> new ArrayList<>()).add(c);
                }
            }
        }
        return map;
    }

    private Map<Long, User> loadUsersForComments(List<FeedComment> comments, Map<Long, User> authorMap) {
        Set<Long> need = new HashSet<>();
        for (FeedComment c : comments) {
            if (c.getUserId() != null) {
                need.add(c.getUserId());
            }
            if (c.getRepliedUserId() != null) {
                need.add(c.getRepliedUserId());
            }
        }
        Map<Long, User> out = new HashMap<>();
        Set<Long> missing = new HashSet<>();
        for (Long uid : need) {
            User fromAuthor = authorMap != null ? authorMap.get(uid) : null;
            if (fromAuthor != null) {
                out.put(uid, fromAuthor);
            } else {
                missing.add(uid);
            }
        }
        if (!missing.isEmpty()) {
            List<User> loaded = userMapper.selectBatchIds(missing);
            if (loaded != null) {
                for (User u : loaded) {
                    if (u != null) {
                        out.put(u.getId(), u);
                    }
                }
            }
        }
        return out;
    }

    private List<FeedPostResponse.CommentItem> buildCommentItems(
            List<FeedComment> comments,
            Set<Long> likedCommentIds,
            Map<Long, User> userById
    ) {
        List<FeedPostResponse.CommentItem> items = new ArrayList<>(comments.size());
        for (FeedComment c : comments) {
            User u = userById.get(c.getUserId());
            String repliedToName = null;
            if (c.getRepliedUserId() != null) {
                User repliedUser = userById.get(c.getRepliedUserId());
                repliedToName = repliedUser != null ? repliedUser.getNickname() : null;
            }
            boolean isDeleted = c.getEraseFlag() != null && c.getEraseFlag() == FeedComment.DELETED;
            boolean commentLiked = likedCommentIds.contains(c.getId());
            items.add(FeedPostResponse.CommentItem.builder()
                    .id(c.getId())
                    .userId(c.getUserId())
                    .nickname(u != null ? u.getNickname() : "")
                    .avatarUrl(u != null ? u.getAvatarUrl() : "")
                    .content(isDeleted ? "该评论已删除" : c.getContent())
                    .images(c.getImages())
                    .parentId(c.getParentId())
                    .repliedToName(repliedToName)
                    .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                    .deleted(isDeleted)
                    .likeCount(c.getLikeCount() != null ? c.getLikeCount() : 0)
                    .liked(commentLiked)
                    .build());
        }
        return items;
    }

    private InviteFeedCard resolveInviteCard(FeedPost post, Map<Long, InviteFeedCard> inviteCardMap) {
        if (post.getInviteId() == null) {
            return null;
        }
        if (inviteCardMap != null) {
            InviteFeedCard c = inviteCardMap.get(post.getInviteId());
            if (c != null) {
                return c;
            }
        }
        return loadInviteFeedCard(post.getInviteId());
    }

    private FeedPostResponse buildFeedPostResponseCore(
            FeedPost post,
            User author,
            boolean liked,
            List<FeedPostResponse.CommentItem> commentItems,
            InviteFeedCard inviteCard,
            String imageThumbs
    ) {
        return FeedPostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .nickname(author != null ? author.getNickname() : "")
                .avatarUrl(author != null ? author.getAvatarUrl() : "")
                .content(post.getContent())
                .images(post.getImages())
                .imageThumbs(imageThumbs)
                .videos(post.getVideos())
                .linkUrl(null)
                .linkTitle(null)
                .linkImage(null)
                .inviteId(post.getInviteId())
                .inviteCard(inviteCard)
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .liked(liked)
                .visibility(post.getVisibility() != null ? post.getVisibility() : VisibilityConstants.ALL)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : "")
                .comments(commentItems)
                .aiTags(post.getAiTags())
                .pinned(post.getPinnedAt() != null)
                .pinnedAt(post.getPinnedAt() != null ? post.getPinnedAt().format(DateTimeConstants.DATETIME_FMT) : null)
                .build();
    }

    /**
     * 列表路径批量组装：每页 2 次点赞查询 + 1 次批量评论（窗口函数）+ 评论用户批量加载。
     */
    private List<FeedPostResponse> toResponseListPosts(
            List<FeedPost> posts,
            Long currentUserId,
            Map<Long, User> authorMap,
            String commentSort,
            Map<Long, InviteFeedCard> inviteCardMap
    ) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = posts.stream().map(FeedPost::getId).filter(Objects::nonNull).toList();
        Set<Long> likedPostIds = batchLoadLikedPostIds(currentUserId, postIds);
        Map<Long, List<FeedComment>> loadedByPost = loadCommentsForListPageBatch(postIds, FeedConstants.LIST_COMMENT_LIMIT, commentSort);
        Map<Long, List<FeedComment>> commentsByPost = new LinkedHashMap<>();
        List<FeedComment> allComments = new ArrayList<>();
        for (FeedPost p : posts) {
            List<FeedComment> cs = new ArrayList<>(loadedByPost.getOrDefault(p.getId(), List.of()));
            cs = dropErasedCommentsWithoutReplies(cs);
            commentsByPost.put(p.getId(), cs);
            allComments.addAll(cs);
        }
        Set<Long> likedCommentIds = batchLoadLikedCommentIds(
                currentUserId,
                allComments.stream().map(FeedComment::getId).collect(Collectors.toList()));
        Map<Long, User> authors = authorMap == null ? new HashMap<>() : new HashMap<>(authorMap);
        Set<Long> missingAuthorIds = posts.stream()
                .map(FeedPost::getUserId)
                .filter(Objects::nonNull)
                .filter(uid -> !authors.containsKey(uid))
                .collect(Collectors.toSet());
        if (!missingAuthorIds.isEmpty()) {
            List<User> fill = userMapper.selectBatchIds(missingAuthorIds);
            if (fill != null) {
                for (User u : fill) {
                    if (u != null) {
                        authors.put(u.getId(), u);
                    }
                }
            }
        }
        Map<Long, User> userById = loadUsersForComments(allComments, authors);
        List<FeedPostResponse> out = new ArrayList<>(posts.size());
        for (FeedPost post : posts) {
            User author = authors.get(post.getUserId());
            List<FeedPostResponse.CommentItem> items = buildCommentItems(
                    commentsByPost.getOrDefault(post.getId(), List.of()),
                    likedCommentIds,
                    userById);
            InviteFeedCard inviteCard = resolveInviteCard(post, inviteCardMap);
            String thumbs = FeedImageThumbPaths.buildImageThumbsCsv(post.getImages());
            out.add(buildFeedPostResponseCore(
                    post,
                    author,
                    likedPostIds.contains(post.getId()),
                    items,
                    inviteCard,
                    thumbs));
        }
        return out;
    }

    private FeedPostResponse toResponsePostDetail(
            FeedPost post,
            Long currentUserId,
            Map<Long, User> authorMap,
            String commentSort,
            Map<Long, InviteFeedCard> inviteCardMap
    ) {
        User author = authorMap != null ? authorMap.get(post.getUserId()) : null;
        if (author == null) {
            author = userMapper.selectById(post.getUserId());
        }
        boolean liked = batchLoadLikedPostIds(currentUserId, List.of(post.getId())).contains(post.getId());
        List<FeedComment> comments = loadCommentsForPostDetail(post.getId(), commentSort);
        comments = dropErasedCommentsWithoutReplies(comments);
        Set<Long> likedCommentIds = batchLoadLikedCommentIds(
                currentUserId,
                comments.stream().map(FeedComment::getId).collect(Collectors.toList()));
        Map<Long, User> userById = loadUsersForComments(comments, authorMap);
        List<FeedPostResponse.CommentItem> commentItems = buildCommentItems(comments, likedCommentIds, userById);
        InviteFeedCard inviteCard = resolveInviteCard(post, inviteCardMap);
        String thumbs = FeedImageThumbPaths.buildImageThumbsCsv(post.getImages());
        return buildFeedPostResponseCore(post, author, liked, commentItems, inviteCard, thumbs);
    }

    private FeedPostResponse toResponse(FeedPost post, Long currentUserId) {
        return toResponse(post, currentUserId, null, FeedConstants.LIST_COMMENT_LIMIT, null, null);
    }

    private FeedPostResponse toResponse(FeedPost post, Long currentUserId, Map<Long, User> authorMap) {
        return toResponse(post, currentUserId, authorMap, FeedConstants.LIST_COMMENT_LIMIT, null, null);
    }

    private FeedPostResponse toResponse(FeedPost post, Long currentUserId, Map<Long, User> authorMap, int commentLimit) {
        return toResponse(post, currentUserId, authorMap, commentLimit, null, null);
    }

    private FeedPostResponse toResponse(FeedPost post, Long currentUserId, Map<Long, User> authorMap, int commentLimit, String commentSort, Map<Long, InviteFeedCard> inviteCardMap) {
        if (commentLimit == FeedConstants.DETAIL_COMMENT_LIMIT) {
            return toResponsePostDetail(post, currentUserId, authorMap, commentSort, inviteCardMap);
        }
        return toResponseListPosts(List.of(post), currentUserId, authorMap, commentSort, inviteCardMap).get(0);
    }

    /**
     * 分享帖子给朋友（批量发送给多个互关朋友）
     * @param postId 帖子ID
     * @param receiverIds 接收人ID列表
     */
    @Transactional
    public void sharePost(Long postId, List<Long> receiverIds) {
        Long senderId = CurrentUser.getId();

        // 获取帖子信息
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.FEED_NOT_FOUND);
        }

        // 获取帖子作者信息
        User postAuthor = userMapper.selectById(post.getUserId());
        String postNickname = postAuthor != null ? postAuthor.getNickname() : "";

        // 构建帖子转发内容（JSON格式）
        String postContent = post.getContent();
        if (postContent != null && postContent.length() > 100) {
            postContent = postContent.substring(0, 100) + "...";
        }

        String shareContent = String.format(
                "{\"postId\":%d,\"postUserId\":%d,\"postNickname\":\"%s\",\"postContent\":\"%s\",\"postImages\":\"%s\",\"postCreatedAt\":\"%s\"}",
                postId,
                post.getUserId(),
                postNickname != null ? postNickname.replace("\"", "\\\"") : "",
                postContent != null ? postContent.replace("\"", "\\\"").replace("\n", " ") : "",
                post.getImages() != null ? post.getImages().replace("\"", "\\\"") : "",
                post.getCreatedAt() != null ? post.getCreatedAt().format(DateTimeConstants.DATETIME_FMT) : ""
        );

        // 批量发送给每个朋友
        for (Long receiverId : receiverIds) {
            Message message = new Message();
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setGroupId(null);
            message.setContent(shareContent);
            message.setMsgType(MsgTypeEnum.POST_SHARE.getCode());
            message.setIsRead(false);
            message.setCreatedAt(java.time.LocalDateTime.now());
            messageMapper.insert(message);
        }

        log.info("用户{}分享帖子{}给{}位朋友", senderId, postId, receiverIds.size());
    }

    /**
     * 获取当前用户点赞的帖子列表
     */
    public List<FeedPostResponse> getLikedPosts(int page, int size) {
        Long userId = CurrentUser.getId();

        // 获取用户点赞的帖子ID列表
        List<FeedLike> likes = feedLikeMapper.selectList(
                new LambdaQueryWrapper<FeedLike>()
                        .eq(FeedLike::getUserId, userId)
                        .orderByDesc(FeedLike::getCreatedAt));

        if (likes.isEmpty()) {
            return new ArrayList<>();
        }

        // 分页
        int fromIndex = page * size;
        if (fromIndex >= likes.size()) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + size, likes.size());
        List<Long> postIds = likes.subList(fromIndex, toIndex).stream()
                .map(FeedLike::getPostId)
                .collect(Collectors.toList());

        // 获取帖子详情
        List<FeedPost> posts = feedPostMapper.selectBatchIds(postIds);
        // 按点赞时间排序
        Map<Long, FeedPost> postMap = posts.stream()
                .collect(Collectors.toMap(FeedPost::getId, p -> p, (a, b) -> a));
        List<FeedPost> orderedPosts = new ArrayList<>();
        for (Long postId : postIds) {
            FeedPost post = postMap.get(postId);
            if (post != null) {
                orderedPosts.add(post);
            }
        }

        Map<Long, User> authorMap = batchLoadAuthors(orderedPosts);
        Map<Long, InviteFeedCard> inviteCardMap = batchInviteCards(orderedPosts);
        return toResponseListPosts(orderedPosts, userId, authorMap, null, inviteCardMap);
    }

    private Map<Long, InviteFeedCard> batchInviteCards(List<FeedPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = posts.stream().map(FeedPost::getInviteId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Invite> invites = inviteMapper.selectList(
                new LambdaQueryWrapper<Invite>().in(Invite::getId, ids));
        if (invites.isEmpty()) {
            return Map.of();
        }
        List<Long> creatorIds = invites.stream().map(Invite::getCreatorId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<User> creators = creatorIds.isEmpty() ? List.of() : userMapper.selectBatchIds(creatorIds);
        Map<Long, User> creatorMap = creators != null
                ? creators.stream().filter(Objects::nonNull).collect(toMap(User::getId, u -> u, (a, b) -> a))
                : Map.of();
        Map<Long, InviteFeedCard> map = new HashMap<>();
        for (Invite inv : invites) {
            if (inv == null || Boolean.TRUE.equals(inv.getDeleted())) {
                continue;
            }
            User c = creatorMap.get(inv.getCreatorId());
            map.put(inv.getId(), toInviteFeedCard(inv, c));
        }
        return map;
    }

    private InviteFeedCard toInviteFeedCard(Invite inv, User creator) {
        if (inv == null) {
            return null;
        }
        return InviteFeedCard.builder()
                .id(inv.getId())
                .title(inv.getTitle())
                .inviteType(inv.getInviteType())
                .status(inv.getStatus())
                .inviteTime(inv.getInviteTime() != null ? inv.getInviteTime().format(DateTimeConstants.DATETIME_FMT) : null)
                .inviteEndTime(inv.getInviteEndTime() != null ? inv.getInviteEndTime().format(DateTimeConstants.DATETIME_FMT) : null)
                .location(inv.getLocation())
                .participantCount(inv.getParticipantCount())
                .maxParticipants(inv.getMaxParticipants())
                .creatorId(inv.getCreatorId())
                .creatorNickname(creator != null ? creator.getNickname() : null)
                .creatorAvatarUrl(creator != null ? creator.getAvatarUrl() : null)
                .build();
    }

    private InviteFeedCard loadInviteFeedCard(Long inviteId) {
        if (inviteId == null) {
            return null;
        }
        Invite inv = inviteMapper.selectById(inviteId);
        if (inv == null || Boolean.TRUE.equals(inv.getDeleted())) {
            return null;
        }
        User creator = userMapper.selectById(inv.getCreatorId());
        return toInviteFeedCard(inv, creator);
    }
}
