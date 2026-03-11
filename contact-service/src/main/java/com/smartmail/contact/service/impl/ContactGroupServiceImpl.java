package com.smartmail.contact.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.contact.entity.ContactGroup;
import com.smartmail.contact.mapper.ContactGroupMapper;
import com.smartmail.contact.service.ContactGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.BitSet;
import java.util.List;

/**
 * 客户分组服务实现；列表与单条操作均按租户隔离。
 */
@Service
public class ContactGroupServiceImpl extends ServiceImpl<ContactGroupMapper, ContactGroup> implements ContactGroupService {

    @Override
    public List<ContactGroup> listByTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        return lambdaQuery().eq(ContactGroup::getTenantId, tenantId).list();
    }

    @Override
    public ContactGroup getByIdAndTenant(Long id, String tenantId) {
        if (id == null || tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return lambdaQuery().eq(ContactGroup::getId, id).eq(ContactGroup::getTenantId, tenantId).one();
    }

    @Override
    public ContactGroup getByLocalIdAndTenant(Integer localId, String tenantId) {
        if (localId == null || tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return lambdaQuery().eq(ContactGroup::getLocalId, localId).eq(ContactGroup::getTenantId, tenantId).one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int nextLocalIdForTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        List<ContactGroup> list = lambdaQuery().eq(ContactGroup::getTenantId, tenantId).select(ContactGroup::getLocalId).list();
        BitSet set = new BitSet();
        for (ContactGroup g : list) {
            if (g.getLocalId() != null && g.getLocalId() > 0) {
                set.set(g.getLocalId());
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
        remove(lambdaQuery().eq(ContactGroup::getLocalId, localId).eq(ContactGroup::getTenantId, tenantId));
    }

    @Override
    public void removeByIdAndTenant(Long id, String tenantId) {
        if (id == null || tenantId == null || tenantId.isBlank()) {
            return;
        }
        remove(lambdaQuery().eq(ContactGroup::getId, id).eq(ContactGroup::getTenantId, tenantId));
    }
}
