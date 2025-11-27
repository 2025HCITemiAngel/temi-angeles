package com.example.temidummyapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class EventQRActivity extends BaseActivity {

    private Button btnQueue;
    private Button btnEvent;
    private FrameLayout qrContainer;
    private FrameLayout eventContainer;
    private TextView titleText;
    private ViewPager2 eventViewPager;
    private ImageButton btnEventPrev;
    private ImageButton btnEventNext;
    private EventDetailAdapter eventAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_qr);
        
        // ActionBar 숨기기
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 뒤로가기 버튼
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // UI 요소 초기화
        btnQueue = findViewById(R.id.btnQueue);
        btnEvent = findViewById(R.id.btnEvent);
        qrContainer = findViewById(R.id.qrContainer);
        eventContainer = findViewById(R.id.eventContainer);
        titleText = findViewById(R.id.titleText);
        eventViewPager = findViewById(R.id.eventViewPager);
        btnEventPrev = findViewById(R.id.btnEventPrev);
        btnEventNext = findViewById(R.id.btnEventNext);

        // 이벤트 데이터 초기화
        setupEventData();

        // 초기 상태: QR 코드 화면 표시 (제목은 레이아웃 파일의 기본값 사용)
        showQRView();

        // 지능형 로봇 현장 줄서기 버튼 클릭
        if (btnQueue != null) {
            btnQueue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showQRView();
                }
            });
        }

        // coshow 이벤트 안내 버튼 클릭
        if (btnEvent != null) {
            btnEvent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEventView();
                }
            });
        }
    }

    private void showQRView() {
        // QR 코드 화면 표시
        if (qrContainer != null) {
            qrContainer.setVisibility(View.VISIBLE);
        }
        if (eventContainer != null) {
            eventContainer.setVisibility(View.GONE);
        }
        
        // 버튼 스타일 업데이트
        if (btnQueue != null) {
            btnQueue.setBackgroundResource(R.drawable.button_selected);
            btnQueue.setBackgroundTintList(null);
            btnQueue.setTextColor(0xFFFFFFFF); // 흰색
        }
        if (btnEvent != null) {
            btnEvent.setBackgroundResource(R.drawable.button_unselected_selector);
            btnEvent.setBackgroundTintList(null);
            btnEvent.setTextColor(0xFF2B87F4); // 파란색
        }
        
        // 제목은 레이아웃 파일의 기본값을 사용하므로 변경하지 않음
    }

    private void showEventView() {
        // 이벤트 정보 화면 표시
        if (qrContainer != null) {
            qrContainer.setVisibility(View.GONE);
        }
        if (eventContainer != null) {
            eventContainer.setVisibility(View.VISIBLE);
        }
        
        // 버튼 스타일 업데이트
        if (btnQueue != null) {
            btnQueue.setBackgroundResource(R.drawable.button_unselected_selector);
            btnQueue.setBackgroundTintList(null);
            btnQueue.setTextColor(0xFF2B87F4); // 파란색
        }
        if (btnEvent != null) {
            btnEvent.setBackgroundResource(R.drawable.button_selected);
            btnEvent.setBackgroundTintList(null);
            btnEvent.setTextColor(0xFFFFFFFF); // 흰색
        }
        
        // 제목은 레이아웃 파일의 기본값을 사용하므로 변경하지 않음
    }

    private void setupEventData() {
        List<EventData> eventList = new ArrayList<>();

        // 이벤트 1: 이재모 피자 이벤트
        EventData event1 = new EventData();
        event1.setMainTitle("CO-SHOW 오면");
        event1.setSubTitle("이재모 피자를");
        event1.setDescription("웨이팅 없이 먹을 수 있다!");
        event1.setDisclaimer("* 선착순 소진 시 조기 종료될 수 있습니다.");
        event1.setImageResId(R.drawable.ic_event_pizza);
        event1.setContent("🍕 코아이의 깜짝 선물! 이재모피자 웨이팅 없이 먹자!\n" +
                "CO-SHOW에 와준 여러분께 감사한 마음을 담아\n" +
                "부산에서 유명한 이재모피자를 준비했어요!\n\n" +
                "✨ 웨이팅 없이 바로 먹는 이재모피자, 코쇼에서만 가능!\n" +
                "모~두에게 드리고 싶지만, 준비된 수량 소진 시 조기 마감될 수 있어요💛\n" +
                "그래도 최대한 많은 친구들이 먹을 수 있도록 코아이가 열심히 준비했대요!\n\n" +
                "📍 언제? 11/27(목) ~ 11/28(금) 11:00 ~ 17:00\n" +
                "📍 어디서? CO-SHOW 전시장 내 출구 방향\n\n" +
                "✔ 참여방법 (3가지 중 1개만 하면 OK!)\n\n" +
                "1. 코아이 인형탈을 찾아 코아이와 예쁘게 사진 찍고 인증하기\n\n" +
                "2. 전시장에서 사진 찍고 SNS 업로드 인증\n" +
                "→ CO-SHOW 전시를 배경으로 사진 촬영\n" +
                "→ 인스타/SNS 업로드 시\n" +
                "#COSHOW #COSS #첨단분야혁신융합대학 3개 해시태그 필수!\n\n" +
                "3. 수험생 인증\n" +
                "→ 수험표 or 수험생임을 확인할 수 있는 내용 인증하기\n\n" +
                "🍕 인증 완료하면, 맛있는 이재모 조각 피자 바로 드려요!\n" +
                "CO-SHOW에서 즐기고, 먹고, 추억까지 챙겨가세요💛");
        eventList.add(event1);

        // 이벤트 2: 수험생 이벤트
        EventData event2 = new EventData();
        event2.setMainTitle("2025 CO-SHOW");
        event2.setSubTitle("수험생 이벤트");
        event2.setDescription("수험표 인증만 해도 도장 2개!");
        event2.setDisclaimer("※ 선착순 1일 1,000명");
        event2.setImageResId(R.drawable.ic_event_highschool);
        event2.setContent("📣 수험표 들고 CO-SHOW로 출발!\n" +
                "수고했어요, 수험생 여러분 💪\n" +
                "이제는 즐길 시간이에요 ✨\n\n" +
                "CO-SHOW 현장에서 수험표 인증하면\n" +
                "🎁 도장 2개 즉시 지급!\n" +
                "스탬프투어 참여하고 푸짐한 경품까지 GET! 🎉\n\n" +
                "💡 참여 방법\n" +
                "1. 등록데스크에서 CO-SHOW 리플렛 수령\n" +
                "2. 전시장 내 이벤트 운영부스에서 수험표 인증 후 도장 2개 받기\n" +
                "3. 다양한 프로그램 즐기고 도장도 받고\n" +
                "4.  모든 도장을 획득했다면 이벤트 부스에서 경품 뽑기!\n\n" +
                "📍 위치: 전시장 내 이벤트 부스\n" +
                "🗓️ 기간: 2025.11.26(수) ~ 11.29(토) 상시 운영\n" +
                "※ 선착순 1일 1,000명\n\n" +
                "✨ 참여 꿀TIP!\n" +
                "수험표는 반드시 실물로 지참해주세요.\n" +
                "현장 운영부스 방문 시 인증 필수!\n" +
                "도장은 CO-SHOW 기간 내 상시 운영됩니다.\n\n" +
                "💡 스페셜 경품 : 갤럭시 워치6, 갤럭시 버즈3 프로, 키크론 V10 Pro Max " +
                "키보드, 키크론 B6 Pro 저슴 블루투스 키보드, COSS-BALL 키링");
        eventList.add(event2);

        // ViewPager2 어댑터 설정
        eventAdapter = new EventDetailAdapter(eventList);
        if (eventViewPager != null) {
            eventViewPager.setAdapter(eventAdapter);
            eventViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateNavigationButtons(position);
                }
            });
        }

        // 화살표 버튼 클릭 리스너
        if (btnEventPrev != null) {
            btnEventPrev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eventViewPager != null && eventViewPager.getCurrentItem() > 0) {
                        eventViewPager.setCurrentItem(eventViewPager.getCurrentItem() - 1, true);
                    }
                }
            });
        }

        if (btnEventNext != null) {
            btnEventNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eventViewPager != null && eventAdapter != null) {
                        int currentItem = eventViewPager.getCurrentItem();
                        if (currentItem < eventAdapter.getItemCount() - 1) {
                            eventViewPager.setCurrentItem(currentItem + 1, true);
                        }
                    }
                }
            });
        }

        // 초기 네비게이션 버튼 상태 설정
        if (eventAdapter != null && eventAdapter.getItemCount() > 0) {
            updateNavigationButtons(0);
        }
    }

    private void updateNavigationButtons(int position) {
        if (btnEventPrev == null || btnEventNext == null || eventAdapter == null) {
            return;
        }

        int itemCount = eventAdapter.getItemCount();
        
        // 첫 번째 페이지면 이전 버튼 숨김
        if (position == 0) {
            btnEventPrev.setVisibility(View.GONE);
        } else {
            btnEventPrev.setVisibility(View.VISIBLE);
        }

        // 마지막 페이지면 다음 버튼 숨김
        if (position >= itemCount - 1) {
            btnEventNext.setVisibility(View.GONE);
        } else {
            btnEventNext.setVisibility(View.VISIBLE);
        }
    }
}

