package com.afs.module.stats.controller;

import com.afs.module.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "统计数据", description = "获取系统各模块的统计数据")
@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    @Autowired
    private StatsService statsService;

    @Operation(summary = "获取系统统计数据", description = "返回各数据表的记录总数，包括用户、案例、知识、会话和消息数量")
    @GetMapping
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.putAll(statsService.getStats());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
