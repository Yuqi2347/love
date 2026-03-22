package com.campus.love.pairdate.controller;

import com.campus.love.common.result.Result;
import com.campus.love.pairdate.dto.PairDateNegotiationVO;
import com.campus.love.pairdate.dto.PairDateSubmitRequest;
import com.campus.love.pairdate.dto.PairDateTimeVO;
import com.campus.love.pairdate.dto.PairDateYueRequest;
import com.campus.love.pairdate.service.PairDateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "心动约一下", description = "匹配互关后双方「约一下」三步协商")
@RestController
@RequestMapping("/pair-date")
@RequiredArgsConstructor
public class PairDateController {

    private final PairDateService pairDateService;

    @Operation(summary = "点击「约一下」意向（双方均记录后创建协商并生成约会方式）")
    @PostMapping("/yue")
    public Result<PairDateNegotiationVO> yue(@Valid @RequestBody PairDateYueRequest request) {
        return Result.success(pairDateService.yue(request.getMatchResultId()));
    }

    @Operation(summary = "查询协商详情")
    @GetMapping("/{id}")
    public Result<PairDateNegotiationVO> getOne(@PathVariable Long id) {
        return Result.success(pairDateService.getById(id));
    }

    @Operation(summary = "提交某一步选择（1/2/3）")
    @PostMapping("/{id}/submit")
    public Result<PairDateNegotiationVO> submit(@PathVariable Long id, @Valid @RequestBody PairDateSubmitRequest body) {
        return Result.success(pairDateService.submit(id, body));
    }

    @Operation(summary = "约会倒计时：目标时间戳 + 服务端时间")
    @GetMapping("/{id}/time")
    public Result<PairDateTimeVO> time(@PathVariable Long id) {
        return Result.success(pairDateService.getTime(id));
    }

    @Operation(summary = "按对方用户 ID 查本周协商（防重复入口）")
    @GetMapping("/pair/{targetUserId}")
    public Result<PairDateNegotiationVO> byPair(@PathVariable Long targetUserId) {
        return Result.success(pairDateService.getByPairTarget(targetUserId));
    }
}
