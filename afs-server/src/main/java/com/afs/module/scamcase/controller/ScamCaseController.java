package com.afs.module.scamcase.controller;

import com.afs.common.annotation.RequireRole;
import com.afs.common.Result;
import com.afs.module.scamcase.entity.ScamCase;
import com.afs.module.scamcase.service.ScamCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 诈骗案例管理控制器
 *
 * 提供诈骗案例的增删改查接口，支持按类型筛选；写操作仅管理员可用
 *
 * @author guanwx
 */
@Tag(name = "诈骗案例管理", description = "诈骗案例的增删改查，支持按类型筛选")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cases")
@CrossOrigin
public class ScamCaseController {

    /** 诈骗案例服务 */
    private final ScamCaseService scamCaseService;

    /**
     * 获取案例列表，可按案例类型筛选
     *
     * @param type 案例类型（可选），为空时返回全部案例
     * @return 案例列表
     */
    @Operation(summary = "获取案例列表", description = "支持按案例类型筛选，如'电信诈骗'、'网络诈骗'等")
    @GetMapping
    public Result<List<ScamCase>> getCases(
            @Parameter(description = "案例类型") @RequestParam(required = false) String type) {
        List<ScamCase> cases;
        if (type != null && !type.isEmpty()) {
            cases = scamCaseService.getCasesByType(type);
        } else {
            cases = scamCaseService.getAllCases();
        }
        return Result.success(cases);
    }

    /**
     * 根据 ID 获取案例详情
     *
     * @param id 案例 ID
     * @return 案例详情
     */
    @Operation(summary = "获取案例详情")
    @GetMapping("/{id}")
    public Result<ScamCase> getCaseById(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id) {
        return Result.success(scamCaseService.getCaseById(id));
    }

    /**
     * 添加诈骗案例
     *
     * @param scamCase 案例信息
     * @return 新增的案例
     */
    @Operation(summary = "添加诈骗案例")
    @RequireRole
    @PostMapping
    public Result<ScamCase> addCase(@RequestBody ScamCase scamCase) {
        return Result.success("添加成功", scamCaseService.addCase(scamCase));
    }

    /**
     * 更新诈骗案例
     *
     * @param id       案例 ID
     * @param scamCase 更新的案例信息
     * @return 更新后的案例
     */
    @Operation(summary = "更新诈骗案例")
    @RequireRole
    @PutMapping("/{id}")
    public Result<ScamCase> updateCase(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id,
            @RequestBody ScamCase scamCase) {
        return Result.success("更新成功", scamCaseService.updateCase(id, scamCase));
    }

    /**
     * 删除诈骗案例
     *
     * @param id 案例 ID
     * @return 操作结果
     */
    @Operation(summary = "删除诈骗案例")
    @RequireRole
    @DeleteMapping("/{id}")
    public Result<Void> deleteCase(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id) {
        scamCaseService.deleteCase(id);
        return Result.success("删除成功");
    }
}
