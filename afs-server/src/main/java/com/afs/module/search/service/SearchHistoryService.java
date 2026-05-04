package com.afs.module.search.service;

import com.afs.module.search.entity.SearchHistory;
import com.afs.module.search.entity.HotSearch;
import com.afs.module.search.mapper.SearchHistoryMapper;
import com.afs.module.search.mapper.HotSearchMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索服务类
 * 
 * 负责搜索历史记录、热门搜索统计等业务逻辑
 */
@Service
public class SearchHistoryService extends ServiceImpl<SearchHistoryMapper, SearchHistory> {

    @Autowired
    private HotSearchMapper hotSearchMapper;

    public SearchHistory saveSearch(Long userId, String keyword, String searchType, Integer resultCount) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setSearchType(searchType);
        history.setResultCount(resultCount);
        this.save(history);

        QueryWrapper<HotSearch> wrapper = new QueryWrapper<>();
        wrapper.eq("keyword", keyword).eq("period", "daily");
        HotSearch hotSearch = hotSearchMapper.selectOne(wrapper);
        if (hotSearch != null) {
            hotSearch.setSearchCount(hotSearch.getSearchCount() + 1);
            hotSearchMapper.updateById(hotSearch);
        } else {
            HotSearch newHot = new HotSearch();
            newHot.setKeyword(keyword);
            newHot.setSearchCount(1);
            newHot.setPeriod("daily");
            hotSearchMapper.insert(newHot);
        }

        return history;
    }

    public List<SearchHistory> getUserHistory(Long userId, int limit) {
        return this.lambdaQuery()
                .eq(SearchHistory::getUserId, userId)
                .orderByDesc(SearchHistory::getCreateTime)
                .last("LIMIT " + limit)
                .list();
    }

    public void deleteUserHistory(Long userId) {
        this.remove(new QueryWrapper<SearchHistory>().eq("user_id", userId));
    }

    public List<HotSearch> getHotSearch(int limit) {
        return hotSearchMapper.selectList(new QueryWrapper<HotSearch>()
                .eq("period", "daily")
                .orderByDesc("search_count")
                .last("LIMIT " + limit));
    }
}
