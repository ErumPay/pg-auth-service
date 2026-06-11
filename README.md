# pg-auth-service

PG 가맹점과 PG 관리자의 인증을 담당하는 Spring Boot 서비스입니다.

주요 기능은 카카오 기반 가맹점 로그인/가입, 약관 동의, JWT 발급 및 갱신,
관리자 로그인과 TOTP 검증, Refresh Token 관리, 관리자 감사 로그 기록입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data JPA / MySQL
- Spring Data Redis
- OpenFeign
- JWT (`jjwt`)
- Gradle


## 가맹점 가입 흐름

1. 카카오 인가 코드를 이용해 로그인 API를 호출합니다.
2. 신규 또는 가입 미완료 계정은 `SIGNUP` 타입 JWT를 발급받습니다.
3. Signup Token으로 필수 약관에 동의합니다.
4. 가맹점 상세 정보를 입력해 회원가입을 완료합니다.
5. 계정은 `PENDING` 상태가 되며 관리자 승인 후 `ACTIVE`로 변경됩니다.
6. `ACTIVE` 가맹점은 다시 로그인하여 Access/Refresh Token을 발급받습니다.

## 계정 상태

```text
DRAFT -> PENDING -> ACTIVE
                  -> REJECTED
ACTIVE -> SUSPENDED
ACTIVE -> WITHDRAWN
```

- `DRAFT`: 가입 진행 중
- `PENDING`: 관리자 심사 대기
- `ACTIVE`: 로그인 및 서비스 이용 가능
- `REJECTED`: 심사 거절
- `SUSPENDED`: 이용 정지
- `WITHDRAWN`: 탈퇴

