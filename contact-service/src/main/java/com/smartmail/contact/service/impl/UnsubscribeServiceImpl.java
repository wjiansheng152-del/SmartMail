package com.smartmail.contact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.contact.entity.Unsubscribe;
import com.smartmail.contact.mapper.UnsubscribeMapper;
import com.smartmail.contact.service.UnsubscribeService;
import org.springframework.stereotype.Service;

@Service
public class UnsubscribeServiceImpl extends ServiceImpl<UnsubscribeMapper, Unsubscribe> implements UnsubscribeService {

    @Override
    public boolean isUnsubscribed(String email) {
        return count(new LambdaQueryWrapper<Unsubscribe>().eq(Unsubscribe::getEmail, email)) > 0;
    }

    @Override
    public java.util.List<String> listAllEmails() {
        return list(new LambdaQueryWrapper<Unsubscribe>().select(Unsubscribe::getEmail))
                .stream().map(Unsubscribe::getEmail).toList();
    }
}
