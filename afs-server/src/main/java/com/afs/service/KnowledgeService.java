package com.afs.service;

import com.afs.entity.Knowledge;
import com.afs.mapper.KnowledgeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeService {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    public List<Knowledge> getAllKnowledge() {
        return knowledgeMapper.selectList(new QueryWrapper<Knowledge>().orderByDesc("create_time"));
    }

    public List<Knowledge> getKnowledgeByCategory(String category) {
        return knowledgeMapper.selectList(
                new QueryWrapper<Knowledge>().eq("category", category).orderByDesc("create_time")
        );
    }

    public Knowledge getKnowledgeById(Long id) {
        return knowledgeMapper.selectById(id);
    }

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
            k1.setCreateTime(java.time.LocalDateTime.now());
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
            k2.setCreateTime(java.time.LocalDateTime.now());
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
            k3.setCreateTime(java.time.LocalDateTime.now());
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
            k4.setCreateTime(java.time.LocalDateTime.now());
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
            k5.setCreateTime(java.time.LocalDateTime.now());
            knowledgeMapper.insert(k5);
        }
    }
}
