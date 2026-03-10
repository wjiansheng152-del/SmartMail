package com.smartmail.contact.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartmail.contact.entity.Blacklist;

public interface BlacklistService extends IService<Blacklist> {

    boolean isBlacklisted(String email);
}
