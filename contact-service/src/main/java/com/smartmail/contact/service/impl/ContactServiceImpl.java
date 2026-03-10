package com.smartmail.contact.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.contact.entity.Contact;
import com.smartmail.contact.mapper.ContactMapper;
import com.smartmail.contact.service.ContactService;
import org.springframework.stereotype.Service;

@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements ContactService {

    @Override
    public IPage<Contact> pageList(int page, int size, Long groupId) {
        Page<Contact> p = new Page<>(page, size);
        if (groupId == null) {
            return page(p);
        }
        return baseMapper.selectPageByGroupId(p, groupId);
    }
}
