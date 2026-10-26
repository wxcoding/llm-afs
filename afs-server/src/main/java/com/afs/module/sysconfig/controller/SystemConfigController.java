package com.afs.module.sysconfig.controller;

import com.afs.common.annotation.RequireRole;
import com.afs.common.Result;
import com.afs.module.sysconfig.entity.SystemConfig;
import com.afs.module.sysconfig.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 *
 * 提供系统参数配置的查询、更新与初始化接口（仅管理员）
 *
 * @author guanwx
 */
@Tag(name = "系统配置", description = "系统参数配置管理接口（仅管理员）")
@RequireRole
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config")
@CrossOrigin
public class SystemConfigController {

    /** 系统配置服务 */
    private final SystemConfigService configService;

    /**
     * 获取所有配置项
     *
     * @return 全部系统配置列表
     */
    @Operation(summary = "获取所有配置", description = "获取系统所有配置项")
    @GetMapping("/all")
    public Result<List<SystemConfig>> getAllConfigs() {
        return Result.success(configService.getAllConfigs());
    }

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    @Operation(summary = "获取配置值", description = "根据配置键获取配置值")
    @GetMapping("/{configKey}")
    public Result<String> getConfigValue(
            @Parameter(description = "配置键", required = true) @PathVariable String configKey) {
        return Result.success(configService.getConfigValue(configKey));
    }

    /**
     * 根据配置类型获取配置项
     *
     * @param configType 配置类型
     * @return 配置键到配置值的映射
     */
    @Operation(summary = "按类型获取配置", description = "根据配置类型获取配置项")
    @GetMapping("/type/{configType}")
    public Result<Map<String, String>> getConfigsByType(
            @Parameter(description = "配置类型", required = true) @PathVariable String configType) {
        return Result.success(configService.getConfigsByType(configType));
    }

    /**
     * 更新指定配置项的值
     *
     * @param configKey   配置键
     * @param configValue 新配置值
     * @return 更新后的配置项
     */
    @Operation(summary = "更新配置", description = "更新指定配置项的值")
    @PutMapping("/{configKey}")
    public Result<SystemConfig> updateConfig(
            @Parameter(description = "配置键", required = true) @PathVariable String configKey,
            @Parameter(description = "配置值", required = true) @RequestParam String configValue) {
        return Result.success(configService.updateConfig(configKey, configValue));
    }

    /**
     * 初始化系统默认配置项
     *
     * @return 操作结果
     */
    @Operation(summary = "初始化默认配置", description = "初始化系统默认配置项")
    @PostMapping("/init")
    public Result<Void> initDefaultConfigs() {
        configService.initDefaultConfigs();
        return Result.success();
    }
}
