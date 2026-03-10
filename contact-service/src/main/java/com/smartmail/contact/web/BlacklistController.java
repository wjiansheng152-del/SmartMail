package com.smartmail.contact.web;

import com.smartmail.common.result.Result;
import com.smartmail.contact.entity.Blacklist;
import com.smartmail.contact.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 黑名单管理：添加黑名单、校验是否在黑名单、查询黑名单列表；黑名单邮箱不参与发送。
 */
@RestController
@RequestMapping("/api/contact/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    /** 添加黑名单邮箱，可选来源 source */
    @PostMapping
    public Result<Blacklist> add(@RequestParam String email, @RequestParam(required = false) String source) {
        Blacklist b = new Blacklist();
        b.setEmail(email);
        b.setSource(source);
        b.setCreateTime(LocalDateTime.now());
        blacklistService.save(b);
        return Result.ok(b);
    }

    /** 检查邮箱是否在黑名单，true 表示不应发送 */
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam String email) {
        return Result.ok(blacklistService.isBlacklisted(email));
    }

    /** 查询当前租户下黑名单列表 */
    @GetMapping("/list")
    public Result<List<Blacklist>> list() {
        return Result.ok(blacklistService.list());
    }
}
