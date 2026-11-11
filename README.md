Markdown
# 옆에서 (Yeop-eoseo)

Pleos Vehicle SDK의 차량 상태를 화면 가장자리의 앰비언트 지평선으로 표현하는 Android 앱과, 같은 시각 효과를 브라우저에서 시험하는 Canvas 시뮬레이터입니다.

## 기능

- Android `TYPE_APPLICATION_OVERLAY`로 화면 가장자리에 지평선을 표시합니다.
- `FLAG_NOT_TOUCHABLE`과 `FLAG_NOT_FOCUSABLE`로 오버레이가 터치나 포커스를 가로채지 않도록 구성했습니다.
- 조향 상태를 좌우 롤로, 브레이크 페달 상태를 수직 이동과 민트색 글로우 펄스로 표현합니다.
- `web/` 시뮬레이터에서 지도/미디어 멀티앱 배경, 조향, 제동, G-force, 노면 요철, 자동 S자 주행을 브라우저로 시험할 수 있습니다.

## 프로젝트 구성

```text
.
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/example/byyourside/
│           ├── service/MotionHorizonService.java
│           └── ui/
│               ├── MainActivity.java
│               └── MotionHorizonView.java
├── web/
│   ├── index.html
│   ├── simulator.css
│   └── simulator.js
├── build.gradle
├── settings.gradle
└── gradlew
```

## Android 앱

### 개발 환경

| 항목 | 버전/값 |
| --- | --- |
| 플랫폼 | Android / AAOS, `minSdk 28`, `targetSdk 34` |
| Android SDK | `compileSdk 34` |
| JDK | 17 |
| Gradle Wrapper | 8.0 |
| Android Gradle Plugin | 8.1.1 |
| Pleos SDK | `ai.pleos.playground:Vehicle:2.3.2` |

Pleos 의존성은 `https://nexus-playground.pleos.ai/repository/maven-releases/`에서 가져옵니다. AndroidX 사용을 위해 `gradle.properties`에서 `android.useAndroidX=true`를 설정합니다.

### 권한

`app/src/main/AndroidManifest.xml`에는 다음 권한이 선언되어 있습니다.

- `android.permission.SYSTEM_ALERT_WINDOW`: 앱 외부에 오버레이 표시
- `pleos.car.permission.CAR_DRIVE`: 조향 및 브레이크 상태 접근
- `pleos.car.permission.CAR_INFO`: 차량 정보 도메인 접근을 위한 선언

오버레이 권한은 일반 런타임 다이얼로그가 아니라 Android 설정 화면에서 사용자가 허용해야 합니다. `MainActivity`는 권한 화면을 열고, 복귀 후 서비스 시작/중지를 제공합니다.

### Vehicle SDK 연동

`MotionHorizonService`는 `Vehicle(getApplicationContext())`를 생성하고 `initialize()`를 호출한 뒤 실제 SDK 도메인 리스너를 등록합니다.

| SDK 도메인 | 실제 v2.3.2 API | 앱에서 사용하는 값 |
| --- | --- | --- |
| Steering Wheel | `getSteeringWheel().registerSteeringWheelAngle(...)` | `Float` 조향각 콜백 |
| Brake | `getBrake().registerBrakePedalStatus(...)` | `Boolean` 페달 눌림 상태 |

각 리스너의 `onFailed(Exception)` 콜백은 로그로 처리합니다. SDK 콜백은 메인 스레드로 전달하며, 서비스 종료 시 콜백 전달을 차단하고 리스너 등록 해제 후 `vehicle.release()`를 호출합니다.

> SDK v2.3.2의 브레이크 리스너는 `BrakePedalStatus`의 Boolean 상태를 전달합니다. 네이티브 앱에서 `BrakePedalPosition`이나 제동 강도값을 받는 것으로 간주하지 않습니다. 웹의 0–100% 브레이크 슬라이더는 시각 효과 조절용 시뮬레이션 입력입니다.

### 렌더링 동작

- 조향각은 `−540°..+540°`로 제한하고 `−15°..+15°`의 지평선 롤로 매핑합니다.
- 매 VSYNC 프레임에서 고정 저역통과 계수 `alpha = 0.12`로 현재값을 목표값에 보간합니다.
- 브레이크 상태는 지평선을 최대 5px 수직 이동시키고, 가장자리 선폭을 기본 5px에서 최대 8px로 확장합니다.
- 브레이크 입력 시 민트색 글로우가 펄스 효과를 냅니다. 이 효과는 주변시 시각 앵커용이며 차량의 실제 가속도/감속도를 계산하지 않습니다.
- 가장자리 효과는 좌우 최외곽 영역에 클리핑해 그립니다.

## 웹 시뮬레이터

웹 시뮬레이터는 `web/index.html`을 진입점으로 사용하는 독립적인 정적 페이지입니다. Android 앱이나 Vehicle SDK 연결 없이 브라우저에서 작동합니다.

16:9 가상 디스플레이에서 내비게이션 지도 또는 미디어 재생 화면을 배경 앱으로 전환할 수 있습니다. 지도 모드에서는 중앙 캔버스를 드래그하거나 휠/확대 버튼으로 조작합니다. 가장자리 지평선은 별도 최상단 Canvas에 그리며 `pointer-events: none`으로 설정되어 중앙 앱 입력을 가로채지 않습니다.

### 조작

- **직접 조향:** 조향 휠을 드래그하거나 `−540°..+540°` 슬라이더를 조정합니다. 휠에 포커스한 상태에서 방향키로도 움직일 수 있습니다.
- **배경 앱:** 내비게이션과 미디어 화면을 전환합니다. 미디어 화면의 재생/일시정지와 탐색 바도 조작할 수 있습니다.
- **브레이크 및 G-force:** 브레이크 `0..100%` 슬라이더와 종가속 `−1.0..+1.0 G` 슬라이더로 pitch shift와 글로우 반응을 확인합니다.
- **요철 잔진동:** `0..100%` 강도 슬라이더가 17Hz 입력을 조향 신호에 더해 저역통과 효과를 확인합니다.
- **지도 입력:** 중앙 지도 영역을 드래그해 패닝하고 마우스 휠 또는 화면의 확대/축소 버튼으로 줌을 조절합니다. 최상단 지평선 레이어는 포인터 이벤트를 받지 않습니다.
- **급정거 시뮬레이션:** 브레이크 입력을 잠시 100%로 올렸다가 해제합니다.
- **S자 코너링 주행:** 사인파 조향 입력으로 자동 주행합니다.
- **초기 상태로 복귀:** 초기화 버튼 또는 `R` 키를 사용합니다.

웹판도 `alpha = 0.12`, `±15°` 롤 제한, 5px 기본/8px 최대 가장자리 펄스 폭을 사용합니다. 웹의 요철 입력과 브레이크 강도는 화면 동작을 시험하기 위한 모델이며, 실제 차량 센서 데이터가 아닙니다.

### 실행

저장소 루트에서 정적 서버를 실행합니다.

```bash
python3 -m http.server 8000 --directory web
```

브라우저에서 `http://localhost:8000`을 열거나 Codespaces의 **Ports** 탭에서 포트 `8000`을 엽니다. 서버를 종료하려면 실행 중인 터미널에서 `Ctrl+C`를 누릅니다.

## 빌드 및 설치

JDK 17과 Android SDK Platform 34가 필요합니다.

```bash
./gradlew build
```

Debug APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. 연결된 AAOS 장치나 에뮬레이터에 설치하려면 다음을 실행합니다.

```bash
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Virtual Console을 ADB로 연결한 환경에서는 포트 포워딩을 사용할 수 있습니다.

```bash
adb forward tcp:8088 tcp:8088
```

ADB 디바이스가 연결되지 않은 경우 포워딩과 APK 설치는 실패합니다. VS Code Ports 탭에서 포트 `8088`을 열어 Virtual Console 웹 화면에 접근합니다.

## 라이선스

This project is submitted for Hyundai Pleos App Contest.