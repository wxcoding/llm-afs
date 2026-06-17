package com.afs.module.rag.base;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 查询改写与扩展服务
 * 
 * 解决的问题：
 * - 用户用词与文档用词不一致（如"被骗"vs"诈骗"）
 * - 口语化与书面语差异（如"怎么办"vs"如何处理"）
 * - 同义词、近义词覆盖
 * 
 * 实现思路：
 * 1. 同义词扩展：将用户查询扩展为多个同义词
 * 2. 口语化转书面语：将口语词汇转换为书面语
 * 
 * @author AFS
 */
@Component
public class QueryExpander {

    /**
     * 反诈领域同义词词典
     * Key: 核心词, Value: 同义词列表
     */
    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();

    /**
     * 口语化词汇 → 书面语映射
     */
    private static final Map<String, String> COLLOQUIAL_TO_FORMAL = new LinkedHashMap<>();

    static {
        // 诈骗相关同义词
        SYNONYMS.put("诈骗", Arrays.asList("骗局", "欺诈", "骗钱", "行骗", "诈骗行为"));
        SYNONYMS.put("被骗", Arrays.asList("上当", "被坑", "被套路", "遭遇诈骗", "损失"));
        SYNONYMS.put("骗子", Arrays.asList("诈骗犯", "不法分子", "黑心商家", "骗子"));
        
        // 金钱相关同义词
        SYNONYMS.put("钱", Arrays.asList("资金", "款项", "金额", "存款", "财产", "积蓄"));
        SYNONYMS.put("转账", Arrays.asList("汇款", "打款", "支付", "付款", "交易"));
        SYNONYMS.put("追回", Arrays.asList("挽回", "讨回", "取回", "追讨", "索回"));
        
        // 贷款相关同义词
        SYNONYMS.put("贷款", Arrays.asList("借贷", "借款", "信用贷", "网贷", "小额贷"));
        SYNONYMS.put("利息", Arrays.asList("利率", "手续费", "服务费", "息费"));
        SYNONYMS.put("逾期", Arrays.asList("延期", "拖欠", "违约", "违约行为"));
        
        // 投资相关同义词
        SYNONYMS.put("投资", Arrays.asList("理财", "炒股", "基金", "赚钱", "收益"));
        SYNONYMS.put("高回报", Arrays.asList("高收益", "高利润", "稳赚", "保本", "零风险"));
        SYNONYMS.put("亏损", Arrays.asList("赔钱", "亏本", "损失", "跌", "贬值"));
        
        // 维权相关同义词
        SYNONYMS.put("报警", Arrays.asList("报案", "举报", "投诉", "求助警方", "报案处理"));
        SYNONYMS.put("维权", Arrays.asList("追责", "索赔", "追偿", "讨公道", "法律途径"));
        SYNONYMS.put("起诉", Arrays.asList("诉讼", "状告", "起诉", "法院", "打官司"));
        
        // 预防相关同义词
        SYNONYMS.put("防范", Arrays.asList("预防", "防止", "避免", "警惕", "识别"));
        SYNONYMS.put("识别", Arrays.asList("辨别", "判断", "鉴别", "看清", "识破"));
        SYNONYMS.put("套路", Arrays.asList("陷阱", "骗局", "圈套", "坑", "手段"));
        
        // 电话网络相关
        SYNONYMS.put("电话", Arrays.asList("来电", "通话", "手机", "号码"));
        SYNONYMS.put("网络", Arrays.asList("互联网", "线上", "网上", "网络平台"));
        SYNONYMS.put("刷单", Arrays.asList("刷信誉", "虚假交易", "刷流量"));
        
        // 社交相关
        SYNONYMS.put("好友", Arrays.asList("朋友", "熟人", "QQ好友", "微信好友"));
        SYNONYMS.put("网友", Arrays.asList("网络朋友", "陌生人", "社交平台认识的人"));
    }

    static {
        // 口语化 → 书面语
        COLLOQUIAL_TO_FORMAL.put("怎么办", "如何处理 解决方案");
        COLLOQUIAL_TO_FORMAL.put("怎么处理", "如何处理 解决方案");
        COLLOQUIAL_TO_FORMAL.put("怎么解决", "如何解决 解决方案");
        COLLOQUIAL_TO_FORMAL.put("能追回吗", "能否追回 挽回损失");
        COLLOQUIAL_TO_FORMAL.put("追得回来吗", "能否追回 挽回损失");
        COLLOQUIAL_TO_FORMAL.put("是不是诈骗", "是否为诈骗 是否骗局");
        COLLOQUIAL_TO_FORMAL.put("是不是真的", "真实性 可信度");
        COLLOQUIAL_TO_FORMAL.put("靠谱吗", "可信度 可靠性");
        COLLOQUIAL_TO_FORMAL.put("能信吗", "可信度 可靠性");
        COLLOQUIAL_TO_FORMAL.put("是真的吗", "真实性 可靠性");
        COLLOQUIAL_TO_FORMAL.put("怎么防", "如何防范 如何预防");
        COLLOQUIAL_TO_FORMAL.put("怎么办才好", "如何处理 正确做法");
        COLLOQUIAL_TO_FORMAL.put("要被骗了", "可能遭遇诈骗");
        COLLOQUIAL_TO_FORMAL.put("钱被骗走了", "资金损失 被诈骗");
        COLLOQUIAL_TO_FORMAL.put("我该怎么办", "我应该如何处理");
    }

    /**
     * 扩展用户查询
     * 
     * @param query 原始查询
     * @return 扩展后的查询列表（包含原始查询和同义词扩展）
     */
    public List<String> expand(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.singletonList("");
        }
        
        List<String> expandedQueries = new ArrayList<>();
        expandedQueries.add(query); // 添加原始查询
        
        String processedQuery = query;
        
        // 1. 处理口语化转书面语
        for (Map.Entry<String, String> entry : COLLOQUIAL_TO_FORMAL.entrySet()) {
            if (processedQuery.contains(entry.getKey())) {
                processedQuery = processedQuery.replace(entry.getKey(), entry.getValue());
            }
        }
        
        // 2. 同义词扩展
        StringBuilder synonymsBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            if (processedQuery.contains(entry.getKey())) {
                synonymsBuilder.append(" ").append(String.join(" ", entry.getValue()));
            }
        }
        
        String synonymsExtension = synonymsBuilder.toString().trim();
        
        // 添加扩展后的查询
        String expandedQuery = processedQuery;
        if (!synonymsExtension.isEmpty()) {
            expandedQuery = processedQuery + " " + synonymsExtension;
        }
        
        if (!expandedQueries.contains(expandedQuery)) {
            expandedQueries.add(expandedQuery);
        }
        
        // 3. 如果原查询包含关键诈骗相关词，添加通用扩展
        if (containsFraudKeywords(query)) {
            String generalExtension = "诈骗 骗局 欺诈 识别 防范 维权";
            if (!expandedQueries.contains(processedQuery + " " + generalExtension)) {
                expandedQueries.add(processedQuery + " " + generalExtension);
            }
        }
        
        return expandedQueries;
    }

    /**
     * 检查查询是否包含诈骗关键词
     */
    private boolean containsFraudKeywords(String query) {
        List<String> fraudKeywords = Arrays.asList(
            "骗", "诈", "钱", "转账", "投资", "贷款", 
            "报警", "维权", "追回", "套路", "高回报"
        );
        
        String lowerQuery = query.toLowerCase();
        for (String keyword : fraudKeywords) {
            if (lowerQuery.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取扩展查询的调试信息
     */
    public String getExpansionInfo(String query) {
        List<String> expansions = expand(query);
        StringBuilder info = new StringBuilder();
        info.append("原始查询: ").append(query).append("\n");
        info.append("扩展查询数量: ").append(expansions.size()).append("\n");
        for (int i = 0; i < expansions.size(); i++) {
            info.append("  [").append(i + 1).append("] ").append(expansions.get(i)).append("\n");
        }
        return info.toString();
    }

    /**
     * 获取同义词词典统计
     */
    public String getDictionaryStats() {
        return String.format(
            "同义词词典统计: %d 个核心词, %d 个口语映射",
            SYNONYMS.size(),
            COLLOQUIAL_TO_FORMAL.size()
        );
    }
}
