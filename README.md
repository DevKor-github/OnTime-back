# OnTime-back ![image](https://github.com/user-attachments/assets/0f6a7c9a-960b-4a34-a934-2bc0e66f3a02) 
#### 시간 관리/지각 방지 애플리케이션 Ontime의 BE 코드입니다.


## ERD
![Image](https://github.com/user-attachments/assets/996885c2-0ba9-40ce-9f2e-2323bf684b5b)

## 데모
아직 FE에서 코드가 완성되지 않아 기능 중 일부만 시연되는 데모영상입니다.
### 로그인 및 온보딩(기본 준비과정 세팅)
![Simulator_Screen_Recording_-_iPhone_16_Pro_-_2025-02-10_at_01 01 06](https://github.com/user-attachments/assets/55329ef4-846d-4d02-b12e-881dc30ecfc3)
![Simulator_Screen_Recording_-_iPhone_16_Pro_-_2025-02-10_at_01 01 06 (1)](https://github.com/user-attachments/assets/9cc38fa3-5b55-4bb4-9e3e-1d5dc61ac409)


### 사용자의 약속 생성 화면
![Simulator_Screen_Recording_-_iPhone_16_Pro_-_2025-02-10_at_01 01 06 (2)](https://github.com/user-attachments/assets/a06de83e-3fcb-4f82-ac7e-44ca3aa1732e)
![Simulator_Screen_Recording_-_iPhone_16_Pro_-_2025-02-10_at_01 01 06 (3)](https://github.com/user-attachments/assets/6c27aee7-a823-474e-9b91-96595a871779)

### 사용자의 약속 준비 화면
![ontime_alarm_screen](https://github.com/user-attachments/assets/b441bca1-4395-4570-89c5-a8f9296b8a28)


## API 명세서
![screencapture-ontime-devkor-club-swagger-ui-index-html-2025-02-10-09_17_38](https://github.com/user-attachments/assets/0da05bb0-8596-48c3-8d16-d579820cc8d8)

## 로컬 개발 설정

백엔드는 실제 운영 비밀값 없이 실행할 수 있도록 `ontime-back/.env.example`에 로컬 전용 샘플 값을 제공합니다. 로컬에서 필요한 경우 이 파일을 `ontime-back/.env`로 복사해 개인 환경에 맞게 수정하세요. Firebase Admin JSON, Apple `.p8` 키, DB 비밀번호, JWT 서명 키 같은 실제 비밀값은 `src/main/resources`나 커밋 대상 파일에 두지 마세요.
