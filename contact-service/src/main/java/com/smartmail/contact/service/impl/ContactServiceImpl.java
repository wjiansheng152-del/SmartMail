package com.smartmail.contact.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.contact.entity.Contact;
import com.smartmail.contact.entity.ContactGroup;
import com.smartmail.contact.mapper.ContactMapper;
import com.smartmail.contact.service.ContactGroupService;
import com.smartmail.contact.service.ContactService;
import com.smartmail.common.tenant.TenantContext;
import com.smartmail.contact.web.dto.ContactUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements ContactService {

    private final ContactGroupService contactGroupService;

    public ContactServiceImpl(ContactGroupService contactGroupService) {
        this.contactGroupService = contactGroupService;
    }

    @Override
    public IPage<Contact> pageList(int page, int size, Long groupId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        Page<Contact> p = new Page<>(page, size);
        IPage<Contact> result;
        if (groupId == null) {
            result = lambdaQuery().eq(Contact::getTenantId, tenantId).page(p);
        } else {
            // 前端传入的 groupId 为分组的 local_id，需解析为内部主键再查 contact_group_member
            ContactGroup group = contactGroupService.getByLocalIdAndTenant(groupId.intValue(), tenantId);
            if (group == null) {
                result = new Page<>(page, size);
                ((Page<Contact>) result).setRecords(Collections.emptyList());
                ((Page<Contact>) result).setTotal(0);
            } else {
                result = baseMapper.selectPageByGroupId(p, group.getId(), tenantId);
            }
        }
        // #region agent log
        try {
            long total = result.getTotal();
            String ids = (result.getRecords() != null ? result.getRecords() : Collections.<Contact>emptyList()).stream().map(c -> String.valueOf(c.getId())).limit(5).collect(Collectors.joining(","));
            String logPath = System.getenv("DEBUG_LOG_PATH") != null ? System.getenv("DEBUG_LOG_PATH") : "/app/debug-63f54d.log";
            String line = "{\"sessionId\":\"63f54d\",\"location\":\"ContactServiceImpl.pageList\",\"message\":\"contact page\",\"data\":{\"tenantId\":\"" + (tenantId != null ? tenantId : "null") + "\",\"total\":" + total + ",\"ids\":\"" + ids + "\"},\"timestamp\":" + System.currentTimeMillis() + ",\"hypothesisId\":\"H1\"}\n";
            Files.write(Paths.get(logPath), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) { /* ignore */ }
        // #endregion
        return result;
    }

    @Override
    public Contact getByIdAndTenant(Long id, String tenantId) {
        if (id == null || tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return lambdaQuery().eq(Contact::getId, id).eq(Contact::getTenantId, tenantId).one();
    }

    @Override
    public Contact getByLocalIdAndTenant(Integer localId, String tenantId) {
        if (localId == null || tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return lambdaQuery().eq(Contact::getLocalId, localId).eq(Contact::getTenantId, tenantId).one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int nextLocalIdForTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        List<Contact> list = lambdaQuery().eq(Contact::getTenantId, tenantId).select(Contact::getLocalId).list();
        BitSet set = new BitSet();
        for (Contact c : list) {
            if (c.getLocalId() != null && c.getLocalId() > 0) {
                set.set(c.getLocalId());
            }
        }
        int next = 1;
        while (set.get(next)) {
            next++;
        }
        return next;
    }

    @Override
    public void removeByLocalIdAndTenant(Integer localId, String tenantId) {
        if (localId == null || tenantId == null || tenantId.isBlank()) {
            return;
        }
        remove(lambdaQuery().eq(Contact::getLocalId, localId).eq(Contact::getTenantId, tenantId));
    }

    @Override
    public void removeByIdAndTenant(Long id, String tenantId) {
        if (id == null || tenantId == null || tenantId.isBlank()) {
            return;
        }
        remove(lambdaQuery().eq(Contact::getId, id).eq(Contact::getTenantId, tenantId));
    }

    @Override
    public Contact update(Long id, ContactUpdateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        Contact contact = getByIdAndTenant(id, tenantId);
        if (contact == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        // 若邮箱有变更，校验是否与其他客户冲突
        if (!request.getEmail().equals(contact.getEmail())) {
            long count = lambdaQuery()
                    .eq(Contact::getTenantId, tenantId)
                    .eq(Contact::getEmail, request.getEmail())
                    .ne(Contact::getId, id)
                    .count();
            if (count > 0) {
                throw new BizException(ErrorCode.CONFLICT, "邮箱已被其他客户使用");
            }
        }
        contact.setEmail(request.getEmail());
        contact.setName(request.getName());
        contact.setMobile(request.getMobile());
        contact.setUpdateTime(LocalDateTime.now());
        updateById(contact);
        return contact;
    }

    @Override
    public Contact updateByLocalId(Integer localId, ContactUpdateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        Contact contact = getByLocalIdAndTenant(localId, tenantId);
        if (contact == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        if (!request.getEmail().equals(contact.getEmail())) {
            long count = lambdaQuery()
                    .eq(Contact::getTenantId, tenantId)
                    .eq(Contact::getEmail, request.getEmail())
                    .ne(Contact::getId, contact.getId())
                    .count();
            if (count > 0) {
                throw new BizException(ErrorCode.CONFLICT, "邮箱已被其他客户使用");
            }
        }
        contact.setEmail(request.getEmail());
        contact.setName(request.getName());
        contact.setMobile(request.getMobile());
        contact.setUpdateTime(LocalDateTime.now());
        updateById(contact);
        return contact;
    }
}
