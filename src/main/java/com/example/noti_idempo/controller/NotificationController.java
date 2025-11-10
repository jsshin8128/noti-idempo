package com.example.noti_idempo.controller;

import com.example.noti_idempo.dto.NotificationRequest;
import com.example.noti_idempo.dto.NotificationResponse;
import com.example.noti_idempo.service.NotificationService;
import com.example.noti_idempo.service.NotificationServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
	
	private final NotificationService notificationService;
	
	@Autowired(required = false)
	private NotificationServiceV2 notificationServiceV2;
	
	@Value("${notification.service.version:v1}")
	private String serviceVersion;
	
	@PostMapping("/send")
	public ResponseEntity<NotificationResponse> sendNotification(
			@RequestBody NotificationRequest request) {
		NotificationResponse response;
		
		if ("v2".equals(serviceVersion) && notificationServiceV2 != null) {
			response = notificationServiceV2.sendNotification(request);
		} else {
			response = notificationService.sendNotification(request);
		}
		
		return ResponseEntity.ok(response);
	}
}

