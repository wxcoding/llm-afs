package com.afs.controller;

import com.afs.entity.Knowledge;
import com.afs.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @GetMapping
    public Map<String, Object> getKnowledge(@RequestParam(required = false) String category) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Knowledge> list;
            if (category != null && !category.isEmpty()) {
                list = knowledgeService.getKnowledgeByCategory(category);
            } else {
                list = knowledgeService.getAllKnowledge();
            }
            result.put("success", true);
            result.put("list", list);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getKnowledgeById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Knowledge knowledge = knowledgeService.getKnowledgeById(id);
            result.put("success", true);
            result.put("knowledge", knowledge);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
