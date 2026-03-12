package com.campus.love.auth.service;

import com.campus.love.auth.dto.SchoolItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SchoolService {

    private static final String SCHOOLS_FILE = "schools.json";
    private List<SchoolItem> allSchools = new ArrayList<>();

    @PostConstruct
    public void loadSchools() {
        try {
            ClassPathResource resource = new ClassPathResource(SCHOOLS_FILE);
            try (InputStream is = resource.getInputStream()) {
                ObjectMapper mapper = new ObjectMapper();
                allSchools = mapper.readValue(is, new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.warn("Failed to load schools.json, using empty list: {}", e.getMessage());
            allSchools = List.of();
        }
    }

    /**
     * 根据关键词搜索学校（支持模糊匹配，包含学校名称）
     */
    public List<SchoolItem> searchSchools(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String k = keyword.trim().toLowerCase();
        return allSchools.stream()
                .filter(s -> s.getName().toLowerCase().contains(k) || s.getDomain().toLowerCase().contains(k))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有支持的学校
     */
    public List<SchoolItem> getAllSchools() {
        return new ArrayList<>(allSchools);
    }

    /**
     * 根据域名校验邮箱是否属于支持的学校
     */
    public boolean isSupportedDomain(String domain) {
        if (domain == null) return false;
        return allSchools.stream()
                .anyMatch(s -> s.getDomain() != null && s.getDomain().equalsIgnoreCase(domain));
    }

    /**
     * 根据学校名称查找学校配置
     */
    public SchoolItem findSchoolByName(String schoolName) {
        if (schoolName == null || schoolName.isBlank()) return null;
        return allSchools.stream()
                .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(schoolName.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验邮箱后缀是否严格匹配所选学校（emailSuffix 如 szu.edu.cn 表示域名须为 szu.edu.cn 或 *.szu.edu.cn）
     * 例如：xxx@szu.edu.cn、xxx@mail.szu.edu.cn 通过；xxx@qq.szu.edu 不通过
     */
    public boolean isEmailSuffixMatch(String email, String schoolName) {
        if (email == null || !email.contains("@")) return false;
        SchoolItem school = findSchoolByName(schoolName);
        if (school == null || school.getEmailSuffix() == null || school.getEmailSuffix().isBlank()) {
            return true;
        }
        int at = email.indexOf("@");
        String domain = (at >= 0 && at < email.length() - 1) ? email.substring(at + 1).trim().toLowerCase() : "";
        String suffix = school.getEmailSuffix().trim().toLowerCase();
        return domain.equals(suffix) || domain.endsWith("." + suffix);
    }
}
