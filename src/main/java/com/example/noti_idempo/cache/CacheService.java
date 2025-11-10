package com.example.noti_idempo.cache;

public interface CacheService {
	
	boolean exists(String key);
	
	void set(String key, int ttlSeconds);
	
	void delete(String key);
	
	/**
	 * 키가 존재하지 않을 때만 값을 저장하는 원자적 연산
	 * @param key 캐시 키
	 * @param ttlSeconds TTL (초)
	 * @return 저장 성공 여부 (키가 이미 존재하면 false)
	 */
	boolean add(String key, int ttlSeconds);
}

