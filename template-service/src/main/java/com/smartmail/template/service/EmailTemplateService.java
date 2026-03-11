package com.smartmail.template.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartmail.template.entity.EmailTemplate;

import java.util.List;

public interface EmailTemplateService extends IService<EmailTemplate> {

    /** 按租户查询模板列表，用于多租户隔离 */
    List<EmailTemplate> listByTenant(String tenantId);

    /** 按 ID 与租户查询，用于 get/update 时租户隔离 */
    EmailTemplate getByIdAndTenant(Long id, String tenantId);

    /** 按租户内序号 local_id 与租户查询 */
    EmailTemplate getByLocalIdAndTenant(Integer localId, String tenantId);

    /** 分配当前租户下可用的 local_id（从 1 起，填补删除后的空缺） */
    int nextLocalIdForTenant(String tenantId);

    /** 按租户内序号与租户删除，幂等 */
    void removeByLocalIdAndTenant(Integer localId, String tenantId);

    /** 按 ID 与租户删除，幂等 */
    void removeByIdAndTenant(Long id, String tenantId);
}
