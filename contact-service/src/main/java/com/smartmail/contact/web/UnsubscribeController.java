package com.smartmail.contact.web;

import com.smartmail.common.result.Result;
import com.smartmail.contact.entity.Unsubscribe;
import com.smartmail.contact.service.UnsubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 退订管理：添加退订邮箱、校验是否已退订；发送前应校验避免向已退订用户发信。
 */
@RestController
@RequestMapping("/api/contact/unsubscribe")
@RequiredArgsConstructor
public class UnsubscribeController {

    private final UnsubscribeService unsubscribeService;

    /** 添加退订记录，若邮箱已存在则直接返回原记录（幂等） */
    @PostMapping
    public Result<Unsubscribe> add(@RequestParam String email, @RequestParam(required = false) String reason) {
        Unsubscribe existing = unsubscribeService.lambdaQuery().eq(Unsubscribe::getEmail, email).one();
        if (existing != null) {
            return Result.ok(existing);
        }
        Unsubscribe u = new Unsubscribe();
        u.setEmail(email);
        u.setReason(reason);
        u.setCreateTime(LocalDateTime.now());
        unsubscribeService.save(u);
        return Result.ok(u);
    }

    /** 检查邮箱是否已退订，true 表示已退订不应再发 */
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam String email) {
        return Result.ok(unsubscribeService.isUnsubscribed(email));
    }

    /** 返回当前租户下所有已退订邮箱列表，供发送前批量过滤 */
    @GetMapping("/list")
    public Result<java.util.List<String>> listEmails() {
        return Result.ok(unsubscribeService.listAllEmails());
    }
}
