package com.example.noti_idempo.config;

import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.ArcusClient;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty(name = "arcus.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ArcusConfig {

	@Value("${arcus.zookeeper.address:127.0.0.1:2181}")
	private String zookeeperAddress;

	@Value("${arcus.service.code:test}")
	private String serviceCode;

	private ArcusClient arcusClient;

	@Bean
	public ArcusClient arcusClient() {
		try {
			log.info("Arcus 클라이언트 초기화 시작: zookeeper={}, serviceCode={}", zookeeperAddress, serviceCode);
			
			// 초기화 재시도 로직
			int maxRetries = 3;
			for (int i = 0; i < maxRetries; i++) {
				try {
					arcusClient = ArcusClient.createArcusClient(
							zookeeperAddress,
							serviceCode,
							new ConnectionFactoryBuilder()
									.setOpTimeout(30000)
					);
					log.info("Arcus 클라이언트 초기화 완료");
					return arcusClient;
				} catch (Exception e) {
					if (i == maxRetries - 1) {
						throw e;
					}
					log.warn("Arcus 클라이언트 초기화 실패 (재시도 {}/{}): {}", i + 1, maxRetries, e.getMessage());
					Thread.sleep(2000);
				}
			}
			throw new RuntimeException("Arcus 클라이언트 초기화 실패: 최대 재시도 횟수 초과");
		} catch (Exception e) {
			log.error("Arcus 클라이언트 초기화 실패: {}", e.getMessage(), e);
			throw new RuntimeException("Arcus 클라이언트 초기화 실패: " + e.getMessage(), e);
		}
	}

	@PreDestroy
	public void shutdown() {
		if (arcusClient != null) {
			arcusClient.shutdown();
		}
	}
}

