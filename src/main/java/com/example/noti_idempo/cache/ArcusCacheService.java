package com.example.noti_idempo.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.ArcusClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ArcusClient.class)
@RequiredArgsConstructor
@Slf4j
public class ArcusCacheService implements CacheService {
	
	private final ArcusClient arcusClient;
	
	@Override
	public boolean exists(String key) {
		try {
			Object value = arcusClient.get(key);
			return value != null;
		} catch (Exception e) {
			log.warn("Arcus 조회 실패, fallback 처리: key={}, error={}", key, e.getMessage());
			return false;
		}
	}
	
	@Override
	public void set(String key, int ttlSeconds) {
		try {
			arcusClient.set(key, ttlSeconds, "1");
		} catch (Exception e) {
			log.warn("Arcus 저장 실패: key={}, error={}", key, e.getMessage());
		}
	}
	
	@Override
	public void delete(String key) {
		try {
			arcusClient.delete(key);
		} catch (Exception e) {
			log.warn("Arcus 삭제 실패: key={}, error={}", key, e.getMessage());
		}
	}
}

