package com.campus.love.common.constants;

/**
 * 邀约模块信用分与参与限制常量。
 */
public final class InviteCreditConstants {

    private InviteCreditConstants() {}

    /** 发起邀约所需最低信用分 */
    public static final int CREDIT_CREATE_THRESHOLD = 80;
    /** 参与邀约所需最低信用分 */
    public static final int CREDIT_JOIN_THRESHOLD = 60;
    /** 同时参与邀约上限 */
    public static final int MAX_CONCURRENT_PARTICIPATES = 2;
    /** 活动开始前 N 小时内取消/退出视为「临时取消」 */
    public static final int TEMP_CANCEL_HOURS = 1;
    /** 成功完成邀约加分 */
    public static final int CREDIT_SUCCESS_COMPLETE = 2;
    /** 临时取消扣分 */
    public static final int CREDIT_TEMP_CANCEL = -5;
    /** 放鸽子扣分 */
    public static final int CREDIT_NO_SHOW = -10;
    /** 默认信用分（新用户或未设置时） */
    public static final int DEFAULT_CREDIT_SCORE = 100;
    /** 信用分上限，达到后不再上升 */
    public static final int CREDIT_MAX = 100;
}
