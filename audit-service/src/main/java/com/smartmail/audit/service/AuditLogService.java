package com.smartmail.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartmail.audit.entity.AuditLog;

import java.util.List;

public interface AuditLogService extends IService<AuditLog> {

    List<AuditLog> listPage(String userId, int page, int size);
}
