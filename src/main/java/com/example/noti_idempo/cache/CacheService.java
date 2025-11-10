package com.example.noti_idempo.cache;

public interface CacheService {
	
	boolean exists(String key);
	
	void set(String key, int ttlSeconds);
	
	void delete(String key);
}

