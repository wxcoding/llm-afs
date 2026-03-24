package com.afs.controller;

import com.afs.entity.ScamCase;
import com.afs.service.ScamCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin
public class ScamCaseController {

    @Autowired
    private ScamCaseService scamCaseService;

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
}
