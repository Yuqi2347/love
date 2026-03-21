package com.campus.love.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarStudioGenerateResponse {

    /** PNG Base64（无 data: 前缀） */
    private String imageBase64;
    private String mimeType;
    /** 本次成功后的剩余次数 */
    private int remaining;
}
