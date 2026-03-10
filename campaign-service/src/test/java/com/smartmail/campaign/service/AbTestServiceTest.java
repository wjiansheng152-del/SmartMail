package com.smartmail.campaign.service;

import com.smartmail.campaign.entity.CampaignAbAssignment;
import com.smartmail.campaign.mapper.CampaignAbAssignmentMapper;
import com.smartmail.campaign.service.impl.AbTestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbTestServiceTest {

    @Mock
    private CampaignAbAssignmentMapper assignmentMapper;

    private AbTestService abTestService;

    @BeforeEach
    void setUp() {
        abTestService = new AbTestServiceImpl(assignmentMapper);
    }

    @Test
    void getTemplateIdWithoutAbConfigReturnsMainTemplate() {
        long main = 100L;
        assertEquals(main, abTestService.getTemplateIdForContact(1L, 1L, main, null));
        assertEquals(main, abTestService.getTemplateIdForContact(1L, 2L, main, ""));
    }

    @Test
    void getTemplateIdWithAbConfigReturnsEitherVariant() {
        when(assignmentMapper.selectOne(any())).thenReturn(null);
        when(assignmentMapper.insert(any(CampaignAbAssignment.class))).thenReturn(1);
        String config = "{\"templateIdA\":10,\"templateIdB\":20,\"ratio\":50}";
        long result = abTestService.getTemplateIdForContact(99L, 99L, 5L, config);
        assertTrue(result == 10L || result == 20L);
    }
}
