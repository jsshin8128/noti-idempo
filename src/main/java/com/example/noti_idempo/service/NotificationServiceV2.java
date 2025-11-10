package com.example.noti_idempo.service;

import com.example.noti_idempo.cache.CacheKeyGenerator;
import com.example.noti_idempo.cache.CacheService;
import com.example.noti_idempo.domain.NotificationType;
import com.example.noti_idempo.dto.NotificationRequest;
import com.example.noti_idempo.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(CacheService.class)
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceV2 {
	
	private final CacheService cacheService;
	private final CacheKeyGenerator cacheKeyGenerator;
	
	@Value("${arcus.ttl:60}")
	private int cacheTtl;
	
	public NotificationResponse sendNotification(NotificationRequest request) {
		Long userId = request.getUserId();
		NotificationType type = request.getNotificationType();
		
		String cacheKey = cacheKeyGenerator.generateKey(userId, type);
		
		if (cacheService.exists(cacheKey)) {
			log.info("중복 알림 방지: userId={}, type={}", userId, type);
			return NotificationResponse.builder()
					.sent(false)
					.message("알림이 이미 발송되었습니다.")
					.reason("DUPLICATE")
					.build();
		}
		
		sendPushNotification(request);
		
		cacheService.set(cacheKey, cacheTtl);
		
		log.info("알림 발송 완료: userId={}, type={}", userId, type);
		return NotificationResponse.builder()
				.sent(true)
				.message("알림이 발송되었습니다.")
				.reason("SUCCESS")
				.build();
	}
	
	private void sendPushNotification(NotificationRequest request) {
		log.debug("푸시 알림 발송: userId={}, message={}", 
				request.getUserId(), request.getMessage());
	}
}

