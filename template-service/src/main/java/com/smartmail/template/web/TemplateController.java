package com.smartmail.template.web;

import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.template.entity.EmailTemplate;
import com.smartmail.template.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件模板管理：创建、查询、更新、删除模板，支持按 ID 查询与全量列表。
 */
@RestController
@RequestMapping("/api/template/template")
@RequiredArgsConstructor
public class TemplateController {

    private final EmailTemplateService emailTemplateService;

    /** 创建模板，自动设置 createTime、updateTime，version 默认为 1 */
    @PostMapping
    public Result<EmailTemplate> create(@RequestBody EmailTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        if (template.getVersion() == null) {
            template.setVersion(1);
        }
        emailTemplateService.save(template);
        return Result.ok(template);
    }

    /** 按 ID 查询模板，不存在则 404 */
    @GetMapping("/{id}")
    public Result<EmailTemplate> get(@PathVariable Long id) {
        EmailTemplate t = emailTemplateService.getById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        return Result.ok(t);
    }

    /** 查询当前租户下全部模板列表 */
    @GetMapping("/list")
    public Result<List<EmailTemplate>> list() {
        return Result.ok(emailTemplateService.list());
    }

    /** 全量更新模板，不存在则 404；仅更新传入字段，updateTime 自动刷新 */
    @PutMapping("/{id}")
    public Result<EmailTemplate> update(@PathVariable Long id, @RequestBody EmailTemplate template) {
        if (emailTemplateService.getById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        template.setId(id);
        template.setUpdateTime(LocalDateTime.now());
        emailTemplateService.updateById(template);
        return Result.ok(emailTemplateService.getById(id));
    }

    /** 按 ID 删除模板，幂等 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        emailTemplateService.removeById(id);
        return Result.ok();
    }
}
