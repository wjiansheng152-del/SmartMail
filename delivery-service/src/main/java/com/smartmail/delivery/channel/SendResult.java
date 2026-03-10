package com.smartmail.delivery.channel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendResult {

    private boolean success;
    private String messageId;
    private String errorMessage;
}
