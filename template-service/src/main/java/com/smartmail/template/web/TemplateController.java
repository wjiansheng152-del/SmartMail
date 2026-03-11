package com.smartmail.template.web;

import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.template.entity.EmailTemplate;
import com.smartmail.template.service.EmailTemplateService;
import com.smartmail.template.web.dto.TemplateListItem;
import com.smartmail.template.web.dto.TemplateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import com.smartmail.common.tenant.TenantContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 邮件模板管理：创建、查询、更新、删除模板，支持按 ID 查询与全量列表。
 */
@RestController
@RequestMapping("/api/template/template")
@RequiredArgsConstructor
public class TemplateController {

    private final EmailTemplateService emailTemplateService;

    /** 创建模板；id 为租户内连续序号（删除后可复用） */
    @PostMapping
    public Result<TemplateResponse> create(@RequestBody EmailTemplate template) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        template.setTenantId(tenantId);
        template.setLocalId(emailTemplateService.nextLocalIdForTenant(tenantId));
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        if (template.getVersion() == null) template.setVersion(1);
        emailTemplateService.save(template);
        return Result.ok(TemplateResponse.from(template));
    }

    /** 按 id（租户内序号）查询模板，不存在则 404 */
    @GetMapping("/{id}")
    public Result<TemplateResponse> get(@PathVariable Integer id) {
        String tenantId = TenantContext.getTenantId();
        EmailTemplate t = emailTemplateService.getByLocalIdAndTenant(id, tenantId != null ? tenantId : "default");
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        return Result.ok(TemplateResponse.from(t));
    }

    /** 查询当前租户下全部模板列表；每项 id 为租户内序号 */
    @GetMapping("/list")
    public Result<List<TemplateListItem>> list() {
        String tenantId = TenantContext.getTenantId();
        List<EmailTemplate> list = emailTemplateService.listByTenant(tenantId);
        List<TemplateListItem> items = IntStream.range(0, list.size())
                .mapToObj(i -> {
                    EmailTemplate t = list.get(i);
                    TemplateListItem item = new TemplateListItem();
                    BeanUtils.copyProperties(t, item);
                    item.setId(t.getLocalId() != null ? t.getLocalId().longValue() : null);
                    item.setSequence(i + 1);
                    return item;
                })
                .toList();
        return Result.ok(items);
    }

    /** 按 id（租户内序号）全量更新模板，不存在则 404 */
    @PutMapping("/{id}")
    public Result<TemplateResponse> update(@PathVariable Integer id, @RequestBody EmailTemplate template) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        EmailTemplate existing = emailTemplateService.getByLocalIdAndTenant(id, tenantId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        template.setId(existing.getId());
        template.setTenantId(tenantId);
        template.setLocalId(id);
        template.setUpdateTime(LocalDateTime.now());
        emailTemplateService.updateById(template);
        return Result.ok(TemplateResponse.from(emailTemplateService.getByLocalIdAndTenant(id, tenantId)));
    }

    /** 按 id（租户内序号）删除模板，幂等 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        String tenantId = TenantContext.getTenantId();
        emailTemplateService.removeByLocalIdAndTenant(id, tenantId != null ? tenantId : "default");
        return Result.ok();
    }
}
