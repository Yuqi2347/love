package com.campus.love.pairdate.enums;

/**
 * 与 t_pair_date_negotiation.status 一致（对齐 V1.2.0_INVITATION_MODULE 附录状态机）
 */
public enum PairDateStatus {
    PENDING,
    SIDE_A_DONE,
    SIDE_B_DONE,
    CALCULATING,
    COMPLETED,
    TIME_MISMATCH,
    EXPIRED
}
