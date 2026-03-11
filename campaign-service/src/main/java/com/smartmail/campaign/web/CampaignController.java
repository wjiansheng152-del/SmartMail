package com.smartmail.campaign.web;

import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.campaign.entity.Campaign;
import com.smartmail.campaign.service.CampaignService;
import com.smartmail.campaign.web.dto.CampaignListItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 营销活动管理：创建活动、按 ID 查询、全量列表；活动状态默认 draft，可配合调度与投递使用。
 */
@RestController
@RequestMapping("/api/campaign/campaign")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    /** 创建活动：分配 local_id（按 createdBy 连续可复用），设置 createTime/updateTime/createdBy，status 默认 draft */
    @PostMapping
    public Result<Campaign> create(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestBody Campaign campaign) {
        Long createdBy = null;
        if (userIdStr != null && !userIdStr.isBlank()) {
            try {
                createdBy = Long.parseLong(userIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        campaign.setCreatedBy(createdBy);
        campaign.setCreateTime(LocalDateTime.now());
        campaign.setUpdateTime(LocalDateTime.now());
        if (campaign.getStatus() == null) campaign.setStatus("draft");
        if (createdBy != null) {
            campaign.setLocalId(campaignService.nextLocalIdForCreatedBy(createdBy));
        } else {
            campaign.setLocalId(1);
        }
        campaignService.save(campaign);
        return Result.ok(campaign);
    }

    /** 按 local_id 查询活动（需 X-User-Id），不存在则 404 */
    @GetMapping("/{id}")
    public Result<Campaign> get(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long createdBy = parseCreatedBy(userIdStr);
        Campaign c = createdBy != null
                ? campaignService.getByLocalIdAndCreatedBy(id, createdBy)
                : campaignService.getById(id != null ? id.longValue() : null);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return Result.ok(c);
    }

    /** 按 local_id 全量更新活动（需 X-User-Id）；createdBy/localId/createTime 以原记录为准 */
    @PutMapping("/{id}")
    public Result<Campaign> update(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @RequestBody Campaign campaign) {
        Long createdBy = parseCreatedBy(userIdStr);
        Campaign existing = createdBy != null
                ? campaignService.getByLocalIdAndCreatedBy(id, createdBy)
                : campaignService.getById(id != null ? id.longValue() : null);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        campaign.setId(existing.getId());
        campaign.setLocalId(existing.getLocalId());
        campaign.setCreateTime(existing.getCreateTime());
        campaign.setCreatedBy(existing.getCreatedBy());
        campaign.setUpdateTime(LocalDateTime.now());
        campaignService.updateById(campaign);
        return Result.ok(campaignService.getById(existing.getId()));
    }

    /** 查询活动列表：若带 X-User-Id 则仅返回该用户创建的活动；每项 id 为 local_id，sequence 为展示序号（1 起） */
    @GetMapping("/list")
    public Result<List<CampaignListItem>> list(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long createdBy = parseCreatedBy(userIdStr);
        List<Campaign> list = campaignService.listByCreatedBy(createdBy);
        List<CampaignListItem> items = IntStream.range(0, list.size())
                .mapToObj(i -> {
                    Campaign c = list.get(i);
                    CampaignListItem item = new CampaignListItem();
                    BeanUtils.copyProperties(c, item);
                    item.setId(c.getLocalId() != null ? c.getLocalId().longValue() : c.getId());
                    item.setSequence(i + 1);
                    return item;
                })
                .toList();
        return Result.ok(items);
    }

    /** 按 local_id 删除营销活动（需 X-User-Id）；不存在也返回 200（幂等）。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long createdBy = parseCreatedBy(userIdStr);
        if (createdBy != null) {
            campaignService.removeByLocalIdAndCreatedBy(id, createdBy);
        } else {
            campaignService.removeCampaign(id != null ? id.longValue() : null);
        }
        return Result.ok();
    }

    private Long parseCreatedBy(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank()) return null;
        try {
            return Long.parseLong(userIdStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
