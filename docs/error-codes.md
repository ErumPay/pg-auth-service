# pg-auth-service Error Codes

## Domain Prefix

| Prefix | Domain |
|---|---|
| `REQ` | 공통 요청 검증, Body, Header, PathVariable |
| `MCH` | 가맹점 로그인, 약관 동의, 회원가입, 계정 상태 |
| `TKN` | Access Token, Refresh Token, Signup Token |
| `ADM` | PG 관리자 로그인, TOTP, IP, 계정 잠금 |
| `AUD` | 관리자 감사 로그 |
| `KAK` | 카카오 OAuth 외부 연동 |
| `EXT` | merchant-service 외부 연동 |
| `DB` | MySQL, Redis 저장 및 조회 |
| `SYS` | 알 수 없는 내부 오류 |

## 요청 검증 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-REQ-001` | `INVALID_REQUEST` | 400 | 잘못된 요청입니다. | 잘못된 JSON Body, PathVariable 타입 오류, enum 변환 실패 등 공통 요청 파싱 실패 | 요청 형식과 값을 수정한 후 재요청 | SDK 미노출 |
| `AUTH-REQ-002` | `MERCHANT_SIGNUP_INVALID_REQUEST` | 400 | 가맹점 회원가입 요청 값이 올바르지 않습니다. | 사업자번호, 가맹점명, MCC 코드, 대표자명, 이메일, 연락처, 정산계좌, 은행명, 서비스명 중 필수값 누락 | 누락된 회원가입 항목을 입력한 후 재요청 | SDK 미노출 |
| `AUTH-REQ-003` | `MERCHANT_STATUS_REQUIRED` | 400 | 가맹점 상태가 필요합니다. | 내부 가맹점 상태 변경 요청의 status가 null | status 값을 포함해 재요청 | SDK 미노출 |

## 가맹점 인증 및 가입 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-MCH-001` | `SERVICE_TERMS_REQUIRED` | 400 | 서비스 이용약관 동의는 필수입니다. | 약관 동의 요청에서 serviceTermsAgreed가 true가 아님 | 필수 약관 동의 후 재요청 | SDK 미노출 |
| `AUTH-MCH-002` | `PRIVACY_POLICY_REQUIRED` | 400 | 개인정보 처리방침 동의는 필수입니다. | 약관 동의 요청에서 privacyPolicyAgreed가 true가 아님 | 개인정보 처리방침 동의 후 재요청 | SDK 미노출 |
| `AUTH-MCH-003` | `TERMS_VERSION_REQUIRED` | 400 | 약관 버전은 필수입니다. | 약관 동의 요청에서 termsVersion 누락 또는 공백 | 현재 약관 버전을 포함해 재요청 | SDK 미노출 |
| `AUTH-MCH-201` | `MERCHANT_ACCOUNT_NOT_FOUND` | 404 | 가맹점 계정을 찾을 수 없습니다. | Signup Token의 accountId에 해당하는 MerchantAuth가 없음 | 로그인 단계부터 다시 진행하거나 계정 데이터 확인 | SDK 미노출 |
| `AUTH-MCH-202` | `MERCHANT_NOT_FOUND` | 404 | 가맹점을 찾을 수 없습니다. | 존재하지 않는 merchantId로 토큰 폐기 또는 상태 변경 요청 | merchantId 확인 후 재요청 | SDK 미노출 |
| `AUTH-MCH-203` | `MERCHANT_LOGIN_NOT_ALLOWED` | 409 | 현재 가맹점 상태에서는 로그인할 수 없습니다. | 가맹점 상태가 PENDING, REJECTED, SUSPENDED, WITHDRAWN 등 ACTIVE/DRAFT 로그인 흐름에 해당하지 않음 | 상태를 안내하고 로그인 중단, 운영 정책에 따라 심사 또는 복구 절차 진행 | SDK 미노출 |
| `AUTH-MCH-204` | `MERCHANT_SIGNUP_NOT_ALLOWED` | 409 | 현재 가맹점 상태에서는 회원가입을 진행할 수 없습니다. | 가맹점 계정 상태가 DRAFT가 아닌데 약관 동의 또는 회원가입 진행 | 현재 가맹점 상태 조회 후 허용되는 절차로 이동 | SDK 미노출 |
| `AUTH-MCH-205` | `MERCHANT_TERMS_REQUIRED` | 409 | 약관 동의 후 회원가입을 진행할 수 있습니다. | 약관 동의 이력 없이 가맹점 회원가입 요청 | 약관 동의 API 완료 후 회원가입 재요청 | SDK 미노출 |
| `AUTH-MCH-301` | `MERCHANT_TERMS_ALREADY_AGREED` | 409 | 이미 약관 동의가 완료된 가맹점 계정입니다. | 같은 accountId로 약관 동의를 중복 요청 | 기존 동의 결과를 사용하고 회원가입 단계로 이동 | SDK 미노출 |

## 토큰 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-TKN-001` | `REFRESH_TOKEN_REQUIRED` | 401 | Refresh Token이 필요합니다. | Authorization Header와 요청 Body 모두에 Refresh Token이 없음 | Refresh Token을 포함해 재요청 | SDK 미노출 |
| `AUTH-TKN-002` | `SIGNUP_TOKEN_REQUIRED` | 401 | Authorization 헤더에 회원가입 토큰이 필요합니다. | 약관 동의 또는 회원가입 요청에 Bearer Signup Token이 없음 | Signup Token을 Bearer 형식으로 포함해 재요청 | SDK 미노출 |
| `AUTH-TKN-003` | `INTERNAL_API_KEY_REQUIRED` | 401 | 내부 API 인증 정보가 필요합니다. | `/internal/v1/auth/**` 요청에 `X-Internal-Api-Key`가 없거나 비어 있음 | 내부 API Key Header를 포함해 재요청 | SDK 미노출 |
| `AUTH-TKN-101` | `SIGNUP_TOKEN_INVALID` | 401 | 유효하지 않은 회원가입 토큰입니다. | Signup Token의 서명 검증 실패 또는 만료 | 카카오 로그인부터 다시 진행해 Signup Token 재발급 | SDK 미노출 |
| `AUTH-TKN-102` | `SIGNUP_TOKEN_TYPE_INVALID` | 401 | 회원가입용 토큰이 아닙니다. | tokenType이 SIGNUP이 아닌 토큰으로 가입 API 호출 | Signup Token으로 교체 후 재요청 | SDK 미노출 |
| `AUTH-TKN-103` | `SIGNUP_TOKEN_ROLE_INVALID` | 403 | 가맹점 회원가입 토큰이 아닙니다. | Signup Token의 role이 MERCHANT가 아님 | 가맹점용 Signup Token으로 교체 후 재요청 | SDK 미노출 |
| `AUTH-TKN-104` | `MERCHANT_REFRESH_TOKEN_INVALID` | 401 | 유효하지 않은 가맹점 Refresh Token입니다. | 토큰 검증 실패, tokenType이 REFRESH가 아님 또는 role이 MERCHANT가 아님 | 로컬 토큰을 제거하고 가맹점 재로그인 | SDK 미노출 |
| `AUTH-TKN-105` | `ADMIN_REFRESH_TOKEN_INVALID` | 401 | 유효하지 않은 관리자 Refresh Token입니다. | 토큰 검증 실패, tokenType이 REFRESH가 아님 또는 role이 PG_ADMIN이 아님 | 관리자 재로그인 | SDK 미노출 |
| `AUTH-TKN-106` | `REFRESH_TOKEN_REVOKED` | 401 | 폐기되었거나 존재하지 않는 Refresh Token입니다. | DB에서 tokenHash를 찾을 수 없거나 isRevoked가 true | 저장된 토큰을 제거하고 재로그인 | SDK 미노출 |
| `AUTH-TKN-107` | `REFRESH_TOKEN_MISMATCH` | 401 | 저장된 Refresh Token과 일치하지 않습니다. | 가맹점 요청 토큰이 Redis에 저장된 현재 활성 토큰과 다름 | 토큰 탈취 또는 이전 토큰 사용 가능성을 고려해 재로그인 | SDK 미노출 |
| `AUTH-TKN-108` | `ACCESS_TOKEN_INVALID` | 401 | 유효하지 않은 Access Token입니다. | Access Token 서명 검증 실패, 만료 또는 tokenType 불일치 | Refresh API로 토큰 갱신하거나 재로그인 | SDK 미노출 |
| `AUTH-TKN-109` | `ACCESS_TOKEN_REVOKED` | 401 | 폐기된 Access Token입니다. | 로그아웃 등으로 Access Token이 Redis blacklist에 등록됨 | 저장 토큰 제거 후 재로그인 | SDK 미노출 |
| `AUTH-TKN-110` | `INTERNAL_API_KEY_INVALID` | 403 | 유효하지 않은 내부 API 인증 정보입니다. | `X-Internal-Api-Key`가 서버 설정값과 일치하지 않음 | 서비스 설정의 API Key 확인 후 재요청 | SDK 미노출 |
| `AUTH-TKN-901` | `TOKEN_HASH_FAILED` | 500 | 토큰 해시 생성에 실패했습니다. | SHA-256 MessageDigest 초기화 실패 | 서버 로그와 런타임 보안 Provider 상태 확인 | SDK 미노출 |

## 관리자 인증 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-ADM-001` | `ADMIN_LOGIN_REQUEST_INVALID` | 400 | 아이디, 비밀번호, TOTP 코드는 필수입니다. | 관리자 로그인 요청의 loginId, password, totpCode 중 하나 이상 누락 | 누락된 로그인 정보를 입력한 후 재요청 | SDK 미노출 |
| `AUTH-ADM-101` | `ADMIN_CREDENTIALS_INVALID` | 401 | 아이디 또는 비밀번호가 일치하지 않습니다. | loginId에 해당하는 계정이 없거나 비밀번호 불일치 | 동일 메시지를 표시하고 자격 증명 재입력 요청 | SDK 미노출 |
| `AUTH-ADM-102` | `ADMIN_TOTP_INVALID` | 401 | TOTP 코드가 일치하지 않습니다. | TOTP 코드가 6자리 형식이 아니거나 현재 허용 시간 구간의 코드와 불일치 | 새로운 TOTP 코드를 입력해 재요청 | SDK 미노출 |
| `AUTH-ADM-103` | `ADMIN_TOTP_VERIFICATION_FAILED` | 401 | TOTP 코드 검증에 실패했습니다. | 잘못된 TOTP secret 또는 암호 연산 오류로 검증 처리 자체가 실패 | 운영자가 TOTP secret과 서버 로그 확인 | SDK 미노출 |
| `AUTH-ADM-104` | `ADMIN_IP_NOT_ALLOWED` | 403 | 허용되지 않은 IP입니다. | 요청 IP가 관리자 계정 allowedIps 목록에 없음 | 허용된 네트워크에서 재시도하거나 allowedIps 설정 확인 | SDK 미노출 |
| `AUTH-ADM-203` | `ADMIN_ACCOUNT_LOCKED` | 423 | 관리자 계정이 잠금 상태입니다. | 로그인 실패 5회 이상으로 lockedUntil이 현재 시각 이후 | 잠금 만료 후 재시도하거나 운영자가 계정 상태 확인 | SDK 미노출 |

## 감사 로그 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-AUD-001` | `AUDIT_ADMIN_ID_REQUIRED` | 400 | admin_id는 필수입니다. | 감사 로그 요청의 adminId가 null | adminId를 포함해 재요청 | SDK 미노출 |
| `AUTH-AUD-002` | `AUDIT_ACTION_REQUIRED` | 400 | action은 필수입니다. | 감사 로그 요청의 action이 누락 또는 공백 | action을 포함해 재요청 | SDK 미노출 |
| `AUTH-AUD-003` | `AUDIT_IP_ADDRESS_REQUIRED` | 400 | ip_address는 필수입니다. | 감사 로그 요청의 ipAddress가 누락 또는 공백 | ipAddress를 포함해 재요청 | SDK 미노출 |
| `AUTH-AUD-004` | `AUDIT_CHANGE_DETAIL_INVALID` | 400 | change_detail JSON 변환에 실패했습니다. | changeDetail 객체를 JSON 문자열로 직렬화할 수 없음 | 전달 객체의 JSON 변환 가능 여부 확인 후 재요청 | SDK 미노출 |
| `AUTH-AUD-201` | `AUDIT_ADMIN_NOT_FOUND` | 404 | 감사 로그 대상 관리자를 찾을 수 없습니다. | 존재하지 않는 adminId로 감사 로그 기록 요청 | adminId 확인 후 재요청 | SDK 미노출 |

## 카카오 OAuth 연동 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-KAK-001` | `KAKAO_AUTH_CODE_REQUIRED` | 400 | 카카오 인가 코드가 필요합니다. | 카카오 로그인 요청의 authorization code 누락 또는 공백 | 카카오 인가 화면부터 다시 진행 | SDK 미노출 |
| `AUTH-KAK-400` | `KAKAO_TOKEN_REQUEST_REJECTED` | 502 | 카카오 토큰 요청에 실패했습니다. | 카카오 토큰 API가 4xx 또는 5xx 응답 반환 | 인가 코드, client 설정 및 카카오 응답 로그 확인 | SDK 미노출 |
| `AUTH-KAK-401` | `KAKAO_TOKEN_REQUEST_UNAVAILABLE` | 503 | 카카오 토큰 요청 중 네트워크 오류가 발생했습니다. | 카카오 토큰 API 연결 실패 또는 네트워크 오류 | 카카오 상태 확인 후 제한적으로 재시도 | SDK 미노출 |
| `AUTH-KAK-402` | `KAKAO_TOKEN_RESPONSE_INVALID` | 502 | 카카오 토큰 발급 응답이 올바르지 않습니다. | 카카오 토큰 응답이 null이거나 accessToken이 없음 | 카카오 응답 로그 확인 후 로그인 재진행 | SDK 미노출 |
| `AUTH-KAK-403` | `KAKAO_USER_REQUEST_REJECTED` | 502 | 카카오 사용자 정보 요청에 실패했습니다. | 카카오 사용자 정보 API가 4xx 또는 5xx 응답 반환 | Access Token과 카카오 응답 로그 확인 | SDK 미노출 |
| `AUTH-KAK-404` | `KAKAO_USER_REQUEST_UNAVAILABLE` | 503 | 카카오 사용자 정보 요청 중 네트워크 오류가 발생했습니다. | 카카오 사용자 정보 API 연결 실패 또는 네트워크 오류 | 카카오 상태 확인 후 제한적으로 재시도 | SDK 미노출 |
| `AUTH-KAK-405` | `KAKAO_USER_RESPONSE_INVALID` | 502 | 카카오 사용자 정보 응답이 올바르지 않습니다. | 사용자 정보 응답이 null이거나 Kakao user id가 없음 | 카카오 응답 로그 확인 후 로그인 재진행 | SDK 미노출 |

## merchant-service 연동 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-EXT-400` | `MERCHANT_CREATE_REQUEST_FAILED` | 503 | 가맹점 생성 서비스를 일시적으로 사용할 수 없습니다. | merchant-service 가맹점 생성 API 호출 실패, 타임아웃 또는 서비스 장애 | merchant-service 상태 확인 후 동일 Idempotency-Key로 재시도 | SDK 미노출 |
| `AUTH-EXT-401` | `MERCHANT_CREATE_RESPONSE_INVALID` | 502 | 가맹점 생성 응답이 올바르지 않습니다. | merchant-service 응답이 null이거나 merchantId가 없음 | 연동 응답 로그 확인, 중복 생성 여부 확인 후 재처리 | SDK 미노출 |

## DB 및 Redis 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-DB-901` | `AUTH_DATA_ACCESS_FAILED` | 500 | 인증 데이터 처리에 실패했습니다. | MerchantAuth, 약관, 관리자, Refresh Token, 감사 로그 DB 저장 또는 조회 실패 | 서버 로그와 DB 상태 확인 후 재시도 | SDK 미노출 |
| `AUTH-DB-902` | `AUTH_CACHE_ACCESS_FAILED` | 500 | 인증 캐시 처리에 실패했습니다. | Refresh Token 저장·조회·삭제 또는 Access Token blacklist 처리 중 Redis 장애 | Redis 상태 확인 후 토큰 정합성 점검 | SDK 미노출 |

## 내부 서버 에러 코드

| Code | Reason | HTTP | Message | 발생 조건 | 호출자 처리 | SDK 공개 코드 매핑 |
|---|---|---:|---|---|---|---|
| `AUTH-SYS-900` | `INTERNAL_SERVER_ERROR` | 500 | 알 수 없는 내부 오류가 발생했습니다. | 위 코드로 분류되지 않은 처리되지 않은 서버 예외 | correlationId 기준으로 서버 로그 확인 | SDK 미노출 |

## 비고

- 이 표는 `pg-auth-service`의 Controller, Service, Security Filter, 외부 Client 및 상태 enum을 기준으로 작성했다.
- 현재 `AuthException`은 HTTP status와 message만 가지고 있어 `code`, `reason` 응답은 아직 구현되지 않았다.
- `AUTH-TKN-108`부터 `AUTH-TKN-110`까지는 현재 Security Filter가 인증 객체를 만들지 않은 뒤 Spring Security 기본 응답으로 처리한다. 공통 에러 응답을 적용할 때 `AuthenticationEntryPoint`와 `AccessDeniedHandler`에서 해당 코드로 변환해야 한다.
- `AUTH-EXT-400`, `AUTH-DB-901`, `AUTH-DB-902`는 현재 전용 예외 변환 없이 `AUTH-SYS-900`으로 처리될 수 있다. Feign, JPA, Redis 예외 처리기를 추가할 때 분리 적용한다.
- 모든 코드는 서비스 내부 및 서비스 간 운영 추적용이며 현재 외부 SDK 공개 코드로 직접 노출하지 않는다.
