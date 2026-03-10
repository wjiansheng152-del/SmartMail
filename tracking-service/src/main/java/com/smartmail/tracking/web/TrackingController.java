package com.smartmail.tracking.web;

import com.smartmail.common.result.Result;
import com.smartmail.tracking.entity.TrackingEvent;
import com.smartmail.tracking.service.TrackingStatsService;
import com.smartmail.tracking.service.TrackingWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 打开率/点击率追踪：提供像素埋点、点击重定向与活动统计查询。
 * <p>
 * 邮件中嵌入像素 URL 可统计打开；点击链接经本接口重定向并记录点击；pixel/click 请求会写入 tracking_event，stats 据此汇总。
 * </p>
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingStatsService trackingStatsService;
    private final TrackingWriteService trackingWriteService;

    /** 1x1 透明 GIF 像素字节，用于打开追踪 */
    private static final byte[] PIXEL = new byte[] {
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, (byte)0x80, 0x00, 0x00, (byte)0xff, (byte)0xff, (byte)0xff, 0x00, 0x00, 0x00, 0x21, (byte)0xf9, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3b
    };

    /** 返回 1x1 透明 GIF，用于邮件打开追踪；若传 campaignId 则写入一条 open 事件到 tracking_event */
    @GetMapping(value = "/pixel/{deliveryId}", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<byte[]> pixel(
            @PathVariable Long deliveryId,
            @RequestParam(required = false) Long campaignId) {
        if (campaignId != null) {
            trackingWriteService.recordOpen(campaignId, deliveryId);
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_GIF).body(PIXEL);
    }

    /** 点击追踪：302 重定向到 url，若传 campaignId 则写入一条 click 事件到 tracking_event */
    @GetMapping("/click/{deliveryId}")
    public ResponseEntity<Void> click(
            @PathVariable Long deliveryId,
            @RequestParam String url,
            @RequestParam(required = false) Long campaignId) {
        if (campaignId != null) {
            trackingWriteService.recordClick(campaignId, deliveryId, url);
        }
        return ResponseEntity.status(302).header("Location", url).build();
    }

    /** 查询指定活动的追踪统计（打开数、点击数等） */
    @GetMapping("/stats/{campaignId}")
    public Result<Map<String, Object>> stats(@PathVariable Long campaignId) {
        return Result.ok(trackingStatsService.getCampaignStats(campaignId));
    }
}
