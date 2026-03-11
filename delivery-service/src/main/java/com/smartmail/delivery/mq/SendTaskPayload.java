package com.smartmail.delivery.mq;

import lombok.Data;

@Data
public class SendTaskPayload {

    /** 投递任务 ID，用于追踪 pixel/click 与状态回写 */
    private Long deliveryId;
    /** 活动 ID，用于追踪与状态汇总 */
    private Long campaignId;
    /** 批次 ID，用于回写 success_count/fail_count */
    private Long batchId;
    private Long contactId;
    private String to;
    private String subject;
    private String htmlBody;
    private String from;
    private String channel;
    private String tenantId;
    /** 活动创建人 ID，用于按用户 SMTP 发信；有值则优先用该用户的 smtp_config，否则用默认通道 */
    private Long smtpConfigUserId;
}
