package com.smartmail.contact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.tenant.TenantContext;
import com.smartmail.contact.entity.ContactGroup;
import com.smartmail.contact.entity.ContactGroupMember;
import com.smartmail.contact.mapper.ContactGroupMemberMapper;
import com.smartmail.contact.service.ContactGroupMemberService;
import com.smartmail.contact.service.ContactGroupService;
import com.smartmail.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分组-客户关联服务实现：校验分组与客户存在性，幂等地将客户加入分组。
 */
@Service
@RequiredArgsConstructor
public class ContactGroupMemberServiceImpl implements ContactGroupMemberService {

    private final ContactGroupMemberMapper contactGroupMemberMapper;
    private final ContactGroupService contactGroupService;
    private final ContactService contactService;

    @Override
    public void addToGroup(Long groupId, Long contactId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        ContactGroup group = contactGroupService.getByLocalIdAndTenant(groupId != null ? groupId.intValue() : null, tenantId);
        if (group == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分组不存在");
        }
        com.smartmail.contact.entity.Contact contact = contactService.getByLocalIdAndTenant(contactId != null ? contactId.intValue() : null, tenantId);
        if (contact == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        long internalGroupId = group.getId();
        long internalContactId = contact.getId();
        ContactGroupMember existing = contactGroupMemberMapper.selectOne(
                new LambdaQueryWrapper<ContactGroupMember>()
                        .eq(ContactGroupMember::getGroupId, internalGroupId)
                        .eq(ContactGroupMember::getContactId, internalContactId));
        if (existing != null) return;
        ContactGroupMember member = new ContactGroupMember();
        member.setGroupId(internalGroupId);
        member.setContactId(internalContactId);
        member.setCreateTime(LocalDateTime.now());
        contactGroupMemberMapper.insert(member);
    }

    @Override
    public void addToGroupBatch(Long groupId, List<Long> contactIds) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        ContactGroup group = contactGroupService.getByLocalIdAndTenant(groupId != null ? groupId.intValue() : null, tenantId);
        if (group == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分组不存在");
        }
        long internalGroupId = group.getId();
        for (Long contactId : contactIds) {
            com.smartmail.contact.entity.Contact contact = contactService.getByLocalIdAndTenant(contactId != null ? contactId.intValue() : null, tenantId);
            if (contact == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "客户不存在：id=" + contactId);
            }
            long internalContactId = contact.getId();
            ContactGroupMember existing = contactGroupMemberMapper.selectOne(
                    new LambdaQueryWrapper<ContactGroupMember>()
                            .eq(ContactGroupMember::getGroupId, internalGroupId)
                            .eq(ContactGroupMember::getContactId, internalContactId));
            if (existing != null) continue;
            ContactGroupMember member = new ContactGroupMember();
            member.setGroupId(internalGroupId);
            member.setContactId(internalContactId);
            member.setCreateTime(LocalDateTime.now());
            contactGroupMemberMapper.insert(member);
        }
    }

    @Override
    public void removeFromGroup(Long groupId, Long contactId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        ContactGroup group = contactGroupService.getByLocalIdAndTenant(groupId != null ? groupId.intValue() : null, tenantId);
        if (group == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分组不存在");
        }
        com.smartmail.contact.entity.Contact contact = contactService.getByLocalIdAndTenant(contactId != null ? contactId.intValue() : null, tenantId);
        if (contact == null) return;
        contactGroupMemberMapper.delete(
                new LambdaQueryWrapper<ContactGroupMember>()
                        .eq(ContactGroupMember::getGroupId, group.getId())
                        .eq(ContactGroupMember::getContactId, contact.getId()));
    }

    @Override
    public void removeFromGroupBatch(Long groupId, List<Long> contactIds) {
        if (contactIds == null || contactIds.isEmpty()) {
            return;
        }
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        final String tenant = tenantId;
        ContactGroup group = contactGroupService.getByLocalIdAndTenant(groupId != null ? groupId.intValue() : null, tenant);
        if (group == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分组不存在");
        }
        List<Long> internalContactIds = contactIds.stream()
                .map(cid -> {
                    com.smartmail.contact.entity.Contact c = contactService.getByLocalIdAndTenant(cid != null ? cid.intValue() : null, tenant);
                    return c != null ? c.getId() : null;
                })
                .filter(id -> id != null)
                .toList();
        if (internalContactIds.isEmpty()) return;
        contactGroupMemberMapper.delete(
                new LambdaQueryWrapper<ContactGroupMember>()
                        .eq(ContactGroupMember::getGroupId, group.getId())
                        .in(ContactGroupMember::getContactId, internalContactIds));
    }
}
