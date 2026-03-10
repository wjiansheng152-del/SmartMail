package com.smartmail.template.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.template.entity.EmailTemplate;
import com.smartmail.template.mapper.EmailTemplateMapper;
import com.smartmail.template.service.EmailTemplateService;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateServiceImpl extends ServiceImpl<EmailTemplateMapper, EmailTemplate> implements EmailTemplateService {
}
