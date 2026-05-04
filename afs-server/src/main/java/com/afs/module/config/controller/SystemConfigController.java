package com.afs.module.config.controller;

import com.afs.common.Result;
import com.afs.module.config.entity.SystemConfig;
import com.afs.module.config.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "系统配置", description = "系统参数配置管理接口")
@RestController
@RequestMapping("/api/config")
@CrossOrigin
public class SystemConfigController {

    @Autowired
    private SystemConfigService configService;

    @Operation(summary = "获取所有配置", description = "获取系统所有配置项")
    @GetMapping("/all")
    public Result<List<SystemConfig>> getAllConfigs() {
        return Result.success(configService.getAllConfigs());
    }

    @Operation(summary = "获取配置值", description = "根据配置键获取配置值")
    @GetMapping("/{configKey}")
    public Result<String> getConfigValue(
            @Parameter(description = "配置键", required = true) @PathVariable String configKey) {
        return Result.success(configService.getConfigValue(configKey));
    }

    @Operation(summary = "按类型获取配置", description = "根据配置类型获取配置项")
    @GetMapping("/type/{configType}")
    public Result<Map<String, String>> getConfigsByType(
            @Parameter(description = "配置类型", required = true) @PathVariable String configType) {
        return Result.success(configService.getConfigsByType(configType));
    }

    @Operation(summary = "更新配置", description = "更新指定配置项的值")
    @PutMapping("/{configKey}")
    public Result<SystemConfig> updateConfig(
            @Parameter(description = "配置键", required = true) @PathVariable String configKey,
            @Parameter(description = "配置值", required = true) @RequestParam String configValue) {
        return Result.success(configService.updateConfig(configKey, configValue));
    }

    @Operation(summary = "初始化默认配置", description = "初始化系统默认配置项")
    @PostMapping("/init")
    public Result<Void> initDefaultConfigs() {
        configService.initDefaultConfigs();
        return Result.success();
    }
}
