package com.example.noti_idempo.cache;

import com.example.noti_idempo.domain.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class CacheKeyGenerator {
	
	private static final String KEY_PREFIX = "notification";
	private static final String KEY_SEPARATOR = ":";
	
	public String generateKey(Long userId, NotificationType notificationType) {
		return KEY_PREFIX + KEY_SEPARATOR + notificationType.name() + KEY_SEPARATOR + userId;
	}
}

