package com.smartmail.contact.service;

import java.util.List;

/**
 * 分组-客户关联服务：将客户加入分组等操作。
 */
public interface ContactGroupMemberService {

    /**
     * 将指定客户加入指定分组。
     * <p>
     * 若该客户已在分组中则幂等返回；分组或客户不存在时抛出 BizException(NOT_FOUND)。
     * </p>
     *
     * @param groupId   分组 id，须存在
     * @param contactId 客户 id，须存在
     */
    void addToGroup(Long groupId, Long contactId);

    /**
     * 批量将客户加入指定分组。
     * <p>
     * 逐个处理：分组不存在则抛 404；任一客户不存在则抛 404；已在分组中的跳过（幂等），否则插入。
     * </p>
     *
     * @param groupId    分组 id，须存在
     * @param contactIds 客户 id 列表，须存在且非空
     */
    void addToGroupBatch(Long groupId, List<Long> contactIds);

    /**
     * 从分组中移除指定客户（删除 contact_group_member 关联记录）。
     * <p>
     * 若该客户本就不在该分组中，视为幂等，仍返回成功。
     * </p>
     *
     * @param groupId   分组 id
     * @param contactId 客户 id
     */
    void removeFromGroup(Long groupId, Long contactId);

    /**
     * 批量从分组中移出客户（删除 contact_group_member 中 groupId + contactId 在列表中的记录）。
     * <p>
     * 本就不在组内的 contactId 视为幂等跳过；不校验分组或客户是否存在。
     * </p>
     *
     * @param groupId    分组 id
     * @param contactIds 客户 id 列表，非空
     */
    void removeFromGroupBatch(Long groupId, List<Long> contactIds);
}
