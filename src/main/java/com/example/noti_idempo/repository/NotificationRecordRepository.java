package com.example.noti_idempo.repository;

import com.example.noti_idempo.domain.NotificationRecord;
import com.example.noti_idempo.domain.NotificationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT n FROM NotificationRecord n " +
			"WHERE n.userId = :userId " +
			"AND n.notificationType = :type " +
			"AND n.createdAt >= :since " +
			"ORDER BY n.createdAt DESC " +
			"LIMIT 1")
	Optional<NotificationRecord> findRecentByUserIdAndType(
			@Param("userId") Long userId,
			@Param("type") NotificationType type,
			@Param("since") LocalDateTime since
	);
}

