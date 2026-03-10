package com.smartmail.contact.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.contact.entity.ContactGroup;
import com.smartmail.contact.mapper.ContactGroupMapper;
import com.smartmail.contact.service.ContactGroupService;
import org.springframework.stereotype.Service;

@Service
public class ContactGroupServiceImpl extends ServiceImpl<ContactGroupMapper, ContactGroup> implements ContactGroupService {
}
