package com.smartmail.contact.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartmail.contact.entity.Contact;
import com.smartmail.contact.web.dto.ContactUpdateRequest;

public interface ContactService extends IService<Contact> {

    IPage<Contact> pageList(int page, int size, Long groupId);

    /** 按 ID 与租户查询，用于单条 get/update 时租户隔离 */
    Contact getByIdAndTenant(Long id, String tenantId);

    /** 按租户内序号 local_id 与租户查询（API 层 id 即 local_id） */
    Contact getByLocalIdAndTenant(Integer localId, String tenantId);

    /** 分配当前租户下可用的 local_id（从 1 起，填补删除后的空缺） */
    int nextLocalIdForTenant(String tenantId);

    /** 按租户内序号与租户删除，不存在也视为成功（幂等） */
    void removeByLocalIdAndTenant(Integer localId, String tenantId);

    /** 按 ID 与租户删除，不存在也视为成功（幂等） */
    void removeByIdAndTenant(Long id, String tenantId);

    /**
     * 根据 id 更新客户信息（email、name、mobile），并刷新 updateTime。
     * <p>
     * 客户不存在抛 404；若新邮箱已被其他客户使用则抛 409。
     * </p>
     *
     * @param id      客户主键
     * @param request 更新内容
     * @return 更新后的客户实体
     */
    Contact update(Long id, ContactUpdateRequest request);

    /** 按租户内序号 local_id 更新（API 层 id 即 local_id） */
    Contact updateByLocalId(Integer localId, ContactUpdateRequest request);
}
