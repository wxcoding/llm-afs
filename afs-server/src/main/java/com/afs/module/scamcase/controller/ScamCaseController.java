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

@Tag(name = "诈骗案例管理", description = "诈骗案例的增删改查，支持按类型筛选")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cases")
@CrossOrigin
public class ScamCaseController {

    private final ScamCaseService scamCaseService;

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

    @Operation(summary = "获取案例详情")
    @GetMapping("/{id}")
    public Result<ScamCase> getCaseById(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id) {
        return Result.success(scamCaseService.getCaseById(id));
    }

    @Operation(summary = "添加诈骗案例")
    @RequireRole
    @PostMapping
    public Result<ScamCase> addCase(@RequestBody ScamCase scamCase) {
        return Result.success("添加成功", scamCaseService.addCase(scamCase));
    }

    @Operation(summary = "更新诈骗案例")
    @RequireRole
    @PutMapping("/{id}")
    public Result<ScamCase> updateCase(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id,
            @RequestBody ScamCase scamCase) {
        return Result.success("更新成功", scamCaseService.updateCase(id, scamCase));
    }

    @Operation(summary = "删除诈骗案例")
    @RequireRole
    @DeleteMapping("/{id}")
    public Result<Void> deleteCase(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id) {
        scamCaseService.deleteCase(id);
        return Result.success("删除成功");
    }
}
