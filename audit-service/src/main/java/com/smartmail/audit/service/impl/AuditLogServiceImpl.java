package com.smartmail.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.audit.entity.AuditLog;
import com.smartmail.audit.mapper.AuditLogMapper;
import com.smartmail.audit.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    @Override
    public List<AuditLog> listPage(String userId, int page, int size) {
        LambdaQueryWrapper<AuditLog> q = new LambdaQueryWrapper<>();
        if (userId != null && !userId.isBlank()) {
            q.eq(AuditLog::getUserId, userId);
        }
        q.orderByDesc(AuditLog::getCreateTime);
        return page(new Page<>(page, size), q).getRecords();
    }
}
