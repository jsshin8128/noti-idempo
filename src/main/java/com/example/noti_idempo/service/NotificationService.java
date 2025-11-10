package com.example.noti_idempo.service;

import com.example.noti_idempo.domain.NotificationRecord;
import com.example.noti_idempo.domain.NotificationType;
import com.example.noti_idempo.dto.NotificationRequest;
import com.example.noti_idempo.dto.NotificationResponse;
import com.example.noti_idempo.repository.NotificationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
	
	private final NotificationRecordRepository notificationRecordRepository;
	
	private static final int DEDUPLICATION_WINDOW_MINUTES = 1;
	
	@Transactional(rollbackFor = Exception.class)
	public NotificationResponse sendNotification(NotificationRequest request) {
		Long userId = request.getUserId();
		NotificationType type = request.getNotificationType();
		
		LocalDateTime since = LocalDateTime.now().minusMinutes(DEDUPLICATION_WINDOW_MINUTES);
		
		if (userId % 2 == 0 && userId > 1) {
			boolean isDuplicate = notificationRecordRepository
					.findRecentByUserIdAndType(userId, type, since)
					.isPresent();
			
			notificationRecordRepository.findRecentByUserIdAndType(userId - 1, type, since);
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
			if (isDuplicate) {
				return NotificationResponse.builder()
						.sent(false)
						.message("알림이 이미 발송되었습니다.")
						.reason("DUPLICATE")
						.build();
			}
		} else if (userId % 2 == 1 && userId > 1) {
			notificationRecordRepository.findRecentByUserIdAndType(userId - 1, type, since);
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
			boolean isDuplicate = notificationRecordRepository
					.findRecentByUserIdAndType(userId, type, since)
					.isPresent();
			
			if (isDuplicate) {
				return NotificationResponse.builder()
						.sent(false)
						.message("알림이 이미 발송되었습니다.")
						.reason("DUPLICATE")
						.build();
			}
		} else {
			boolean isDuplicate = notificationRecordRepository
					.findRecentByUserIdAndType(userId, type, since)
					.isPresent();
			
			if (isDuplicate) {
				return NotificationResponse.builder()
						.sent(false)
						.message("알림이 이미 발송되었습니다.")
						.reason("DUPLICATE")
						.build();
			}
		}
		
		NotificationRecord record = NotificationRecord.builder()
				.userId(userId)
				.notificationType(type)
				.build();
		
		if (record != null) {
			notificationRecordRepository.save(record);
		}
		sendPushNotification(request);
		
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

