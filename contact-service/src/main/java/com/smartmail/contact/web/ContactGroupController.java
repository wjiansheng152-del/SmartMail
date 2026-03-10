package com.smartmail.contact.web;

import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.contact.entity.ContactGroup;
import com.smartmail.contact.service.ContactGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户分组管理：创建分组、按 ID 查询、全量列表、删除分组；分组用于客户筛选与营销定向。
 */
@RestController
@RequestMapping("/api/contact/group")
@RequiredArgsConstructor
public class ContactGroupController {

    private final ContactGroupService contactGroupService;

    /** 创建分组，ruleType 默认 static，自动设置 createTime、updateTime */
    @PostMapping
    public Result<ContactGroup> create(@RequestBody ContactGroup group) {
        group.setCreateTime(java.time.LocalDateTime.now());
        group.setUpdateTime(java.time.LocalDateTime.now());
        if (group.getRuleType() == null) {
            group.setRuleType("static");
        }
        contactGroupService.save(group);
        return Result.ok(group);
    }

    /** 按 ID 查询分组，不存在则 404 */
    @GetMapping("/{id}")
    public Result<ContactGroup> get(@PathVariable Long id) {
        ContactGroup g = contactGroupService.getById(id);
        if (g == null) {
            throw new com.smartmail.common.exception.BizException(ErrorCode.NOT_FOUND, "分组不存在");
        }
        return Result.ok(g);
    }

    /** 查询当前租户下全部分组列表 */
    @GetMapping("/list")
    public Result<List<ContactGroup>> list() {
        return Result.ok(contactGroupService.list());
    }

    /** 按 ID 删除分组，幂等 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contactGroupService.removeById(id);
        return Result.ok();
    }
}
