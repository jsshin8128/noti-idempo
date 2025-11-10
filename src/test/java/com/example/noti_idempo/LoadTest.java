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

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadTest {
	
	@LocalServerPort
	private int port;
	
	@Autowired
	private TestRestTemplate restTemplate;
	
	private String getBaseUrl() {
		return "http://localhost:" + port;
	}
	
	@Test
	void testConcurrentRequests() throws InterruptedException {
		int threadCount = 200;
		int requestsPerThread = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger duplicateCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);
		AtomicInteger timeoutCount = new AtomicInteger(0);
		AtomicInteger deadlockCount = new AtomicInteger(0);
		
		List<Long> responseTimes = new CopyOnWriteArrayList<>();
		AtomicLong totalWaitTime = new AtomicLong(0);
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					for (int j = 0; j < requestsPerThread; j++) {
						long requestStart = System.currentTimeMillis();
						
						Long userId = (long) ((threadId % 10) + 1);
						
						NotificationRequest request = new NotificationRequest(
								userId,
								NotificationType.LIKE,
								"Test message from thread " + threadId + "-" + j
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
							String exceptionClass = e.getClass().getSimpleName();
							String fullMessage = e.toString();
							
							boolean isDeadlockRelated = false;
							if (errorMsg != null) {
								String lowerMsg = errorMsg.toLowerCase();
								String lowerClass = exceptionClass.toLowerCase();
								String lowerFull = fullMessage.toLowerCase();
								
								isDeadlockRelated = lowerMsg.contains("deadlock") ||
									lowerMsg.contains("deadlock detected") ||
									lowerMsg.contains("40001") ||
									lowerMsg.contains("lock timeout") ||
									lowerMsg.contains("could not acquire lock") ||
									lowerMsg.contains("lock wait timeout") ||
									lowerMsg.contains("timeout trying to lock") ||
									lowerClass.contains("deadlock") ||
									lowerFull.contains("deadlock") ||
									(lowerMsg.contains("timeout") && lowerMsg.contains("lock")) ||
									(exceptionClass.contains("JdbcSQLException") && lowerMsg.contains("40001"));
							}
							
							Throwable cause = e.getCause();
							while (cause != null && !isDeadlockRelated) {
								String causeMsg = cause.getMessage();
								if (causeMsg != null) {
									String lowerCauseMsg = causeMsg.toLowerCase();
									if (lowerCauseMsg.contains("deadlock") || 
										lowerCauseMsg.contains("40001") ||
										(lowerCauseMsg.contains("timeout") && lowerCauseMsg.contains("lock"))) {
										isDeadlockRelated = true;
									}
								}
								cause = cause.getCause();
							}
							
							if (isDeadlockRelated) {
								deadlockCount.incrementAndGet();
							} else if (errorMsg != null && (
									errorMsg.contains("timeout") || 
									errorMsg.contains("Timeout") ||
									errorMsg.contains("timed out")
							)) {
								timeoutCount.incrementAndGet();
							}
						}
						
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}
					}
				} finally {
					latch.countDown();
				}
			});
		}
		
		boolean completed = latch.await(120, TimeUnit.SECONDS);
		long totalTime = System.currentTimeMillis() - startTime;
		
		executor.shutdown();
		
		System.out.println("\n=== 부하 테스트 결과 ===");
		System.out.println("동시 요청 수 (Thread Count): " + threadCount);
		System.out.println("스레드당 요청 수: " + requestsPerThread);
		System.out.println("총 요청 수: " + (threadCount * requestsPerThread));
		System.out.println("성공 (발송): " + successCount.get());
		System.out.println("중복 방지: " + duplicateCount.get());
		System.out.println("에러: " + errorCount.get());
		System.out.println("타임아웃: " + timeoutCount.get());
		System.out.println("Deadlock: " + deadlockCount.get());
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
			
			System.out.println("\n--- 성능 지표 ---");
			System.out.println("평균 응답 시간: " + String.format("%.2f", avgResponseTime) + "ms");
			System.out.println("최대 응답 시간: " + maxResponseTime + "ms");
			System.out.println("최소 응답 시간: " + minResponseTime + "ms");
			System.out.println("1초 이상 걸린 요청 수: " + slowRequests + "건");
			System.out.println("총 Lock 대기 시간: " + totalWaitTime.get() + "ms");
			System.out.println("TPS: " + String.format("%.2f", (threadCount * requestsPerThread * 1000.0 / totalTime)));
		}
		
		assertThat(completed).isTrue();
		assertThat(successCount.get()).isGreaterThan(0);
		assertThat(duplicateCount.get() + successCount.get() + errorCount.get())
				.isLessThanOrEqualTo(threadCount * requestsPerThread);
	}
}

