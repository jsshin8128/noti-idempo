package com.example.noti_idempo.controller;

import com.example.noti_idempo.dto.NotificationRequest;
import com.example.noti_idempo.dto.NotificationResponse;
import com.example.noti_idempo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
	
	private final NotificationService notificationService;
	
	@PostMapping("/send")
	public ResponseEntity<NotificationResponse> sendNotification(
			@RequestBody NotificationRequest request) {
		NotificationResponse response = notificationService.sendNotification(request);
		return ResponseEntity.ok(response);
	}
}

