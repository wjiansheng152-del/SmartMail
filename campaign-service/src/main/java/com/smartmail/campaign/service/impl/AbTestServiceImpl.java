package com.smartmail.campaign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmail.campaign.entity.CampaignAbAssignment;
import com.smartmail.campaign.mapper.CampaignAbAssignmentMapper;
import com.smartmail.campaign.service.AbTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AbTestServiceImpl implements AbTestService {

    private final CampaignAbAssignmentMapper assignmentMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public long getTemplateIdForContact(long campaignId, long contactId, long mainTemplateId, String abConfigJson) {
        if (abConfigJson == null || abConfigJson.isBlank()) {
            return mainTemplateId;
        }
        CampaignAbAssignment existing = assignmentMapper.selectOne(
                new LambdaQueryWrapper<CampaignAbAssignment>()
                        .eq(CampaignAbAssignment::getCampaignId, campaignId)
                        .eq(CampaignAbAssignment::getContactId, contactId));
        if (existing != null) {
            return resolveTemplateId(mainTemplateId, abConfigJson, existing.getVariant());
        }
        try {
            JsonNode node = objectMapper.readTree(abConfigJson);
            long templateIdA = node.has("templateIdA") ? node.get("templateIdA").asLong() : mainTemplateId;
            long templateIdB = node.has("templateIdB") ? node.get("templateIdB").asLong() : mainTemplateId;
            int ratio = node.has("ratio") ? node.get("ratio").asInt(50) : 50;
            String variant = ThreadLocalRandom.current().nextInt(100) < ratio ? "A" : "B";
            CampaignAbAssignment assignment = new CampaignAbAssignment();
            assignment.setCampaignId(campaignId);
            assignment.setContactId(contactId);
            assignment.setVariant(variant);
            assignment.setCreateTime(LocalDateTime.now());
            assignmentMapper.insert(assignment);
            return "A".equals(variant) ? templateIdA : templateIdB;
        } catch (Exception e) {
            return mainTemplateId;
        }
    }

    private long resolveTemplateId(long mainTemplateId, String abConfigJson, String variant) {
        try {
            JsonNode node = objectMapper.readTree(abConfigJson);
            if ("A".equals(variant) && node.has("templateIdA")) {
                return node.get("templateIdA").asLong();
            }
            if ("B".equals(variant) && node.has("templateIdB")) {
                return node.get("templateIdB").asLong();
            }
        } catch (Exception ignored) {
        }
        return mainTemplateId;
    }
}
