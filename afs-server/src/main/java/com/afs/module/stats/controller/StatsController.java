package com.afs.module.stats.controller;

import com.afs.common.Result;
import com.afs.module.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统计数据控制器
 *
 * 提供系统各模块数据量的统计查询接口
 *
 * @author guanwx
 */
@Tag(name = "统计数据", description = "获取系统各模块的统计数据")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    /** 统计服务 */
    private final StatsService statsService;

    /**
     * 获取系统各模块的统计数据
     *
     * @return 各数据表记录总数的统计结果
     */
    @Operation(summary = "获取系统统计数据", description = "返回各数据表的记录总数，包括用户、案例、知识、会话和消息数量")
    @GetMapping
    public Result<Map<String, Object>> getStats() {
        return Result.success(statsService.getStats());
    }
}
