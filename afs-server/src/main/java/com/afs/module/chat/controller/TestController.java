package com.afs.module.chat.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @Author: guanwx
 * @CreateTime: 2026/10/4 星期日 04:07
 * @Description:
 * @Version: 1.0
 */

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ChatClient chatClient;

    @GetMapping("/chatString")
    public String chatString(@RequestParam String message) {
        String content = chatClient.prompt().user(message).call().content();
        log.debug("生成信息：{}", content);
        return content;
    }

    @GetMapping(value = "/chatStream/{message}", produces = "text/html;charset=UTF-8")
    public Flux<String> chatStream(@PathVariable String message) {
        return chatClient.prompt().user(message).stream().content();
    }


}
