package com.afs.module.scamcase.controller;

import com.afs.module.scamcase.entity.ScamCase;
import com.afs.module.scamcase.service.ScamCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "诈骗案例管理", description = "诈骗案例的增删改查，支持按类型筛选")
@RestController
@RequestMapping("/api/cases")
@CrossOrigin
public class ScamCaseController {

    @Autowired
    private ScamCaseService scamCaseService;

    @Operation(summary = "获取案例列表", description = "支持按案例类型筛选，如'电信诈骗'、'网络诈骗'等")
    @GetMapping
    public Map<String, Object> getCases(
            @Parameter(description = "案例类型") @RequestParam(required = false) String type) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ScamCase> cases;
            if (type != null && !type.isEmpty()) {
                cases = scamCaseService.getCasesByType(type);
            } else {
                cases = scamCaseService.getAllCases();
            }
            result.put("success", true);
            result.put("cases", cases);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "获取案例详情")
    @GetMapping("/{id}")
    public Map<String, Object> getCaseById(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ScamCase caseInfo = scamCaseService.getCaseById(id);
            result.put("success", true);
            result.put("case", caseInfo);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "添加诈骗案例")
    @PostMapping
    public Map<String, Object> addCase(@RequestBody ScamCase scamCase) {
        Map<String, Object> result = new HashMap<>();
        try {
            ScamCase created = scamCaseService.addCase(scamCase);
            result.put("success", true);
            result.put("message", "添加成功");
            result.put("case", created);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "更新诈骗案例")
    @PutMapping("/{id}")
    public Map<String, Object> updateCase(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id,
            @RequestBody ScamCase scamCase) {
        Map<String, Object> result = new HashMap<>();
        try {
            ScamCase updated = scamCaseService.updateCase(id, scamCase);
            result.put("success", true);
            result.put("message", "更新成功");
            result.put("case", updated);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "删除诈骗案例")
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCase(
            @Parameter(description = "案例 ID", required = true) @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            scamCaseService.deleteCase(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
