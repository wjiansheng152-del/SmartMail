package com.smartmail.template.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.template.entity.EmailTemplate;
import com.smartmail.template.mapper.EmailTemplateMapper;
import com.smartmail.template.service.EmailTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.BitSet;
import java.util.List;

/**
 * 邮件模板服务实现；列表与单条操作均按租户隔离。
 */
@Service
public class EmailTemplateServiceImpl extends ServiceImpl<EmailTemplateMapper, EmailTemplate> implements EmailTemplateService {

    @Override
    public List<EmailTemplate> listByTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        return lambdaQuery().eq(EmailTemplate::getTenantId, tenantId).list();
    }

    @Override
    public EmailTemplate getByIdAndTenant(Long id, String tenantId) {
        if (id == null || tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return lambdaQuery().eq(EmailTemplate::getId, id).eq(EmailTemplate::getTenantId, tenantId).one();
    }

    @Override
    public EmailTemplate getByLocalIdAndTenant(Integer localId, String tenantId) {
        if (localId == null || tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return lambdaQuery().eq(EmailTemplate::getLocalId, localId).eq(EmailTemplate::getTenantId, tenantId).one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int nextLocalIdForTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        List<EmailTemplate> list = lambdaQuery().eq(EmailTemplate::getTenantId, tenantId).select(EmailTemplate::getLocalId).list();
        BitSet set = new BitSet();
        for (EmailTemplate t : list) {
            if (t.getLocalId() != null && t.getLocalId() > 0) {
                set.set(t.getLocalId());
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
        remove(lambdaQuery().eq(EmailTemplate::getLocalId, localId).eq(EmailTemplate::getTenantId, tenantId));
    }

    @Override
    public void removeByIdAndTenant(Long id, String tenantId) {
        if (id == null || tenantId == null || tenantId.isBlank()) {
            return;
        }
        remove(lambdaQuery().eq(EmailTemplate::getId, id).eq(EmailTemplate::getTenantId, tenantId));
    }
}
