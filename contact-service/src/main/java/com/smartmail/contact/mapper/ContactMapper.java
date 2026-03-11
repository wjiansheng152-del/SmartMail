package com.smartmail.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartmail.contact.entity.Contact;
import org.apache.ibatis.annotations.Param;

public interface ContactMapper extends BaseMapper<Contact> {

    IPage<Contact> selectPageByGroupId(Page<Contact> page, @Param("groupId") Long groupId, @Param("tenantId") String tenantId);
}
