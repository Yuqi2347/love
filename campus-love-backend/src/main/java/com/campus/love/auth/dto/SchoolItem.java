package com.campus.love.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolItem {
    private String name;
    private String domain;
    /** 邮箱后缀规则，例如 szu.edu.cn 表示邮箱需以 @szu.edu.cn 或其子域名结尾 */
    private String emailSuffix;
    /**
     * 注册时可用的完整邮箱域名列表（不含 @）。若存在，前端可让用户只填学号再拼接域名。
     * 例如深圳大学：mails.szu.edu.cn、email.szu.edu.cn
     */
    private List<String> emailDomains;
    /** 学校可选校区列表 */
    private List<String> campuses;
}
