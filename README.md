Markdown
# 🚗 옆에서 (Yeop-eoseo)
> **Pleos Connect 기반 차량 데이터 연동 비침습적 앰비언트 멀미 방지 시스템**  
> *Non-Intrusive Motion Sickness Mitigation via Pleos Vehicle SDK & Ambient Horizon Overlay Architecture*

---

## 🤖 IDE AI Agent Prompt Instruction (초기 프로젝트 자동 생성 가이드)

> **[Prompt for Cursor / GitHub Copilot Agent]**  
> Read this entire `README.md` specification and initialize the complete, production-ready Android Automotive OS (AAOS) repository.  
> 1. Set up Gradle 8.0 wrapper (`gradle-wrapper.properties` targeting `https://services.gradle.org/distributions/gradle-8.0-bin.zip` and executable `gradlew`).  
> 2. Configure `settings.gradle` with Maven repository `https://nexus-playground.pleos.ai/repository/maven-releases/` and module `:app`.  
> 3. Configure `app/build.gradle` with Java 17, `minSdk 28`, `compileSdk 34`, and official Pleos SDK dependency `ai.pleos.playground:Vehicle:2.3.2`.  
> 4. Declare permissions (`android.permission.SYSTEM_ALERT_WINDOW`, `pleos.car.permission.CAR_DRIVE`, `pleos.car.permission.CAR_INFO`) in `app/src/main/AndroidManifest.xml`.  
> 5. Create core source files (`MotionHorizonView.java`, `MotionHorizonService.java`, `MainActivity.java`) with proper package name `com.example.byyourside`.  
> 6. Ensure `./gradlew build` completes with `BUILD SUCCESSFUL`.

---

## 📌 프로젝트 소개 (Overview)

**'옆에서(Yeop-eoseo)'**는 자동차 동승자가 미디어 감상, 내비게이션 검색, 화면 조작을 할 때 발생하는 **감각 불일치(Sensory Conflict) 멀미**를 예방하는 차량용 시각 웰니스 디스플레이 플랫폼입니다.

디스플레이 최외곽 5px 영역에 **반투명 앰비언트 가상 지평선 오버레이**를 렌더링하여, 미디어 몰입성을 보존하면서 안구 주변시(Peripheral Vision)로 차량의 실시간 물리 운동감을 전달합니다.

---

## 🛠️ 개발 및 빌드 환경 명세 (Environment Setup)

Pleos Playground 공식 개발자 포털 가이드를 100% 준수합니다.

* **Target OS / Platform:** Android Automotive OS (AAOS), API 28+ (Android 9.0+)
* **Build System:** Gradle 8.0+ / Android Gradle Plugin 8.1.1
* **JDK Version:** Java 17 (JDK 17)
* **Official SDK Dependency:** `ai.pleos.playground:Vehicle:2.3.2`
* **SDK Maven Repository:** `https://nexus-playground.pleos.ai/repository/maven-releases/`

---

## 🏗️ 필수 프로젝트 파일 및 설정 명세 (File Specifications)

### 1. `settings.gradle` (루트)
```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("[https://nexus-playground.pleos.ai/repository/maven-releases/](https://nexus-playground.pleos.ai/repository/maven-releases/)") }
    }
}

rootProject.name = "hyundai_app"
include ':app'
2. build.gradle (루트)
Groovy
plugins {
    id 'com.android.application' version '8.1.1' apply false
}
3. app/build.gradle (앱 모듈)
Groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.example.byyourside'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.byyourside"
        minSdk 28
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'ai.pleos.playground:Vehicle:2.3.2'
}
4. app/src/main/AndroidManifest.xml
XML
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    package="com.example.byyourside">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="pleos.car.permission.CAR_DRIVE" />
    <uses-permission android:name="pleos.car.permission.CAR_INFO" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="옆에서"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.MotionHorizonService"
            android:enabled="true"
            android:exported="false" />

    </application>
</manifest>
📂 패키지 및 컴포넌트 아키텍처 (Source Architecture)
Plaintext
com.example.byyourside/
├── service/
│   └── MotionHorizonService.java    // WindowManager 최상단 TYPE_APPLICATION_OVERLAY 및 Pleos Vehicle SDK 연동
├── ui/
│   ├── MotionHorizonView.java       // 60fps VSYNC 보간, 최외곽 5px 글로우 Canvas 렌더링 뷰
│   └── MainActivity.java            // SYSTEM_ALERT_WINDOW 런타임 권한 승인 및 서비스 시동
💡 핵심 기술 특징
터미널 터치 100% 투과 오버레이:

WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE 및 FLAG_NOT_FOCUSABLE을 적용하여 밑에 깔린 유튜브/내비게이션 조작 터치 이벤트 방해 요소를 완벽히 제거.

Pleos Vehicle SDK 라이프사이클 관리:

Vehicle 인스턴스 초기화(initialize()), 비동기 콜백 수신, release() 리소스 해제 흐름 준수.

🚀 빌드 및 검증 (Build & Execution)
Bash
# 1. Gradle 8.0 래퍼 환경 확인 및 권한 부여
chmod +x gradlew

# 2. 빌드 실행
./gradlew build
📄 라이선스 (License)
This project is submitted for Hyundai Pleos App Contest.


---

### 💡 활용하는 방법
1. 위 마크다운 코드를 복사해서 `README.md`에 넣고 저장합니다.
2. IDE AI(Copilot / Cursor) 채팅창에 **"README.md 파일 상단의 AI Prompt Instruction 내용대로