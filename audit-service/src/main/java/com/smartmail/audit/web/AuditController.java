package com.smartmail.audit.web;

import com.smartmail.audit.entity.AuditLog;
import com.smartmail.audit.service.AuditLogService;
import com.smartmail.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志接口：写入审计记录、按用户与分页查询日志列表，用于操作追溯与合规。
 */
@RestController
@RequestMapping("/api/audit/log")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    /** 写入一条审计日志，createTime 自动设置 */
    @PostMapping
    public Result<AuditLog> create(@RequestBody AuditLog log) {
        log.setCreateTime(LocalDateTime.now());
        auditLogService.save(log);
        return Result.ok(log);
    }

    /** 分页查询审计日志，可按 userId 过滤 */
    @GetMapping("/list")
    public Result<List<AuditLog>> list(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(auditLogService.listPage(userId, page, size));
    }
}
