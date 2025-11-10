package com.example.noti_idempo.dto;

import com.example.noti_idempo.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
	private Long userId;
	private NotificationType notificationType;
	private String message;
}

