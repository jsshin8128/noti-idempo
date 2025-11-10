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
│   ├── NotificationService.java      # DB 기반
│   └── NotificationServiceV2.java    # Arcus 캐시 기반
├── cache/
│   ├── CacheService.java
│   ├── ArcusCacheService.java
│   └── CacheKeyGenerator.java
├── config/
│   └── ArcusConfig.java
├── controller/
│   └── NotificationController.java
└── dto/
    ├── NotificationRequest.java
    └── NotificationResponse.java
```

## 구현 내용

### 1단계: DB 기반 중복 방지

- 최근 발송 알림 기록을 DB에 저장
- 알림 발송 전 DB 조회하여 중복 확인
- 중복 체크 시간 범위: 최근 1분

### 2단계: Arcus 캐시 기반 중복 방지

- Arcus 캐시를 이용한 중복 확인
- 캐시 키: `notification:LIKE:1` 형식
- TTL: 60초
- **실험 결과**: 단일 인스턴스 환경에서도 Race Condition 발생 확인
- **문제점**: 캐시 조회(`exists`)와 저장(`set`) 사이에 Race Condition 발생
- **수평 확장 환경**: 여러 서버가 동시에 요청을 처리하는 상황에서 중복 알림 재발 가능

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

### Arcus 설정

Docker Compose로 Arcus 실행:
```yaml
services:
  zoo1:
    image: zookeeper:3.9
    ports:
      - 2181:2181
  register:
    image: jam2in/zkcli:3.5.9
    environment:
      ZK_ENSEMBLE: zoo1:2181
      SERVICE_CODE: test
      CACHENODES: cache1:11211
  cache1:
    image: jam2in/arcus-memcached:latest
    ports:
      - 11211:11211
```

application.properties:
```properties
arcus.enabled=true
arcus.zookeeper.address=127.0.0.1:2181
arcus.service.code=test
arcus.ttl=60
notification.service.version=v2
```

### H2 Console
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (비어있음)

## 부하 테스트

### DB 기반 테스트
```bash
./gradlew test --tests LoadTest
```

### Arcus 캐시 기반 테스트
```bash
./gradlew test --tests LoadTestV2
```

테스트 설정:
- 동시 요청: 200 스레드 × 20회 = 4,000건
- 사용자 ID: 1~10 순환
- 알림 유형: LIKE

## 성능 비교

| 지표 | DB 기반 | Arcus 캐시 기반 | 비고 |
|------|---------|-----------------|------|
| 평균 응답 시간 | 3,200ms | 3,251ms | 거의 동일 |
| 최대 응답 시간 | 9,432ms | 7,556ms | 약간 개선 |
| TPS | 60.23 | 59.58 | 거의 동일 |
| Lock 대기 시간 | 8,842,138ms | 9,032,148ms | 거의 동일 |
| 중복 발송 | 22건 | 22건 | 동일 (Race Condition) |

**실험 결과 분석:**
- Arcus 캐시 사용에도 불구하고 DB 기반과 거의 동일한 성능 지표
- 단일 인스턴스 환경에서도 Race Condition 발생 (22건 중복 발송)
- 캐시 조회(`exists`)와 저장(`set`) 사이의 시간 간격에서 Race Condition 발생
- 수평 확장 환경에서는 더욱 심각한 문제가 될 수 있음

## 기술 스택

- Java 17
- Spring Boot 3.5.7
- Spring Data JPA
- H2 Database
- Arcus Java Client 1.14.1
- Lombok
- JUnit 5
