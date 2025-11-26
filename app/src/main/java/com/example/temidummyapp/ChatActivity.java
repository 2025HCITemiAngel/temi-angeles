package com.example.temidummyapp;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatActivity extends BaseActivity {
    private static final String TAG = "ChatActivity";
    private static final int KEYBOARD_HEIGHT_THRESHOLD = 150;

    private ChatAdapter chatAdapter;
    private OpenAIService openAIService;
    private ApiKeyManager apiKeyManager;
    private ChatStorage chatStorage;
    private RecyclerView chatList;
    private EditText inputMessage;
    private Button btnSend;
    private Button btnReset;
    private View backButton;
    private boolean isWaitingForResponse = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 전체화면 모드 및 리스너 설정
        setupImmersiveMode();
        setupKeyboardListener();
        stopWakeWordService();

        // OpenAI 서비스 초기화
        setupOpenAI();

        // 채팅 저장소 초기화
        chatStorage = new ChatStorage(this);

        // UI 초기화
        initializeViews();

        // 저장된 채팅 기록 불러오기 또는 환영 메시지 표시
        loadOrInitializeChat();
    }

    private WakeWordService getWakeWordService() {
        if (getApplication() instanceof TemiApplication) {
            TemiApplication app = (TemiApplication) getApplication();
            return (app != null) ? app.getWakeWordService() : null;
        }
        return null;
    }

    private void stopWakeWordService() {
        WakeWordService service = getWakeWordService();
        if (service != null && service.isListening()) {
            service.stopListening();
            Log.d(TAG, "Wake Word 감지 일시 중지");
        }
    }

    private void startWakeWordService() {
        WakeWordService service = getWakeWordService();
        if (service != null && !service.isListening()) {
            service.startListening();
            Log.d(TAG, "Wake Word 감지 다시 시작");
        }
    }

    private void setupImmersiveMode() {
        // 마시멜로(API 23)에서 adjustResize와 Fullscreen 모드가 충돌하므로
        // 바를 투명하게 만들고 컨텐츠가 바 뒤로 확장되도록 설정

        // 상단바와 네비게이션 바를 투명하게 설정
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }

        // 컨텐츠가 바 뒤로 확장되도록 설정 (adjustResize는 계속 작동)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void initializeViews() {
        setupBackButton();
        setupResetButton();
        setupChatList();
        setupInputAndSendButton();
    }

    private void setupBackButton() {
        backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                // 메시지 수신 중이면 무시
                if (isWaitingForResponse) {
                    Toast.makeText(this, "메시지를 받는 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 채팅 기록 저장
                saveChatHistory();
                finish();
            });
        }
    }

    private void setupResetButton() {
        btnReset = findViewById(R.id.btn_reset);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                // 확인 다이얼로그 표시
                new android.app.AlertDialog.Builder(this)
                        .setTitle("채팅 초기화")
                        .setMessage("모든 채팅 기록이 삭제됩니다. 계속하시겠습니까?")
                        .setPositiveButton("확인", (dialog, which) -> {
                            resetChat();
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });
        }
    }

    private void setupChatList() {
        chatList = findViewById(R.id.chat_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 메시지가 아래부터 쌓이도록
        chatList.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter();
        chatList.setAdapter(chatAdapter);

        chatList.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
            }
            return false;
        });

        // 키보드 변화 감지하여 자동 스크롤
        chatList.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                // 키보드가 올라왔을 때 - 마지막 메시지로 스크롤
                chatList.postDelayed(() -> {
                    if (chatAdapter.getItemCount() > 0) {
                        chatList.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                }, 100);
            }
        });
    }

    private void setupInputAndSendButton() {
        inputMessage = findViewById(R.id.input_message);
        btnSend = findViewById(R.id.btn_send);

        btnSend.setOnClickListener(v -> sendUserMessage());

        // Enter 키로도 전송 가능
        inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendUserMessage();
            return true;
        });
    }

    private void sendUserMessage() {
        String text = inputMessage.getText() != null ? inputMessage.getText().toString().trim() : "";

        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.chat_empty_message), Toast.LENGTH_SHORT).show();
            return;
        }

        if (isWaitingForResponse) {
            Toast.makeText(this, "응답을 기다리는 중입니다...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!openAIService.hasApiKey()) {
            showApiKeyDialog();
            return;
        }

        // 사용자 메시지 추가
        ChatMessage userMessage = new ChatMessage(text, ChatMessage.TYPE_USER);
        chatAdapter.addMessage(userMessage);
        scrollToBottom();

        // 입력창 초기화
        inputMessage.setText("");
        hideKeyboard();

        // GPT 응답 요청
        requestBotResponse();
    }

    private void requestBotResponse() {
        isWaitingForResponse = true;
        setButtonsEnabled(false);

        // 빈 봇 메시지 추가 (스트리밍으로 채워질 예정)
        ChatMessage botMessage = new ChatMessage("", ChatMessage.TYPE_BOT);
        chatAdapter.addMessage(botMessage);
        final int botMessageIndex = chatAdapter.getMessages().size() - 1;
        scrollToBottom();

        // 스트리밍 요청
        openAIService.sendMessageStreaming(chatAdapter.getMessages(), new OpenAIService.StreamCallback() {
            @Override
            public void onStream(String chunk) {
                // 실시간으로 텍스트 추가
                ChatMessage currentMessage = chatAdapter.getMessages().get(botMessageIndex);
                currentMessage.setMessage(currentMessage.getMessage() + chunk);
                chatAdapter.notifyItemChanged(botMessageIndex);
                scrollToBottom();
            }

            @Override
            public void onComplete() {
                isWaitingForResponse = false;
                setButtonsEnabled(true);
                Log.d(TAG, "스트리밍 완료");

                // 메시지가 비어있으면 에러 처리
                ChatMessage finalMessage = chatAdapter.getMessages().get(botMessageIndex);
                if (finalMessage.getMessage().isEmpty()) {
                    finalMessage.setMessage("응답을 받지 못했습니다.");
                    chatAdapter.notifyItemChanged(botMessageIndex);
                }
            }

            @Override
            public void onError(String error) {
                isWaitingForResponse = false;
                setButtonsEnabled(true);

                // 에러 메시지로 업데이트
                ChatMessage errorMessage = chatAdapter.getMessages().get(botMessageIndex);
                errorMessage.setMessage("죄송합니다. 오류가 발생했습니다: " + error);
                chatAdapter.notifyItemChanged(botMessageIndex);
                scrollToBottom();

                Log.e(TAG, "GPT 스트리밍 오류: " + error);
            }
        });
    }

    /**
     * 버튼들의 활성화/비활성화 상태 및 시각적 효과 설정
     */
    private void setButtonsEnabled(boolean enabled) {
        // 전송 버튼
        btnSend.setEnabled(enabled);

        // 뒤로가기 버튼
        if (backButton != null) {
            backButton.setEnabled(enabled);
            backButton.setAlpha(enabled ? 1.0f : 0.3f); // 비활성화 시 30% 투명도
        }

        // 초기화 버튼
        if (btnReset != null) {
            btnReset.setEnabled(enabled);
            btnReset.setAlpha(enabled ? 1.0f : 0.5f); // 비활성화 시 50% 투명도
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // 마시멜로에서 키보드 작동을 위해 바를 투명하게 유지
        if (hasFocus) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            }
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 바를 투명하게 설정
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startWakeWordService();
    }

    private void setupKeyboardListener() {
        View rootLayout = findViewById(R.id.root_layout);
        if (rootLayout == null)
            return;

        final int[] lastKeypadHeight = { 0 };

        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect r = new android.graphics.Rect();
            rootLayout.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootLayout.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            // 키보드 높이 변화 감지
            if (Math.abs(keypadHeight - lastKeypadHeight[0]) > 50) {
                if (keypadHeight > KEYBOARD_HEIGHT_THRESHOLD) {
                    // 키보드가 올라왔을 때
                    Log.d(TAG, "마시멜로 키보드 올라옴: " + keypadHeight + "px");
                    // adjustResize가 자동으로 레이아웃 조정
                    rootLayout.postDelayed(this::scrollToBottom, 100);
                } else {
                    // 키보드가 내려갔을 때
                    Log.d(TAG, "마시멜로 키보드 내려감");
                }
                lastKeypadHeight[0] = keypadHeight;
            }
        });
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            chatList.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    private void setupOpenAI() {
        openAIService = new OpenAIService();
        apiKeyManager = new ApiKeyManager(this);

        // API 키 직접 설정
        String apiKey = "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";
        apiKeyManager.saveApiKey(apiKey);
        openAIService.setApiKey(apiKey);

        Log.d(TAG, "OpenAI API 키 설정 완료");
    }

    private void showApiKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("OpenAI API 키 설정");
        builder.setMessage("챗봇 기능을 사용하려면 OpenAI API 키가 필요합니다.\n\nAPI 키를 입력해주세요:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("sk-proj-...");
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            String apiKey = input.getText().toString().trim();
            if (!apiKey.isEmpty()) {
                apiKeyManager.saveApiKey(apiKey);
                openAIService.setApiKey(apiKey);
                Toast.makeText(this, "API 키가 저장되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "API 키를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> {
            dialog.cancel();
            Toast.makeText(this, "API 키 없이는 챗봇을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
                "안녕하세요! 행사장 안내 챗봇입니다.\n\n" +
                        "행사장 정보, 부스 위치, 이벤트 일정 등 궁금한 점을 물어보세요.",
                ChatMessage.TYPE_BOT);
        chatAdapter.addMessage(welcomeMessage);
    }

    private void loadOrInitializeChat() {
        if (chatStorage.hasMessages()) {
            // 저장된 채팅 기록 불러오기
            List<ChatMessage> savedMessages = chatStorage.loadMessages();
            for (ChatMessage message : savedMessages) {
                chatAdapter.addMessage(message);
            }
            scrollToBottom();
            Log.d(TAG, "저장된 채팅 기록 불러옴: " + savedMessages.size() + "개");
        } else {
            // 첫 실행 시 환영 메시지 표시
            addWelcomeMessage();
        }
    }

    private void saveChatHistory() {
        List<ChatMessage> messages = chatAdapter.getMessages();
        chatStorage.saveMessages(messages);
        Log.d(TAG, "채팅 기록 저장됨: " + messages.size() + "개");
    }

    private void resetChat() {
        // 채팅 기록 삭제
        chatStorage.clearMessages();
        chatAdapter.clearMessages();

        // 환영 메시지 다시 표시
        addWelcomeMessage();
        scrollToBottom();

        Toast.makeText(this, "채팅이 초기화되었습니다.", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "채팅 초기화 완료");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 앱이 백그라운드로 갈 때 자동 저장
        saveChatHistory();
    }
}
