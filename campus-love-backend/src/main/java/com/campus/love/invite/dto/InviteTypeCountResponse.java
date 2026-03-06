package com.campus.love.invite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteTypeCountResponse {

    private String inviteType;
    private Long count;
}

