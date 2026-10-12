package com.afs.module.stats.controller;

import com.afs.common.Result;
import com.afs.module.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "统计数据", description = "获取系统各模块的统计数据")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "获取系统统计数据", description = "返回各数据表的记录总数，包括用户、案例、知识、会话和消息数量")
    @GetMapping
    public Result<Map<String, Object>> getStats() {
        return Result.success(statsService.getStats());
    }
}
