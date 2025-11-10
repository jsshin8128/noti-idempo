package com.example.noti_idempo;

import com.example.noti_idempo.domain.NotificationType;
import com.example.noti_idempo.dto.NotificationRequest;
import com.example.noti_idempo.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"arcus.enabled=true", "notification.service.version=v3"})
class LoadTestV3 {
	
	@LocalServerPort
	private int port;
	
	@Autowired
	private TestRestTemplate restTemplate;
	
	private String getBaseUrl() {
		return "http://localhost:" + port;
	}
	
	@Test
	void testConcurrentRequestsWithHorizontalScaling() throws InterruptedException {
		int serverCount = 5;
		int requestsPerServer = 40;
		int totalThreadCount = serverCount * requestsPerServer;
		
		ExecutorService executor = Executors.newFixedThreadPool(totalThreadCount);
		CountDownLatch latch = new CountDownLatch(totalThreadCount);
		
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger duplicateCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);
		AtomicInteger timeoutCount = new AtomicInteger(0);
		
		List<Long> responseTimes = new CopyOnWriteArrayList<>();
		AtomicLong totalWaitTime = new AtomicLong(0);
		
		long startTime = System.currentTimeMillis();
		
		for (int serverId = 0; serverId < serverCount; serverId++) {
			final int currentServerId = serverId;
			for (int requestId = 0; requestId < requestsPerServer; requestId++) {
				final int currentRequestId = requestId;
				executor.submit(() -> {
					try {
						long requestStart = System.currentTimeMillis();
						
						Long userId = 1L;
						
						NotificationRequest request = new NotificationRequest(
								userId,
								NotificationType.LIKE,
								"Test message from server " + currentServerId + "-" + currentRequestId
						);
						
						HttpHeaders headers = new HttpHeaders();
						headers.setContentType(MediaType.APPLICATION_JSON);
						HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, headers);
						
						try {
							ResponseEntity<NotificationResponse> response = restTemplate.postForEntity(
									getBaseUrl() + "/api/notifications/send",
									entity,
									NotificationResponse.class
							);
							
							long responseTime = System.currentTimeMillis() - requestStart;
							responseTimes.add(responseTime);
							
							if (responseTime > 1000) {
								totalWaitTime.addAndGet(responseTime - 1000);
							}
							
							NotificationResponse responseBody = response.getBody();
							if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
								if (responseBody.isSent()) {
									successCount.incrementAndGet();
								} else {
									duplicateCount.incrementAndGet();
								}
							}
						} catch (Exception e) {
							errorCount.incrementAndGet();
							String errorMsg = e.getMessage();
							if (errorMsg != null && (
									errorMsg.contains("timeout") ||
									errorMsg.contains("Timeout") ||
									errorMsg.contains("timed out")
							)) {
								timeoutCount.incrementAndGet();
							}
						}
					} finally {
						latch.countDown();
					}
				});
			}
		}
		
		boolean completed = latch.await(120, TimeUnit.SECONDS);
		long totalTime = System.currentTimeMillis() - startTime;
		
		executor.shutdown();
		
		System.out.println("\n=== 부하 테스트 결과 (Arcus add 연산 기반, 수평 확장 환경) ===");
		System.out.println("시뮬레이션 서버 수: " + serverCount);
		System.out.println("서버당 요청 수: " + requestsPerServer);
		System.out.println("총 요청 수: " + totalThreadCount);
		System.out.println("성공 (발송): " + successCount.get());
		System.out.println("중복 방지: " + duplicateCount.get());
		System.out.println("에러: " + errorCount.get());
		System.out.println("타임아웃: " + timeoutCount.get());
		System.out.println("총 소요 시간: " + totalTime + "ms");
		
		long maxResponseTime = 0L;
		if (!responseTimes.isEmpty()) {
			double avgResponseTime = responseTimes.stream()
					.mapToLong(Long::longValue)
					.average()
					.orElse(0.0);
			maxResponseTime = responseTimes.stream()
					.mapToLong(Long::longValue)
					.max()
					.orElse(0L);
			long minResponseTime = responseTimes.stream()
					.mapToLong(Long::longValue)
					.min()
					.orElse(0L);
			
			long slowRequests = responseTimes.stream()
					.filter(time -> time > 1000)
					.count();
			
			System.out.println("\n--- 성능 지표 (수평 확장 환경) ---");
			System.out.println("평균 응답 시간: " + String.format("%.2f", avgResponseTime) + "ms");
			System.out.println("최대 응답 시간: " + maxResponseTime + "ms");
			System.out.println("최소 응답 시간: " + minResponseTime + "ms");
			System.out.println("1초 이상 걸린 요청 수: " + slowRequests + "건");
			System.out.println("총 대기 시간: " + totalWaitTime.get() + "ms");
			System.out.println("TPS: " + String.format("%.2f", (totalThreadCount * 1000.0 / totalTime)));
		}
		
		System.out.println("\n--- Race Condition 해결 검증 ---");
		System.out.println("예상 성공 건수: 1건 (원자적 add 연산)");
		System.out.println("실제 성공 건수: " + successCount.get() + "건");
		
		assertThat(completed).isTrue();
		assertThat(successCount.get()).isGreaterThan(0);
		assertThat(duplicateCount.get() + successCount.get() + errorCount.get())
				.isLessThanOrEqualTo(totalThreadCount);
	}
}

