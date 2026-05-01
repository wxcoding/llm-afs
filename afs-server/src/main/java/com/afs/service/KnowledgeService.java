package com.afs.service;

import com.afs.entity.Knowledge;
import com.afs.mapper.KnowledgeMapper;
import com.afs.util.DocumentParser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务
 *
 * 提供知识库内容的增删改查功能，同时负责将知识库内容同步到向量库，
 * 支持按分类查询和关键词搜索。
 */
@Service
public class KnowledgeService {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private RagService ragService;

    /**
     * 获取所有知识库列表
     *
     * @return 按创建时间倒序排列的知识列表
     */
    public List<Knowledge> getAllKnowledge() {
        return knowledgeMapper.selectList(new QueryWrapper<Knowledge>().orderByDesc("create_time"));
    }

    /**
     * 根据分类获取知识列表
     *
     * @param category 知识分类，如"防范技巧"、"诈骗类型"等
     * @return 该分类下的所有知识
     */
    public List<Knowledge> getKnowledgeByCategory(String category) {
        return knowledgeMapper.selectList(
                new QueryWrapper<Knowledge>().eq("category", category).orderByDesc("create_time")
        );
    }

    /**
     * 根据 ID 获取知识详情
     *
     * @param id 知识 ID
     * @return 知识对象，如果不存在返回 null
     */
    public Knowledge getKnowledgeById(Long id) {
        return knowledgeMapper.selectById(id);
    }

    /**
     * 添加知识条目
     *
     * 同时将知识内容同步到向量库，以便 AI 检索。
     *
     * @param knowledge 知识对象
     * @return 创建后的知识对象
     */
    public Knowledge addKnowledge(Knowledge knowledge) {
        // 设置创建时间
        knowledge.setCreateTime(LocalDateTime.now());
        // 保存到数据库
        knowledgeMapper.insert(knowledge);
        
        // 如果有内容，同步到向量库
        if (knowledge.getContent() != null && !knowledge.getContent().isEmpty()) {
            ragService.addKnowledgeDocument(
                    knowledge.getId(),     // 知识ID
                    knowledge.getTitle(),  // 标题
                    knowledge.getCategory(),  // 分类
                    knowledge.getContent()     // 内容
            );
        }
        return knowledge;
    }

    /**
     * 更新知识条目
     *
     * 更新向量库中的文档以保持同步。
     *
     * @param id       知识 ID
     * @param knowledge 包含更新内容的知识对象
     * @return 更新后的知识对象
     * @throws RuntimeException 如果知识不存在
     */
    public Knowledge updateKnowledge(Long id, Knowledge knowledge) {
        // 获取现有知识
        Knowledge existing = knowledgeMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("知识不存在");
        }
        
        // 更新字段
        existing.setTitle(knowledge.getTitle());
        existing.setCategory(knowledge.getCategory());
        existing.setContent(knowledge.getContent());
        
        // 保存到数据库
        knowledgeMapper.updateById(existing);

        // 先从向量库删除旧文档
        ragService.deleteKnowledgeDocument(id);
        
        // 如果有内容，重新同步到向量库
        if (existing.getContent() != null && !existing.getContent().isEmpty()) {
            ragService.addKnowledgeDocument(
                    existing.getId(),
                    existing.getTitle(),
                    existing.getCategory(),
                    existing.getContent()
            );
        }
        return existing;
    }

    /**
     * 删除知识条目
     *
     * 同时从向量库中删除对应的文档。
     *
     * @param id 知识 ID
     */
    public void deleteKnowledge(Long id) {
        // 先从向量库删除文档
        ragService.deleteKnowledgeDocument(id);
        // 再从数据库删除
        knowledgeMapper.deleteById(id);
    }

    /**
     * 搜索知识
     *
     * 在标题、内容、分类中进行关键词模糊匹配。
     *
     * @param keyword 搜索关键词
     * @return 匹配的知识列表
     */
    public List<Knowledge> searchKnowledge(String keyword) {
        return knowledgeMapper.selectList(
                new QueryWrapper<Knowledge>()
                        .like("title", keyword)
                        .or()
                        .like("content", keyword)
                        .or()
                        .like("category", keyword)
                        .orderByDesc("create_time")
        );
    }

    public List<Map<String, Object>> semanticSearch(String query, int topK) {
        return ragService.searchWithMetadata(query, topK);
    }

    /**
     * 初始化默认知识数据
     *
     * 当知识库为空时，自动插入预设的防诈骗知识内容，
     * 包括防范技巧、诈骗类型、应对方法等。
     */
    public void initDefaultKnowledge() {
        if (knowledgeMapper.selectCount(new QueryWrapper<Knowledge>()) == 0) {
            Knowledge k1 = new Knowledge();
            k1.setTitle("防范电信诈骗的十个凡是");
            k1.setCategory("防范技巧");
            k1.setContent("""
                1. 凡是自称公检法要求汇款的
                2. 凡是让你汇款到'安全账户'的
                3. 凡是通知中奖、领取补贴要你先交钱的
                4. 凡是通知'家属'出事先要汇款的
                5. 凡是电话中索要个人信息和银行卡信息的
                6. 凡是让你开通网银接受检查的
                7. 凡是自称领导(老板)要求打款的
                8. 凡是陌生网站(链接)要求登记银行卡信息的
                9. 凡是QQ、微信要求转账汇款的
                10. 凡是高收益投资、稳赚不赔的

                遇到以上情况一律是诈骗！
                """);
            k1.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(k1);

            Knowledge k2 = new Knowledge();
            k2.setTitle("六个一律要记牢");
            k2.setCategory("防范技巧");
            k2.setContent("""
                1. 接电话，只要一谈到银行卡，一律挂掉
                2. 接电话，只要一谈到中奖了，一律挂掉
                3. 接电话，只要一谈到是公检法税务或领导干部的，一律挂掉
                4. 所有短信，但凡让点击链接的，一律删掉
                5. 微信不认识的人发来的链接，一律不点
                6. 所有170/171开头的电话，一律不接

                记住这六个一律，远离电信诈骗！
                """);
            k2.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(k2);

            Knowledge k3 = new Knowledge();
            k3.setTitle("常见诈骗类型大盘点");
            k3.setCategory("诈骗类型");
            k3.setContent("""
                【电信诈骗】
                - 冒充公检法诈骗
                - 冒充客服退款诈骗
                - 虚假中奖诈骗
                - 钓鱼短信诈骗

                【网络诈骗】
                - 刷单返利诈骗
                - 虚假购物诈骗
                - 网络游戏诈骗
                - 虚假招聘诈骗

                【情感诈骗】
                - 杀猪盘诈骗
                - 婚恋交友诈骗
                - 冒充熟人诈骗

                【投资诈骗】
                - 虚假投资理财
                - 非法集资
                - 虚拟币传销
                - 原始股骗局
                """);
            k3.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(k3);

            Knowledge k4 = new Knowledge();
            k4.setTitle("遇到诈骗怎么办");
            k4.setCategory("应对方法");
            k4.setContent("""
                1. 保持冷静，不要恐慌
                2. 立即停止一切操作，不要继续转账
                3. 保留所有证据（聊天记录、转账凭证、对方联系方式等）
                4. 第一时间拨打110报警
                5. 拨打12321举报诈骗信息
                6. 如已转账，立即联系银行冻结账户
                7. 及时修改密码，防止再次被骗
                8. 告知家人朋友，防止他们也被骗

                记住：被骗后及时报警，有可能追回损失！
                """);
            k4.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(k4);

            Knowledge k5 = new Knowledge();
            k5.setTitle("如何保护个人信息");
            k5.setCategory("防范技巧");
            k5.setContent("""
                【个人信息保护指南】

                1. 身份证复印件要注明用途
                2. 快递单要撕碎或涂抹后再丢弃
                3. 不在社交平台晒机票、火车票、护照等
                4. 不随意连接公共WiFi
                5. 手机验证码不要告诉任何人
                6. 银行卡密码要定期更换
                7. 不在不明网站填写个人信息
                8. 及时注销不再使用的账户

                【注意】
                - 官方客服不会索要验证码
                - 不会有安全账户
                - 不会要求你转账
                """);
            k5.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(k5);

            Knowledge k6 = new Knowledge();
            k6.setTitle("刷单返利诈骗详解");
            k6.setCategory("诈骗类型");
            k6.setContent("""
                【什么是刷单返利诈骗】
                刷单返利诈骗是目前发案率最高的电信网络诈骗类型，占所有电信网络诈骗案件的近三分之一。

                【常见套路】
                1. 第一步：通过群聊、短视频等渠道发布兼职刷单广告
                2. 第二步：先让受害人做小额任务，并真实返利，建立信任
                3. 第三步：诱导受害人下载虚假APP或进入虚假网站
                4. 第四步：要求做大额任务，以"连单""卡单"等理由拒绝返现
                5. 第五步：受害人投入越来越多，最终血本无归

                【识别要点】
                - 所有刷单都是违法行为
                - 正规平台不会要求先充值再做任务
                - "轻松赚钱""日入过百"都是诱饵
                - 一旦开始做大额任务，基本就是骗局

                【防范建议】
                - 不要相信任何刷单兼职信息
                - 不要下载陌生人推荐的APP
                - 不要向陌生账户转账
                """);
            k6.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(k6);

            Knowledge k7 = new Knowledge();
            k7.setTitle("冒充公检法诈骗详解");
            k7.setCategory("诈骗类型");
            k7.setContent("""
                【什么是冒充公检法诈骗】
                骗子冒充公安、检察院、法院等机关工作人员，以受害人涉嫌犯罪为由，要求配合调查并转账。

                【常见套路】
                1. 来电自称是公安机关/检察院/法院工作人员
                2. 声称你涉嫌洗钱、非法出入境等犯罪
                3. 要求添加QQ/微信进行"线上办案"
                4. 发送伪造的"逮捕令""通缉令"
                5. 要求将资金转入"安全账户"进行核查
                6. 威胁如果不配合将面临法律后果

                【识别要点】
                - 公检法不会通过电话办案
                - 公检法不会通过QQ/微信发送法律文书
                - 公检法不存在所谓的"安全账户"
                - 不会要求你转账到任何账户

                【防范建议】
                - 接到此类电话立即挂断
                - 有疑问请拨打110或到当地派出所咨询
                - 不要添加陌生人QQ/微信
                - 不要点击不明链接或下载不明软件
                """);
            k7.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(k7);
        }
    }

    /**
     * 将所有知识库内容同步到向量库
     *
     * 用于初始化或修复向量库数据。
     */
    public void syncAllToVectorStore() {
        List<Knowledge> allKnowledge = knowledgeMapper.selectList(new QueryWrapper<>());

        ragService.clearKnowledgeVector();

        for (Knowledge k : allKnowledge) {
            if (k.getContent() != null && !k.getContent().isEmpty()) {
                ragService.addKnowledgeDocument(k.getId(), k.getTitle(), k.getCategory(), k.getContent());
            }
        }
    }

    /**
     * 最大文件大小限制（10MB）
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 上传文档
     *
     * @param file     上传的文件
     * @param category 分类（可选）
     * @param title    标题（可选）
     * @return 上传结果，包含 success、message、knowledge 等字段
     */
    public Map<String, Object> uploadDocument(MultipartFile file, String category, String title) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要上传的文件");
                return result;
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                result.put("success", false);
                result.put("message", "文件名不能为空");
                return result;
            }

            // 验证文件格式
            String lowerFileName = fileName.toLowerCase();
            if (!lowerFileName.endsWith(".pdf") && !lowerFileName.endsWith(".docx") &&
                    !lowerFileName.endsWith(".doc") && !lowerFileName.endsWith(".md") &&
                    !lowerFileName.endsWith(".markdown") && !lowerFileName.endsWith(".txt") &&
                    !lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
                result.put("success", false);
                result.put("message", "不支持的文件格式，请上传 PDF、Word、Markdown、Excel 或文本文件");
                return result;
            }

            // 验证文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                result.put("success", false);
                result.put("message", "文件大小超过限制（最大 10MB）");
                return result;
            }

            // 解析文档
            Map<String, Object> parseResult = DocumentParser.parseDocument(file);
            String content = (String) parseResult.get("content");
            String documentType = (String) parseResult.get("documentType");

            if (content == null || content.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "无法从文档中提取内容");
                return result;
            }

            // 清理内容
            content = content.trim().replaceAll("\\s+", " ");

            // 自动提取标题（如果未提供）
            if (title == null || title.trim().isEmpty()) {
                title = DocumentParser.extractTitle(fileName);
            }

            // 自动建议分类（如果未提供）
            if (category == null || category.trim().isEmpty()) {
                category = DocumentParser.suggestCategory(fileName, content);
            }

            // 创建知识对象
            Knowledge knowledge = new Knowledge();
            knowledge.setTitle(title);
            knowledge.setCategory(category);
            knowledge.setContent(content);
            knowledge.setDocumentType(documentType);
            knowledge.setSourceFile(fileName);
            knowledge.setFileSize(file.getSize());
            knowledge.setCharCount((Integer) parseResult.get("charCount"));

            // 保存知识
            Knowledge created = addKnowledge(knowledge);

            // 设置成功结果
            result.put("success", true);
            result.put("message", "文档上传成功");
            result.put("knowledge", created);
            result.put("documentType", documentType);
            result.put("charCount", parseResult.get("charCount"));
            result.put("fileName", fileName);
            result.put("fileSize", file.getSize());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "文档处理失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 批量上传文档
     *
     * @param files    上传的文件数组
     * @param category 分类（可选，统一应用于所有文件）
     * @return 上传结果，包含 successCount、failCount、failMessages 等字段
     */
    public Map<String, Object> uploadDocumentsBatch(MultipartFile[] files, String category) {
        Map<String, Object> result = new HashMap<>();

        if (files == null || files.length == 0) {
            result.put("success", false);
            result.put("message", "请选择要上传的文件");
            return result;
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder failMessages = new StringBuilder();

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            try {
                Map<String, Object> uploadResult = uploadDocument(file, category, null);
                if ((Boolean) uploadResult.get("success")) {
                    successCount++;
                } else {
                    failCount++;
                    if (failMessages.length() > 0) {
                        failMessages.append("; ");
                    }
                    failMessages.append(fileName).append(": ").append(uploadResult.get("message"));
                }
            } catch (Exception e) {
                failCount++;
                if (failMessages.length() > 0) {
                    failMessages.append("; ");
                }
                failMessages.append(fileName).append(": ").append(e.getMessage());
            }
        }

        result.put("success", failCount == 0);
        result.put("successCount", successCount);
        result.put("failCount", failCount);

        if (failCount == 0) {
            result.put("message", "全部上传成功");
        } else if (successCount == 0) {
            result.put("message", "全部上传失败: " + failMessages);
        } else {
            result.put("message", "部分上传成功");
            result.put("failMessages", failMessages.toString());
        }

        return result;
    }
}
