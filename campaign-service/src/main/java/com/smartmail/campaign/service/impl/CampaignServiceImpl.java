package com.smartmail.campaign.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.campaign.entity.Campaign;
import com.smartmail.campaign.mapper.CampaignMapper;
import com.smartmail.campaign.service.CampaignService;
import org.springframework.stereotype.Service;

@Service
public class CampaignServiceImpl extends ServiceImpl<CampaignMapper, Campaign> implements CampaignService {
}
