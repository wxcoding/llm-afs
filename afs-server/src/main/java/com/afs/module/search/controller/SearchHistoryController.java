package com.afs.module.search.controller;

import com.afs.common.Result;
import com.afs.common.PageResult;
import com.afs.module.search.entity.SearchHistory;
import com.afs.module.search.entity.HotSearch;
import com.afs.module.search.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "搜索管理", description = "搜索历史、热门搜索等接口")
@RestController
@RequestMapping("/api/search")
@CrossOrigin
public class SearchHistoryController {

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Operation(summary = "保存搜索记录", description = "记录用户搜索行为，同时更新热门搜索统计")
    @PostMapping("/history")
    public Result<SearchHistory> saveSearch(
            @Parameter(description = "用户 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "搜索类型") @RequestParam(required = false) String searchType,
            @Parameter(description = "搜索结果数量") @RequestParam(required = false, defaultValue = "0") Integer resultCount) {
        return Result.success(searchHistoryService.saveSearch(userId, keyword, searchType, resultCount));
    }

    @Operation(summary = "获取用户搜索历史", description = "获取指定用户的搜索历史记录")
    @GetMapping("/history")
    public Result<List<SearchHistory>> getUserHistory(
            @Parameter(description = "用户 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(searchHistoryService.getUserHistory(userId, limit));
    }

    @Operation(summary = "清空用户搜索历史", description = "删除指定用户的所有搜索记录")
    @DeleteMapping("/history")
    public Result<Void> deleteUserHistory(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId) {
        searchHistoryService.deleteUserHistory(userId);
        return Result.success();
    }

    @Operation(summary = "获取热门搜索", description = "获取当前热门搜索关键词排名")
    @GetMapping("/hot")
    public Result<List<HotSearch>> getHotSearch(
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") int limit) {
        return Result.success(searchHistoryService.getHotSearch(limit));
    }
}
