package com.campus.love.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchItemResponse {

    private Long id;
    private String nickname;
    private String avatarUrl;
}

