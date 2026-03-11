package com.smartmail.contact.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartmail.contact.entity.ContactGroup;

import java.util.List;

public interface ContactGroupService extends IService<ContactGroup> {

    /** 按租户查询分组列表 */
    List<ContactGroup> listByTenant(String tenantId);

    /** 按 ID 与租户查询，用于 get/update/成员操作 时租户隔离 */
    ContactGroup getByIdAndTenant(Long id, String tenantId);

    /** 按租户内序号 local_id 与租户查询 */
    ContactGroup getByLocalIdAndTenant(Integer localId, String tenantId);

    /** 分配当前租户下可用的 local_id（从 1 起，填补删除后的空缺） */
    int nextLocalIdForTenant(String tenantId);

    /** 按租户内序号与租户删除，幂等 */
    void removeByLocalIdAndTenant(Integer localId, String tenantId);

    /** 按 ID 与租户删除，幂等 */
    void removeByIdAndTenant(Long id, String tenantId);
}
