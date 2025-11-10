# 알림 중복 발송 방지 시스템

알림 시스템에서 동일한 알림이 짧은 시간 내 여러 번 발송되는 문제를 해결하기 위한 프로젝트입니다.

## 프로젝트 구조

```
src/main/java/com/example/noti_idempo/
├── domain/
│   ├── NotificationType.java
│   └── NotificationRecord.java
├── repository/
│   └── NotificationRecordRepository.java
├── service/
│   └── NotificationService.java
├── controller/
│   └── NotificationController.java
└── dto/
    ├── NotificationRequest.java
    └── NotificationResponse.java
```

## 구현 내용

### DB 기반 중복 방지

- 최근 발송 알림 기록을 DB에 저장
- 알림 발송 전 DB 조회하여 중복 확인
- 중복 체크 시간 범위: 최근 1분

## API

### 알림 발송

```bash
POST /api/notifications/send
```

**요청:**
```json
{
  "userId": 1,
  "notificationType": "LIKE",
  "message": "사용자가 좋아요를 눌렀습니다."
}
```

**응답:**
```json
{
  "sent": true,
  "message": "알림이 발송되었습니다.",
  "reason": "SUCCESS"
}
```

## 실행 방법

### 빌드
```bash
./gradlew build
```

### 실행
```bash
./gradlew bootRun
```

### H2 Console
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (비어있음)

## 부하 테스트

```bash
./gradlew test --tests LoadTest
```

테스트 설정:
- 동시 요청: 200 스레드 × 20회 = 4,000건
- 사용자 ID: 1~10 순환
- 알림 유형: LIKE

## 기술 스택

- Java 17
- Spring Boot 3.5.7
- Spring Data JPA
- H2 Database
- Lombok
- JUnit 5
