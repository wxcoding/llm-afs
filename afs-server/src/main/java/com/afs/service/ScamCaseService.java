package com.afs.service;

import com.afs.entity.ScamCase;
import com.afs.mapper.ScamCaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScamCaseService {

    @Autowired
    private ScamCaseMapper scamCaseMapper;

    public List<ScamCase> getAllCases() {
        return scamCaseMapper.selectList(new QueryWrapper<ScamCase>().orderByDesc("create_time"));
    }

    public List<ScamCase> getCasesByType(String type) {
        return scamCaseMapper.selectList(
                new QueryWrapper<ScamCase>().eq("type", type).orderByDesc("create_time")
        );
    }

    public ScamCase getCaseById(Long id) {
        return scamCaseMapper.selectById(id);
    }

    public void initDefaultCases() {
        if (scamCaseMapper.selectCount(new QueryWrapper<ScamCase>()) == 0) {
            ScamCase c1 = new ScamCase();
            c1.setTitle("冒充公检法诈骗");
            c1.setType("电信诈骗");
            c1.setContent("张先生接到自称公安局的电话，称其涉嫌洗钱犯罪，要求将资金转入'安全账户'进行核查。张先生按对方指示转账50万元后发现被骗。");
            c1.setTips("公检法机关不会通过电话办案，不会要求转账到所谓'安全账户'，遇到此类电话直接挂断并报警。");
            c1.setCreateTime(java.time.LocalDateTime.now());
            scamCaseMapper.insert(c1);

            ScamCase c2 = new ScamCase();
            c2.setTitle("刷单返利诈骗");
            c2.setType("网络诈骗");
            c2.setContent("李女士被拉入一个刷单群，前几单确实收到返利，随后对方诱导她做大单，需要充值10万元才能提现，最终本金无法收回。");
            c2.setTips("刷单本就是违法行为，正规平台不会要求先充值再做任务，所有刷单返利都是诈骗！");
            c2.setCreateTime(java.time.LocalDateTime.now());
            scamCaseMapper.insert(c2);

            ScamCase c3 = new ScamCase();
            c3.setTitle("杀猪盘诈骗");
            c3.setType("情感诈骗");
            c3.setContent("王女士在网上认识一名'成功人士'，对方嘘寒问暖建立感情后，诱导她在某投资平台投资，先后投入30万元后平台无法访问，'男友'也失联。");
            c3.setTips("网恋需谨慎，不要轻易相信'高富帅'、'成功人士'，更不要跟着对方投资理财，这通常是'杀猪盘'骗局。");
            c3.setCreateTime(java.time.LocalDateTime.now());
            scamCaseMapper.insert(c3);

            ScamCase c4 = new ScamCase();
            c4.setTitle("虚假中奖诈骗");
            c4.setType("电信诈骗");
            c4.setContent("陈先生收到'中奖'短信，称其中了100万大奖，需要先交2万元手续费才能领取奖金，他转账后对方消失。");
            c4.setTips("天上不会掉馅饼！正规中奖不会要求先交钱，收到中奖信息要冷静核实，不轻信、不转账。");
            c4.setCreateTime(java.time.LocalDateTime.now());
            scamCaseMapper.insert(c4);

            ScamCase c5 = new ScamCase();
            c5.setTitle("虚假投资理财诈骗");
            c5.setType("投资诈骗");
            c5.setContent("赵先生被拉入股票交流群，群内有'老师'推荐下载某投资APP，声称有内幕消息能稳赚。赵先生投入20万元后APP无法打开。");
            c5.setTips("不要轻信荐股群、老师推荐，投资理财要通过正规金融机构，不下载不明投资APP，不向个人账户转账。");
            c5.setCreateTime(java.time.LocalDateTime.now());
            scamCaseMapper.insert(c5);
        }
    }
}
