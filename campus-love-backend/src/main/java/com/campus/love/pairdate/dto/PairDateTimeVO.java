package com.campus.love.pairdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairDateTimeVO {
    private Long meetingTimestamp;
    private long serverTime;
}
