# 🤖 NavigatingActivity - 테미 이동 중 얼굴 화면

## 📋 개요

테미가 목적지로 이동할 때 표시되는 **전체 화면 Activity**입니다.
기존의 작은 AlertDialog 대신, 큰 얼굴 화면으로 이동 상태를 시각적으로 표시합니다.

---

## ✨ 주요 기능

### 1️⃣ 전체 화면 얼굴 표시
- 파란색 배경에 테미 얼굴 이미지
- 큰 텍스트로 이동 상태 표시
- 로딩 인디케이터로 진행 중임을 강조

### 2️⃣ 터치로 이전 화면 복귀
```
사용자가 화면 터치
    ↓
NavigatingActivity.finish()
    ↓
이전 화면(MainActivity 또는 DirectionsActivity)로 복귀
```

**기존 AlertDialog 동작과 동일:**
- 모달 외부 터치 → 닫힘
- NavigatingActivity 터치 → Activity 종료 (이전 화면 복귀)

### 3️⃣ 자동 종료
```
테미가 목적지 도착
    ↓
onGoToLocationStatusChanged() 콜백
    ↓
finish() 자동 호출
    ↓
이전 화면으로 자동 복귀
```

---

## 🎨 화면 구성

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│          [테미 얼굴 이미지]          │  (300x300dp)
│                                     │
│                                     │
│      "등록데스크" 으로 이동 중입니다  │  (32sp, 볼드)
│                                     │
│     잠시만 길을 내어주세요          │  (18sp)
│   (화면을 터치하면 이전 화면으로...)  │
│                                     │
│              [로딩 바]              │
│                                     │
└─────────────────────────────────────┘
```

---

## 🔄 동작 플로우

### 일반 이동 시나리오

```
[MainActivity 또는 DirectionsActivity]
    ↓
사용자가 목적지 선택 (예: "등록데스크")
    ↓
startNavigation("등록데스크") 호출
    ↓
startNavigatingActivity("등록데스크") 호출
    ↓
[NavigatingActivity 시작]
    - 전체 화면으로 얼굴 표시
    - "'등록데스크' 으로 이동 중입니다" 표시
    - robot.goTo("등록데스크") 실행
    ↓
테미가 이동 중...
    ↓
[3가지 종료 경로]
    1) 사용자가 화면 터치 → finish() → 이전 화면 복귀
    2) 테미 도착 → 자동 finish() → 이전 화면 복귀
    3) Back 버튼 (기본 Android 동작)
```

### 드래그 후 재시도 시나리오

```
[NavigatingActivity에서 이동 중]
    ↓
사용자가 테미 머리를 잡음 (드래그)
    ↓
이동 중단
    ↓
사용자가 손을 뗌
    ↓
onRobotDragStateChanged(false) 콜백
    ↓
MainActivity/DirectionsActivity에서:
    - startNavigatingActivity(currentDestination) 재호출
    - robot.goTo(currentDestination) 재시도
    ↓
[NavigatingActivity 다시 시작]
```

---

## 📱 화면 상태 업데이트

NavigatingActivity는 실시간으로 이동 상태를 모니터링합니다:

| 이동 상태 | 표시 메시지 |
|-----------|-------------|
| **calculate** | "경로를 계산하고 있습니다..." |
| **start / going** | "'목적지' 으로 이동 중입니다" |
| **obstacle** | "장애물을 피하고 있습니다..." |
| **abort / cancel** | "이동이 중단되었습니다" |
| **arrived / complete** | Toast: "도착했습니다" → 자동 종료 |

---

## 🎯 기존 AlertDialog와 비교

### 기존 (AlertDialog)

```
┌──────────────────────┐
│     이동 중          │  ← 작은 모달 창
│                      │
│ 이동 안내 중입니다!  │
│ 잠시만 길을...       │
└──────────────────────┘
```

**한계:**
- ❌ 작은 화면 (전체의 30% 정도)
- ❌ 시각적 임팩트 부족
- ❌ 브랜드 정체성 표현 어려움

### 현재 (NavigatingActivity)

```
┌───────────────────────────────────┐
│                                   │
│                                   │
│       [큰 테미 얼굴]              │  ← 전체 화면
│                                   │
│  "'등록데스크' 으로 이동 중입니다" │
│                                   │
│       [로딩 애니메이션]            │
│                                   │
└───────────────────────────────────┘
```

**장점:**
- ✅ 전체 화면 사용
- ✅ 큰 얼굴 이미지로 브랜드 강조
- ✅ 명확한 상태 표시
- ✅ 터치로 쉽게 복귀

---

## 🔧 커스터마이징

### 얼굴 이미지 변경

`activity_navigating.xml`에서:

```xml
<ImageView
    android:id="@+id/temi_face"
    android:src="@drawable/ic_launcher_foreground"  ← 여기 수정
    android:layout_width="300dp"
    android:layout_height="300dp" />
```

**권장 이미지:**
- 테미 캐릭터 얼굴
- 애니메이션 GIF (AnimationDrawable 사용)
- 로티(Lottie) 애니메이션

### 배경색 변경

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:background="#4A90E2"  ← 파란색 (커스터마이징 가능)
```

**추천 색상:**
- `#4A90E2` - 밝은 파란색 (현재)
- `#6A5ACD` - 슬레이트 블루
- `#00BCD4` - 청록색 (Cyan)

### 텍스트 크기 조절

```xml
<TextView
    android:id="@+id/status_text"
    android:textSize="32sp"  ← 상태 텍스트
    android:textSize="18sp"  ← 안내 텍스트
```

---

## 🐛 트러블슈팅

### 문제 1: Activity가 시작되지 않음

**원인:** AndroidManifest.xml에 등록 안 됨

**해결:**
```xml
<activity
    android:name=".NavigatingActivity"
    android:exported="false"
    android:screenOrientation="landscape" />
```

### 문제 2: 화면 터치해도 안 닫힘

**원인:** `root_layout`의 clickable 설정 문제

**해결:**
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:id="@+id/root_layout"
    android:clickable="true"  ← 필수
    android:focusable="true"  ← 필수
```

### 문제 3: 도착해도 자동으로 안 닫힘

**원인:** Robot listener 등록 안 됨

**해결:** `onStart()`와 `onStop()`에서 listener 등록/해제 확인

```java
@Override
protected void onStart() {
    super.onStart();
    robot.addOnGoToLocationStatusChangedListener(this);
}
```

---

## 📊 테스트 체크리스트

### 기본 동작
- [ ] 이동 시작 시 NavigatingActivity 표시됨
- [ ] 화면 터치 시 이전 화면으로 복귀
- [ ] 도착 시 자동으로 Activity 종료됨
- [ ] Back 버튼으로 종료 가능

### 상태 업데이트
- [ ] "경로 계산 중..." 메시지 표시
- [ ] "이동 중..." 메시지 표시
- [ ] "장애물 회피..." 메시지 표시
- [ ] "도착" Toast 표시

### 드래그 시나리오
- [ ] 드래그 시 NavigatingActivity 유지
- [ ] 드래그 해제 후 재시도 시 다시 NavigatingActivity 표시

### 화면 전환
- [ ] MainActivity → NavigatingActivity 전환 부드러움
- [ ] DirectionsActivity → NavigatingActivity 전환 부드러움
- [ ] 전체 화면 모드 유지됨 (상태바/네비게이션바 숨김)

---

## 🚀 향후 개선 아이디어

### 1. 애니메이션 추가
```java
// Lottie 애니메이션 사용
LottieAnimationView animationView = findViewById(R.id.temi_face);
animationView.setAnimation("temi_walking.json");
animationView.playAnimation();
```

### 2. 진행률 표시
```java
// 목적지까지 거리 계산 및 진행률 표시
int progress = calculateProgress(currentLocation, destination);
progressBar.setProgress(progress);
```

### 3. 음성 피드백
```java
// 특정 구간 통과 시 안내
if (passedCheckpoint) {
    robot.speak("곧 도착합니다!");
}
```

### 4. 테마별 화면
```java
// 시간대별 테마
if (isNightTime()) {
    rootLayout.setBackgroundColor(Color.parseColor("#2C3E50"));
}
```

---

## 📞 문의 및 지원

이동 중 화면 관련 문제가 있으시면:
1. LogCat에서 "NavigatingActivity" 태그 확인
2. 이동 상태 로그 확인
3. Robot SDK 버전 확인

**개발자:** temi-angeles 팀  
**마지막 업데이트:** 2025-11-27

---

✅ **구현 완료!** 이제 테미가 이동할 때 큰 얼굴 화면이 표시됩니다! 🎉

