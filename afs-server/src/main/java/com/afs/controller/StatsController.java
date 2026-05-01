package com.afs.controller;

import com.afs.mapper.KnowledgeMapper;
import com.afs.mapper.ScamCaseMapper;
import com.afs.mapper.ChatSessionMapper;
import com.afs.mapper.ChatMessageMapper;
import com.afs.mapper.UserMapper;
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
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private ScamCaseMapper scamCaseMapper;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Operation(summary = "获取系统统计数据", description = "返回各数据表的记录总数，包括用户、案例、知识、会话和消息数量")
    @GetMapping
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("knowledgeCount", knowledgeMapper.selectCount(null));
            result.put("caseCount", scamCaseMapper.selectCount(null));
            result.put("sessionCount", chatSessionMapper.selectCount(null));
            result.put("messageCount", chatMessageMapper.selectCount(null));
            result.put("userCount", userMapper.selectCount(null));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
