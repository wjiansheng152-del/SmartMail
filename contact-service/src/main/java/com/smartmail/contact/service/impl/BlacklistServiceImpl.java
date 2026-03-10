package com.smartmail.contact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.contact.entity.Blacklist;
import com.smartmail.contact.mapper.BlacklistMapper;
import com.smartmail.contact.service.BlacklistService;
import org.springframework.stereotype.Service;

@Service
public class BlacklistServiceImpl extends ServiceImpl<BlacklistMapper, Blacklist> implements BlacklistService {

    @Override
    public boolean isBlacklisted(String email) {
        return count(new LambdaQueryWrapper<Blacklist>().eq(Blacklist::getEmail, email)) > 0;
    }
}
