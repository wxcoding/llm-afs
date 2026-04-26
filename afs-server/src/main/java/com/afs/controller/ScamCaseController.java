package com.afs.controller;

import com.afs.entity.ScamCase;
import com.afs.service.ScamCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 诈骗案例控制器
 *
 * 提供诈骗案例的 CRUD 接口，支持按类型筛选。
 */
@RestController
@RequestMapping("/api/cases")
@CrossOrigin
public class ScamCaseController {

    @Autowired
    private ScamCaseService scamCaseService;

    /**
     * 获取案例列表
     *
     * 支持按案例类型筛选。
     *
     * @param type 案例类型（可选），如"电信诈骗"、"网络诈骗"等
     * @return 案例列表
     */
    @GetMapping
    public Map<String, Object> getCases(@RequestParam(required = false) String type) {
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

    /**
     * 获取案例详情
     *
     * @param id 案例 ID
     * @return 案例详细信息
     */
    @GetMapping("/{id}")
    public Map<String, Object> getCaseById(@PathVariable Long id) {
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

    /**
     * 添加诈骗案例
     *
     * @param scamCase 案例对象
     * @return 创建结果
     */
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

    /**
     * 更新诈骗案例
     *
     * @param id       案例 ID
     * @param scamCase 包含更新内容的案例对象
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateCase(@PathVariable Long id, @RequestBody ScamCase scamCase) {
        Map<String, Object> result = new HashMap<>();
        try {
            ScamCase updated = scamCaseService.updateCase(id, scamCase);
            result.put("success", true);
            result.put("message", "更新成功");
            result.put("case", updated);
        } catch ( Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除诈骗案例
     *
     * @param id 案例 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCase(@PathVariable Long id) {
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
