package com.smartmail.contact.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartmail.contact.entity.Contact;

public interface ContactService extends IService<Contact> {

    IPage<Contact> pageList(int page, int size, Long groupId);
}
