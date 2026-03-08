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
                .anyMatch(s -> s.getDomain().equalsIgnoreCase(domain));
    }
}
