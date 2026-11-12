# 옆에서 (Yeop-eoseo)

**Ambient Motion Cues for Motion Sickness Relief**

차량의 움직임을 화면 가장자리의 앰비언트 시각 효과로 전달하여 동승자의 멀미 완화를 돕는 차량용 시각 보조 앱입니다.

옆에서는 Pleos Vehicle SDK를 통해 전달되는 차량 데이터를 실시간으로 처리하고, 화면 가장자리에 부드러운 지평선(Ambient Horizon)을 표현합니다.

사용자는 옆에서 앱을 실행하고 시각 효과를 활성화한 뒤, 내비게이션이나 미디어 등 다른 차량용 앱을 이용하면서도 차량의 움직임에 대응하는 시각적 단서를 경험할 수 있습니다.

> Less information. More awareness.
>
> 더 많은 정보를 보여주는 대신, 적은 시각적 표현으로 의미 있는 변화를 전달합니다.

---

## 1. 프로젝트 개요

### 1.1 문제 정의

차량에서 스마트폰이나 인포테인먼트 화면을 바라보는 탑승자는 시각적으로 정적인 화면을 인식하는 반면, 전정기관은 차량의 가속, 감속, 회전 등의 움직임을 감지합니다.

이러한 시각 정보와 전정 감각 정보 사이의 불일치는 차량 멀미 발생에 기여할 수 있습니다.

기존의 멀미 완화 방법은 화면을 보지 않거나 창밖을 바라보도록 권장하는 경우가 많습니다.

옆에서는 다른 접근을 시도합니다.

**사용자가 화면을 계속 이용하면서도 차량의 움직임을 시각적으로 인지할 수 있도록 화면 가장자리에 주변시 기반의 시각적 단서를 제공합니다.**

이러한 시각적 단서가 감각 정보의 불일치를 완화하고 멀미 증상을 줄이는 데 도움이 될 수 있다는 가설을 기반으로 합니다.

단, 실제 멀미 완화 효과는 개인과 주행 환경에 따라 달라질 수 있으며, 사용자 실험을 통해 검증해야 합니다.

### 1.2 핵심 사용자

- 차량의 동승자
- 뒷좌석 탑승자
- 차량 이동 중 내비게이션, 미디어 등 화면을 이용하는 탑승자
- 차량 멀미에 민감한 탑승자

본 앱은 운전자가 아닌 동승자를 주요 사용자로 설계합니다.

### 1.3 핵심 기능

- Pleos Vehicle SDK를 통한 차량 데이터 수신
- 조향 및 제동 입력 기반 시각적 단서 제공
- 화면 가장자리의 Ambient Horizon 렌더링
- Android Application Overlay를 통한 다른 앱 위 표시
- 세션 기반 효과 활성화 및 종료
- 브라우저 기반 차량 움직임 및 시각 효과 시뮬레이션

---

## 2. 사용자 경험

옆에서는 사용자가 직접 앱을 실행하고 시각 효과를 활성화하는 세션 기반 구조를 목표로 합니다.

### 2.1 실행 흐름

1. 사용자가 Pleos Connect에서 옆에서 앱을 실행합니다.
2. 앱에서 오버레이 권한 및 필요한 차량 데이터 접근 권한을 확인합니다.
3. 사용자가 멀미 완화 시각 효과를 활성화합니다.
4. 앱은 Vehicle SDK를 통해 차량 데이터를 수신합니다.
5. Ambient Horizon이 화면 가장자리에 표시됩니다.
6. 사용자는 내비게이션, 미디어 등 다른 앱으로 이동합니다.
7. 오버레이는 활성 세션 동안 유지됩니다.
8. 사용자가 옆에서 앱으로 돌아와 종료하면 오버레이와 차량 데이터 리스너를 해제합니다.

### 2.2 동작 원칙

- 사용자가 직접 실행한 경우에만 활성화합니다.
- 앱이 실행되지 않았거나 세션이 종료된 상태에서는 차량 데이터를 수집하지 않습니다.
- 중앙 콘텐츠를 최대한 가리지 않습니다.
- 차량 움직임에 대응하는 시각 효과는 부드럽고 제한된 범위에서 표현합니다.
- 차량 데이터가 유효하지 않거나 연결이 끊기면 잘못된 움직임을 표시하지 않고 안전하게 효과를 중단합니다.
- 차량의 시스템 UI, 경고 및 안전 관련 정보를 가리지 않아야 합니다.

---

## 3. 시각 효과 설계

### 3.1 Ambient Horizon

Ambient Horizon은 화면의 좌우 가장자리에 지평선 형태의 그래픽을 표시하는 핵심 인터페이스입니다.

중앙 화면의 콘텐츠를 유지하면서 주변시를 통해 차량의 움직임에 관한 시각적 단서를 전달하는 것을 목표로 합니다.

### 3.2 데이터와 시각 효과 매핑

| 차량 데이터 | 시각적 표현 | 상태 |
| --- | --- | --- |
| Steering Angle | 좌우 지평선 롤 | SDK 연동 |
| Brake Pedal Status | 수직 이동 및 글로우 변화 | SDK 연동 |
| Longitudinal Acceleration | 전후 방향 움직임 표현 | 추가 API 확인 필요 |
| Lateral Acceleration | 좌우 방향 움직임 표현 | 추가 API 확인 필요 |
| Yaw Rate | 차량 회전 움직임 표현 | 추가 API 확인 필요 |

현재 SDK에서 확보한 조향각과 브레이크 페달 상태는 운전자의 조작 입력입니다.

이는 차량의 실제 가속도나 회전 운동과 동일하지 않습니다.

따라서 실제 차량 운동 데이터가 확보되기 전까지는 해당 입력을 기반으로 한 시각적 단서로 표현하며, 실제 운동량을 측정한 것처럼 간주하지 않습니다.

### 3.3 렌더링 설계

- 조향각은 `−540°..+540°` 범위로 제한합니다.
- 지평선 롤은 현재 `−15°..+15°` 범위에서 표현합니다.
- 브레이크 페달 상태는 Boolean 입력으로 처리합니다.
- 브레이크 입력은 수직 이동 및 민트색 글로우 변화로 표현합니다.
- 브레이크 강도나 실제 감속도는 계산하지 않습니다.
- 지평선의 변화는 프레임 기반 보간을 통해 부드럽게 처리합니다.
- 화면 가장자리 영역으로 렌더링을 제한합니다.
- 급격한 밝기 변화, 섬광 및 과도한 움직임을 지양합니다.

기존 구현의 고정 저역통과 계수 `alpha = 0.12`는 초기 시각 효과 튜닝값으로 사용합니다.

최종 필터 계수와 애니메이션 반응 속도는 차량 데이터의 갱신 주기, 렌더링 지연 및 사용자 테스트 결과를 바탕으로 조정합니다.

---

## 4. Android 오버레이 아키텍처

### 4.1 목표 작동 방식

옆에서는 Android `TYPE_APPLICATION_OVERLAY`를 사용하여 다른 앱 위에 Ambient Horizon을 표시하는 구조를 목표로 합니다.

사용자가 옆에서 앱을 실행하고 효과를 활성화하면 별도의 서비스가 오버레이와 차량 데이터 수신을 관리합니다.

오버레이는 중앙 앱의 터치 및 포커스를 가로채지 않도록 구성합니다.

### 4.2 Overlay Window 설정

목표 구현 방식은 다음과 같습니다.

- `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`
- `WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE`
- `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE`
- `SYSTEM_ALERT_WINDOW`

`FLAG_NOT_TOUCHABLE`과 `FLAG_NOT_FOCUSABLE`을 사용하여 오버레이가 다른 앱의 입력을 가로채지 않도록 합니다.

단, Android 및 Pleos Connect의 시스템 정책에 따라 다른 앱 위에 표시하는 기능이 제한될 수 있습니다.

**본 프로젝트는 오버레이 허용을 전제로 설계하지만, 실제 Pleos Connect Emulator 및 App Market 심사를 통해 허용 여부를 확인해야 합니다.**

`SYSTEM_ALERT_WINDOW` 권한이 선언되어 있거나 Android 설정에서 허용되었다고 해서 Pleos 환경에서 오버레이 사용이 자동으로 승인되는 것은 아닙니다.

### 4.3 서비스 생명주기

오버레이는 세션 기반으로 동작합니다.

**세션 시작**

1. 사용자가 앱에서 시작 버튼을 누릅니다.
2. 오버레이 권한 및 차량 데이터 접근 권한을 확인합니다.
3. `MotionHorizonService`를 시작합니다.
4. Vehicle SDK를 초기화하고 데이터 리스너를 등록합니다.
5. 오버레이 Window를 생성하고 렌더링을 시작합니다.

**세션 종료**

1. 사용자가 종료 버튼을 누릅니다.
2. 오버레이 렌더링을 중단합니다.
3. Window를 제거합니다.
4. 차량 데이터 리스너 등록을 해제합니다.
5. `vehicle.release()`를 호출합니다.
6. 서비스를 종료합니다.

서비스가 비정상적으로 종료되는 경우에도 오버레이와 차량 데이터 리소스가 적절히 정리되도록 구현합니다.

---

## 5. 기술 스택

### 5.1 Android 앱

| 항목 | 버전 및 값 |
| --- | --- |
| 플랫폼 | Android / AAOS |
| minSdk | 28 |
| targetSdk | 34 |
| compileSdk | 34 |
| JDK | 17 |
| Gradle Wrapper | 8.0 |
| Android Gradle Plugin | 8.1.1 |
| Pleos SDK | `ai.pleos.playground:Vehicle:2.3.2` |
| 개발 언어 | Java |
| 렌더링 | Android Canvas / Custom View |
| 오버레이 | Android WindowManager |

Pleos SDK 의존성은 다음 Maven Repository를 사용합니다.

`https://nexus-playground.pleos.ai/repository/maven-releases/`

AndroidX 사용을 위해 `gradle.properties`에 다음 설정을 적용합니다.

```properties
android.useAndroidX=true
```

### 5.2 주요 권한

현재 프로젝트에서 사용하는 권한은 다음과 같습니다.

| 권한 | 목적 |
| --- | --- |
| `android.permission.SYSTEM_ALERT_WINDOW` | 다른 앱 위 오버레이 표시 |
| `pleos.car.permission.CAR_DRIVE` | 브레이크 등 주행 관련 데이터 접근 |
| `pleos.car.permission.CAR_INFO` | 차량 정보 도메인 접근 |
| `pleos.car.permission.READ_CAR_STEERING` | 조향 정보 접근 여부 및 실제 권한명 확인 필요 |

실제 권한은 SDK v2.3.2의 공식 문서 및 개발 환경에서 확인해야 합니다.

Manifest에 권한을 선언하는 것만으로 차량 데이터 접근이 보장되는 것은 아닙니다.

Pleos 플랫폼에서 요구하는 권한 승인, 앱 등록 및 사용 범위를 별도로 확인해야 합니다.

### 5.3 Vehicle SDK 연동

`MotionHorizonService`는 다음과 같은 방식으로 Vehicle SDK를 초기화합니다.

```java
Vehicle vehicle = new Vehicle(getApplicationContext());
vehicle.initialize();
```

현재 연동하는 SDK 도메인은 다음과 같습니다.

| SDK 도메인 | API | 앱에서 사용하는 값 |
| --- | --- | --- |
| Steering Wheel | `getSteeringWheel().registerSteeringWheelAngle(...)` | `Float` 조향각 콜백 |
| Brake | `getBrake().registerBrakePedalStatus(...)` | `Boolean` 페달 눌림 상태 |

각 리스너의 `onFailed(Exception)` 콜백은 오류를 로그로 남기고 세션을 종료합니다.

SDK 콜백을 UI 스레드에서 안전하게 처리하고, 서비스 종료 시 리스너 등록을 해제한 뒤 `vehicle.release()`를 호출합니다.

### 5.4 추가 차량 운동 데이터

멀미 완화 목적에 더 적합한 시각 효과를 구현하기 위해 다음 데이터의 제공 여부를 추가로 조사합니다.

- 종가속도(Longitudinal Acceleration)
- 횡가속도(Lateral Acceleration)
- 요레이트(Yaw Rate)
- 차량 속도(Vehicle Speed)

이 데이터들은 현재 확보된 SDK 연동 항목이 아닙니다.

공식 SDK에서 지원되는 API와 접근 권한을 확인하기 전까지 구현된 기능으로 간주하지 않습니다.

---

## 6. 내부 소프트웨어 아키텍처

향후 기능 확장과 테스트를 위해 차량 데이터 수집, 신호 처리, 시각적 매핑, 렌더링을 분리하는 구조를 지향합니다.

| 모듈 | 역할 |
| --- | --- |
| `VehicleDataProvider` | Pleos Vehicle SDK 연결 및 데이터 수신 |
| `MotionEngine` | 차량 데이터 통합 및 움직임 상태 관리 |
| `MotionFilter` | 노이즈 제거 및 신호 보간 |
| `MotionMapper` | 차량 데이터를 시각적 움직임으로 변환 |
| `MotionHorizonService` | 세션 및 오버레이 생명주기 관리 |
| `MotionHorizonView` | Canvas 기반 시각 효과 렌더링 |
| `HorizonRenderer` | 지평선 그래픽 렌더링 |
| `SettingsView` | 효과 활성화 및 사용자 설정 |

### 6.1 권장 프로젝트 구조

```text
.
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/example/byyourside/
│           ├── MainActivity.java
│           ├── service/
│           │   └── MotionHorizonService.java
│           ├── vehicle/
│           │   └── VehicleDataProvider.java
│           ├── motion/
│           │   ├── MotionEngine.java
│           │   ├── MotionFilter.java
│           │   └── MotionMapper.java
│           ├── ui/
│           │   ├── MotionHorizonView.java
│           │   ├── SettingsView.java
│           │   └── HorizonRenderer.java
│           └── model/
│               ├── VehicleMotion.java
│               └── HorizonState.java
├── web/
│   ├── index.html
│   ├── simulator.css
│   └── simulator.js
├── build.gradle
├── settings.gradle
├── gradle.properties
└── gradlew
```

위 구조는 목표 아키텍처입니다. 현재 저장소에 존재하는 파일과 향후 분리할 파일을 구분하며 단계적으로 적용합니다.

---

## 7. 웹 시뮬레이터

웹 시뮬레이터는 Android 앱과 Vehicle SDK에 의존하지 않는 독립적인 정적 페이지입니다.

Android 앱에서 사용하는 Ambient Horizon의 시각 효과를 브라우저에서 시험하는 것이 목적입니다.

### 7.1 시뮬레이션 환경

16:9 가상 차량 디스플레이를 기반으로 다음 배경을 제공합니다.

- 내비게이션 지도
- 미디어 재생 화면

가장자리 지평선은 별도의 최상위 Canvas에 렌더링합니다.

`pointer-events: none`을 사용하여 중앙 콘텐츠의 사용자 입력을 가로채지 않습니다.

### 7.2 주요 조작 기능

- 직접 조향: 휠 드래그 및 조향각 슬라이더
- 배경 앱 전환: 내비게이션 / 미디어
- 미디어 재생 및 일시정지
- 브레이크 입력: 0~100% 시각 효과 시뮬레이션
- 종가속도: -1.0~+1.0 G 시뮬레이션
- 횡방향 움직임 및 G-force 시각 효과 실험
- 노면 요철 입력
- 급정거 시뮬레이션
- 자동 S자 주행
- 초기 상태 복귀

### 7.3 시뮬레이션 데이터 주의사항

웹 시뮬레이터의 브레이크 강도, G-force, 노면 요철 및 자동 주행 입력은 시각 효과를 시험하기 위한 가상 데이터입니다.

실제 차량 센서값이나 SDK에서 제공하는 데이터가 아닙니다.

웹 시뮬레이터의 효과가 실제 차량 환경에서 멀미 완화 효과를 제공한다는 것을 의미하지도 않습니다.

---

## 8. 안전성 및 사용성 원칙

옆에서는 주행 중 사용되는 차량용 시각 인터페이스이므로 안전성을 핵심 설계 원칙으로 둡니다.

### 8.1 시각적 안전성

- 과도한 밝기 변화와 섬광 효과를 사용하지 않습니다.
- 급격하고 반복적인 애니메이션을 지양합니다.
- 지평선 움직임을 제한된 범위에서 표현합니다.
- 중앙 콘텐츠를 가리지 않습니다.
- 차량 시스템 UI와 경고 정보를 가리지 않습니다.
- 주변시로 인식할 수 있는 절제된 그래픽을 지향합니다.

### 8.2 운전자 주의 분산 최소화

본 앱의 주요 사용자는 동승자이지만 차량 화면은 운전자도 볼 수 있습니다.

따라서 운전자의 주의를 과도하게 끌지 않는 디자인을 적용합니다.

앱은 차량 주행을 제어하거나 운전자에게 주행 지시를 제공하지 않습니다.

### 8.3 데이터 오류 처리

- Vehicle SDK 연결 실패 시 시각 효과를 비활성화합니다.
- 데이터 수신이 중단되면 마지막 값을 무기한 유지하지 않습니다.
- 유효하지 않은 입력값은 렌더링에 반영하지 않습니다.
- 차량 데이터의 시간 정보를 관리하여 지연 및 오래된 데이터의 사용을 방지합니다.
- 조향 및 브레이크 입력이 3초 동안 갱신되지 않으면 해당 신호를 렌더링에서 제외합니다.
- 세션 종료 시 모든 차량 데이터 리스너를 해제합니다.

---

## 9. 사용자 검증 계획

옆에서의 핵심 가설은 차량의 움직임과 동기화된 주변시 시각 단서가 동승자의 멀미 증상 완화에 도움이 될 수 있다는 것입니다.

이를 검증하기 위해 다음과 같은 사용자 실험을 계획합니다.

### 9.1 실험 조건

| 조건 | 설명 |
| --- | --- |
| Control | 일반적인 화면 이용 |
| Experimental | 옆에서 Ambient Horizon 활성화 |

가능한 경우 동일한 참가자가 두 조건을 모두 경험하는 교차 실험으로 진행합니다.

주행 환경과 화면 콘텐츠를 최대한 통제하고 실험 순서에 따른 영향을 고려합니다.

### 9.2 측정 지표

- 멀미 증상 변화
- 불편함 발생까지 걸린 시간
- 화면 이용 지속 시간
- 시각 효과의 편안함
- 시각 효과의 과도한 자극 여부
- 전반적인 사용자 만족도

필요에 따라 SSQ(Simulator Sickness Questionnaire) 등의 검증된 설문 도구를 활용합니다.

실제 주행 실험은 안전한 통제 환경에서 동승자를 대상으로 진행하며, 증상이 악화되는 경우 즉시 중단합니다.

사용자 검증 전까지 멀미 완화 효과를 입증된 성능으로 주장하지 않습니다.

---

## 10. 빌드 및 실행

### 웹 시뮬레이터

저장소 루트에서 정적 서버를 실행합니다.

```bash
python3 -m http.server 8000 --directory web
```

브라우저에서 다음 주소를 엽니다.

```text
http://localhost:8000
```

Codespaces를 사용하는 경우 Ports 탭에서 8000번 포트를 엽니다.

### Android 앱

JDK 17과 Android SDK Platform 34가 필요합니다.

```bash
./gradlew build
```

Debug APK는 다음 경로에 생성됩니다.

```text
app/build/outputs/apk/debug/app-debug.apk
```

연결된 AAOS 장치나 에뮬레이터에 설치합니다.

```bash
adb devices -l

adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Virtual Console을 ADB로 연결한 환경에서는 포트 포워딩을 사용할 수 있습니다.

```bash
adb forward tcp:8088 tcp:8088
```

ADB 디바이스가 연결되지 않은 경우 포트 포워딩과 APK 설치는 실패합니다.

VS Code Ports 탭에서 포트 8088을 열어 Virtual Console 웹 화면에 접근할 수 있습니다.

---

## 11. 개발 로드맵

| 단계 | 개발 목표 | 주요 작업 |
| --- | --- | --- |
| Phase 1 | 기본 기능 | Vehicle SDK 연동 및 Ambient Horizon 구현 |
| Phase 2 | 기술 검증 | 오버레이 권한, 플랫폼 정책 및 추가 데이터 조사 |
| Phase 3 | 알고리즘 개선 | 데이터 필터링, 동기화 및 움직임 매핑 |
| Phase 4 | 제품 완성 | 동승자 UX, 세션 관리 및 안전성 개선 |
| Phase 5 | 효과 검증 | 사용자 실험 및 비교 평가 |
| Phase 6 | 공모전 준비 | 프로토타입, 시연 영상 및 사업화 전략 |

### 현재 핵심 개발 과제

1. Pleos Connect에서 `TYPE_APPLICATION_OVERLAY` 사용 가능 여부 확인
2. Vehicle SDK의 조향 및 브레이크 데이터 수신 안정성 검증
3. 필요한 차량 데이터 접근 권한 및 App Market 승인 범위 확인
4. 실제 차량 운동 데이터 API 확보 가능성 조사
5. 차량 움직임과 지평선 사이의 시간적 동기화 검증
6. 주행 중 시각적 안전성 및 멀미 완화 효과 검증

---

## 12. 기술적 한계 및 전제사항

본 프로젝트의 오버레이 구현은 Android `TYPE_APPLICATION_OVERLAY`를 사용하는 것을 전제로 설계합니다.

그러나 실제 Pleos Connect 환경에서 다른 앱 위에 오버레이를 표시할 수 있는지, 해당 기능이 App Market 정책상 허용되는지는 별도 확인이 필요합니다.

또한 Vehicle SDK에서 제공하는 조향각과 브레이크 페달 상태는 운전자의 조작 입력이며, 실제 차량 운동량을 직접 측정하는 데이터가 아닙니다.

가속도, 요레이트 등 추가 차량 운동 데이터는 공식 SDK 지원 여부와 접근 권한을 확인한 후 적용합니다.

본 앱은 멀미 완화를 목표로 하는 시각 보조 기술이며, 의료적 치료 또는 멀미 예방 효과를 보장하지 않습니다.

---

## 13. 프로젝트 철학

**옆에서는 차량의 움직임을 보여주는 앱이 아니라, 차량의 움직임을 느끼게 해주는 앱입니다.**

차량 안에서 화면을 보는 경험을 포기하지 않으면서도, 차량의 움직임을 시각적으로 인지할 수 있는 새로운 인터페이스를 제안합니다.

화면을 더 복잡하게 만드는 대신, 화면 가장자리의 작은 움직임으로 새로운 차량 경험을 만듭니다.

---

## License

This project is developed and submitted for the Hyundai Motor Group Pleos App Development Challenge.