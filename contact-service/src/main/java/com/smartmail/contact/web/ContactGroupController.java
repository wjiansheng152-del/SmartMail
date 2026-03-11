package com.smartmail.contact.web;

import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.contact.entity.ContactGroup;
import com.smartmail.contact.service.ContactGroupMemberService;
import com.smartmail.contact.service.ContactGroupService;
import com.smartmail.contact.web.dto.AddGroupMemberBatchRequest;
import com.smartmail.contact.web.dto.AddGroupMemberRequest;
import com.smartmail.contact.web.dto.ContactGroupListItem;
import com.smartmail.contact.web.dto.ContactGroupResponse;
import com.smartmail.contact.web.dto.RemoveGroupMemberBatchRequest;
import com.smartmail.common.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 客户分组管理：创建分组、按 ID 查询、全量列表、删除分组、将客户加入分组；分组用于客户筛选与营销定向。
 */
@RestController
@RequestMapping("/api/contact/group")
@RequiredArgsConstructor
public class ContactGroupController {

    private final ContactGroupService contactGroupService;
    private final ContactGroupMemberService contactGroupMemberService;

    /** 创建分组；id 为租户内连续序号（删除后可复用） */
    @PostMapping
    public Result<ContactGroupResponse> create(@RequestBody ContactGroup group) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        group.setTenantId(tenantId);
        group.setLocalId(contactGroupService.nextLocalIdForTenant(tenantId));
        group.setCreateTime(java.time.LocalDateTime.now());
        group.setUpdateTime(java.time.LocalDateTime.now());
        if (group.getRuleType() == null) group.setRuleType("static");
        contactGroupService.save(group);
        return Result.ok(ContactGroupResponse.from(group));
    }

    /** 按 id（租户内序号）查询分组，不存在则 404 */
    @GetMapping("/{id}")
    public Result<ContactGroupResponse> get(@PathVariable Integer id) {
        String tenantId = TenantContext.getTenantId();
        ContactGroup g = contactGroupService.getByLocalIdAndTenant(id, tenantId != null ? tenantId : "default");
        if (g == null) {
            throw new com.smartmail.common.exception.BizException(ErrorCode.NOT_FOUND, "分组不存在");
        }
        return Result.ok(ContactGroupResponse.from(g));
    }

    /** 查询当前租户下全部分组列表；每项 id 为租户内序号 */
    @GetMapping("/list")
    public Result<List<ContactGroupListItem>> list() {
        String tenantId = TenantContext.getTenantId();
        List<ContactGroup> list = contactGroupService.listByTenant(tenantId);
        List<ContactGroupListItem> items = IntStream.range(0, list.size())
                .mapToObj(i -> {
                    ContactGroup g = list.get(i);
                    ContactGroupListItem item = new ContactGroupListItem();
                    BeanUtils.copyProperties(g, item);
                    item.setId(g.getLocalId() != null ? g.getLocalId().longValue() : null);
                    item.setSequence(i + 1);
                    return item;
                })
                .toList();
        return Result.ok(items);
    }

    /**
     * 将客户加入分组：请求体传 contactId，重复加入同一分组视为幂等返回 200；
     * 分组或客户不存在时返回 404。
     */
    @PostMapping("/{groupId}/member")
    public Result<Void> addMember(@PathVariable Long groupId, @Valid @RequestBody AddGroupMemberRequest request) {
        contactGroupMemberService.addToGroup(groupId, request.getContactId());
        return Result.ok();
    }

    /**
     * 批量将客户加入分组：请求体传 contactIds 数组，重复加入同一分组视为幂等跳过；
     * 分组不存在或任一客户不存在时返回 404。
     */
    @PostMapping("/{groupId}/member/batch")
    public Result<Void> addMemberBatch(@PathVariable Long groupId, @Valid @RequestBody AddGroupMemberBatchRequest request) {
        contactGroupMemberService.addToGroupBatch(groupId, request.getContactIds());
        return Result.ok();
    }

    /**
     * 从分组中移除某个客户：删除该分组下该客户的关联记录，若本就不在组内则幂等返回 200。
     */
    @DeleteMapping("/{groupId}/member/{contactId}")
    public Result<Void> removeMember(@PathVariable Long groupId, @PathVariable Long contactId) {
        contactGroupMemberService.removeFromGroup(groupId, contactId);
        return Result.ok();
    }

    /**
     * 批量从分组中移出客户：请求体传 contactIds，删除该分组下这些客户的关联记录，幂等。
     */
    @DeleteMapping("/{groupId}/member/batch")
    public Result<Void> removeMemberBatch(
            @PathVariable Long groupId,
            @Valid @RequestBody RemoveGroupMemberBatchRequest request) {
        contactGroupMemberService.removeFromGroupBatch(groupId, request.getContactIds());
        return Result.ok();
    }

    /** 按 id（租户内序号）删除分组，幂等 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        String tenantId = TenantContext.getTenantId();
        contactGroupService.removeByLocalIdAndTenant(id, tenantId != null ? tenantId : "default");
        return Result.ok();
    }
}
