package com.smartmail.contact.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartmail.contact.entity.Unsubscribe;

import java.util.List;

public interface UnsubscribeService extends IService<Unsubscribe> {

    boolean isUnsubscribed(String email);

    /** 返回当前租户下所有已退订邮箱列表，供发送前批量过滤 */
    List<String> listAllEmails();
}
