package com.ashraf.rider.controller;

import com.ashraf.rider.dto.RiderLocationPing;
import com.ashraf.rider.service.RiderLocationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class RiderLocationSocketController {

    private final RiderLocationService riderLocationService;
    private final SimpMessagingTemplate messagingTemplate;

    public RiderLocationSocketController(RiderLocationService riderLocationService,
                                         SimpMessagingTemplate messagingTemplate) {
        this.riderLocationService = riderLocationService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/rider/location")
    public void onLocationPing(RiderLocationPing ping, SimpMessageHeaderAccessor headerAccessor) {
        Object riderId = headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("riderId") : null;
        if (riderId == null) return; // connected, but not as a rider — ignore

        riderLocationService.updateLocation((Long) riderId, ping.latitude(), ping.longitude());
        if (ping.orderPublicId() != null) {
            messagingTemplate.convertAndSend("/topic/delivery/" + ping.orderPublicId() + "/location", ping);
        }
    }
}