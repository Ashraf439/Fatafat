package com.ashraf.config;

import com.ashraf.core.entity.User;
import com.ashraf.core.repository.UserRepository;
import com.ashraf.rider.repository.DeliveryRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeliveryChannelInterceptor implements ChannelInterceptor {

    private static final Pattern DELIVERY_TOPIC = Pattern.compile("^/topic/delivery/([0-9a-fA-F-]{36})/location$");
    private static final Pattern JOB_TOPIC = Pattern.compile("^/topic/rider/(\\d+)/jobs$");

    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;

    public DeliveryChannelInterceptor(UserRepository userRepository, DeliveryRepository deliveryRepository) {
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) return message;

        String destination = accessor.getDestination();
        Map<String, Object> session = accessor.getSessionAttributes();
        Long userId = session != null ? (Long) session.get("userId") : null;
        if (destination == null || userId == null) {
            throw new MessagingException(message, "Not authorized");
        }

        Matcher delivery = DELIVERY_TOPIC.matcher(destination);
        if (delivery.matches() && !canWatchDelivery(userId, UUID.fromString(delivery.group(1)))) {
            throw new MessagingException(message, "Not authorized for this delivery");
        }

        Matcher job = JOB_TOPIC.matcher(destination);
        if (job.matches() && !isOwnJobTopic(userId, Long.valueOf(job.group(1)))) {
            throw new MessagingException(message, "Not authorized for this rider's jobs");
        }

        return message;
    }

    @Transactional(readOnly = true)
    protected boolean canWatchDelivery(Long userId, UUID orderPublicId) {
        return deliveryRepository.findByOrder_PublicId(orderPublicId)
                .map(d -> userId.equals(d.getOrder().getCustomer().getUser().getId())
                        || userId.equals(d.getRider().getUser().getId())
                        || userId.equals(d.getOrder().getRestaurant().getOwnerUser().getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    protected boolean isOwnJobTopic(Long userId, Long riderId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRider() != null && user.getRider().getId().equals(riderId);
    }
}