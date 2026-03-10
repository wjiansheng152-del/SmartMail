package com.smartmail.delivery.channel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendRequest {

    private String to;
    private String subject;
    private String htmlBody;
    private String from;
    private String fromName;
    private String replyTo;
}
