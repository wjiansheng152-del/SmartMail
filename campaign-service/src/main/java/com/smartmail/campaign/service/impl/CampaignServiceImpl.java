package com.smartmail.campaign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartmail.campaign.entity.Campaign;
import com.smartmail.campaign.entity.CampaignAbAssignment;
import com.smartmail.campaign.mapper.CampaignAbAssignmentMapper;
import com.smartmail.campaign.mapper.CampaignMapper;
import com.smartmail.campaign.service.CampaignService;
import com.smartmail.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 营销活动服务实现：删除时在应用层先清理 campaign_ab_assignment 再删 campaign；
 * 列表支持按创建人过滤，实现多用户数据隔离。
 */
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl extends ServiceImpl<CampaignMapper, Campaign> implements CampaignService {

    private final CampaignAbAssignmentMapper campaignAbAssignmentMapper;

    @Override
    public List<Campaign> listByCreatedBy(Long createdBy) {
        List<Campaign> result = createdBy == null ? list() : list(new LambdaQueryWrapper<Campaign>().eq(Campaign::getCreatedBy, createdBy));
        // #region agent log
        try {
            String tenantId = TenantContext.getTenantId();
            String ids = result.stream().map(c -> String.valueOf(c.getId())).collect(Collectors.joining(","));
            String logPath = System.getenv("DEBUG_LOG_PATH") != null ? System.getenv("DEBUG_LOG_PATH") : "/app/debug-63f54d.log";
            String line = "{\"sessionId\":\"63f54d\",\"location\":\"CampaignServiceImpl.listByCreatedBy\",\"message\":\"campaign list\",\"data\":{\"tenantId\":\"" + (tenantId != null ? tenantId : "null") + "\",\"createdBy\":" + (createdBy != null ? createdBy : "null") + ",\"size\":" + result.size() + ",\"ids\":\"" + ids + "\"},\"timestamp\":" + System.currentTimeMillis() + ",\"hypothesisId\":\"H3\"}\n";
            Files.write(Paths.get(logPath), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) { /* ignore */ }
        // #endregion
        return result;
    }

    @Override
    public void removeCampaign(Long id) {
        campaignAbAssignmentMapper.delete(
                new LambdaQueryWrapper<CampaignAbAssignment>()
                        .eq(CampaignAbAssignment::getCampaignId, id));
        removeById(id);
    }

    @Override
    public int nextLocalIdForCreatedBy(Long createdBy) {
        if (createdBy == null) {
            return 1;
        }
        List<Campaign> list = list(new LambdaQueryWrapper<Campaign>()
                .eq(Campaign::getCreatedBy, createdBy)
                .orderByAsc(Campaign::getLocalId));
        int next = 1;
        for (Campaign c : list) {
            Integer lid = c.getLocalId();
            if (lid != null && lid == next) {
                next++;
            } else if (lid == null || lid > next) {
                return next;
            }
        }
        return next;
    }

    @Override
    public Campaign getByLocalIdAndCreatedBy(Integer localId, Long createdBy) {
        if (localId == null || createdBy == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<Campaign>()
                .eq(Campaign::getLocalId, localId)
                .eq(Campaign::getCreatedBy, createdBy));
    }

    @Override
    public void removeByLocalIdAndCreatedBy(Integer localId, Long createdBy) {
        Campaign c = getByLocalIdAndCreatedBy(localId, createdBy);
        if (c != null) {
            removeCampaign(c.getId());
        }
    }
}
