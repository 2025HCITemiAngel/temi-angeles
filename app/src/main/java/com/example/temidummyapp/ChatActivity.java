package com.example.temidummyapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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

    // STT 관련
    private SpeechToTextService sttService; // 배치 방식
    private RealtimeSTTService realtimeSTTService; // 실시간 방식
    private ImageButton btnMic;
    private View listeningOverlay;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO_STT = 1002;

    // STT 모드
    private enum STTMode {
        REALTIME, // 실시간 입력
        BATCH // 다 듣고 입력
    }

    private STTMode currentSTTMode = STTMode.REALTIME; // 기본값: 실시간
    private TextView btnSTTRealtime;
    private TextView btnSTTBatch;
    private StringBuilder realtimeTextBuffer = new StringBuilder(); // 실시간 텍스트 버퍼

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

        // STT 서비스 초기화
        setupSTT();

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
        btnMic = findViewById(R.id.btn_mic);
        listeningOverlay = findViewById(R.id.listening_overlay);

        btnSend.setOnClickListener(v -> sendUserMessage());

        // Enter 키로도 전송 가능
        inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendUserMessage();
            return true;
        });

        // 마이크 버튼 클릭 - 녹음 시작
        btnMic.setOnClickListener(v -> startListening());

        // 듣기 모드 오버레이 터치 - 녹음 중지 및 전송
        listeningOverlay.setOnClickListener(v -> stopListeningAndSend());
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
        // Wake Word는 MainActivity에서만 제어됨 (자동 재시작 안 함)

        // STT 서비스 리소스 해제
        if (sttService != null) {
            sttService.release();
        }
        if (realtimeSTTService != null) {
            realtimeSTTService.release();
        }
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

        // API 키를 BuildConfig에서 가져오기
        String apiKey = BuildConfig.OPENAI_API_KEY;
        apiKeyManager.saveApiKey(apiKey);
        openAIService.setApiKey(apiKey);

        Log.d(TAG, "OpenAI API 키 설정 완료");
    }

    private void setupSTT() {
        // 배치 방식 STT 서비스
        sttService = new SpeechToTextService(this);

        // 실시간 방식 STT 서비스
        realtimeSTTService = new RealtimeSTTService(this);

        // OpenAI API 키를 BuildConfig에서 가져와 두 서비스 모두에 설정
        String apiKey = BuildConfig.OPENAI_API_KEY;
        sttService.setApiKey(apiKey);
        realtimeSTTService.setApiKey(apiKey);

        // STT 모드 토글 버튼 설정
        setupSTTModeButtons();

        Log.d(TAG, "STT 서비스 초기화 완료 (배치 + 실시간)");
    }

    /**
     * STT 모드 토글 버튼 설정
     */
    private void setupSTTModeButtons() {
        btnSTTRealtime = findViewById(R.id.btn_stt_realtime);
        btnSTTBatch = findViewById(R.id.btn_stt_batch);

        if (btnSTTRealtime == null || btnSTTBatch == null) {
            return;
        }

        // 초기 UI 업데이트
        updateSTTModeUI();

        // 실시간 입력 버튼
        btnSTTRealtime.setOnClickListener(v -> {
            if (currentSTTMode != STTMode.REALTIME) {
                currentSTTMode = STTMode.REALTIME;
                updateSTTModeUI();
                Toast.makeText(this, "실시간 입력 모드로 전환됨", Toast.LENGTH_SHORT).show();
            }
        });

        // 다 듣고 입력 버튼
        btnSTTBatch.setOnClickListener(v -> {
            if (currentSTTMode != STTMode.BATCH) {
                currentSTTMode = STTMode.BATCH;
                updateSTTModeUI();
                Toast.makeText(this, "다 듣고 입력 모드로 전환됨", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * STT 모드 UI 업데이트
     */
    private void updateSTTModeUI() {
        if (btnSTTRealtime == null || btnSTTBatch == null) {
            return;
        }

        if (currentSTTMode == STTMode.REALTIME) {
            // 실시간 입력 활성화
            btnSTTRealtime.setBackgroundColor(0xFF1976D2);
            btnSTTRealtime.setTextColor(0xFFFFFFFF);
            btnSTTBatch.setBackgroundColor(0xFFDDE6F5);
            btnSTTBatch.setTextColor(0xFF4A5A6A);
        } else {
            // 다 듣고 입력 활성화
            btnSTTRealtime.setBackgroundColor(0xFFDDE6F5);
            btnSTTRealtime.setTextColor(0xFF4A5A6A);
            btnSTTBatch.setBackgroundColor(0xFF1976D2);
            btnSTTBatch.setTextColor(0xFFFFFFFF);
        }
    }

    /**
     * 음성 듣기 시작
     */
    private void startListening() {
        // 녹음 권한 확인
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { android.Manifest.permission.RECORD_AUDIO },
                        PERMISSION_REQUEST_RECORD_AUDIO_STT);
                return;
            }
        }

        // 메시지 수신 중이면 무시
        if (isWaitingForResponse) {
            Toast.makeText(this, "메시지를 받는 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 현재 STT 모드에 따라 다른 서비스 사용
        if (currentSTTMode == STTMode.REALTIME) {
            startRealtimeListening();
        } else {
            startBatchListening();
        }
    }

    /**
     * 실시간 입력 모드 시작
     */
    private void startRealtimeListening() {
        Log.d(TAG, "실시간 입력 모드 시작");

        // 텍스트 버퍼 초기화
        realtimeTextBuffer.setLength(0);
        inputMessage.setText("");

        // 듣기 모드 오버레이 표시
        listeningOverlay.setVisibility(View.VISIBLE);
        TextView listeningText = listeningOverlay.findViewById(R.id.listening_text);
        TextView listeningInstruction = listeningOverlay.findViewById(R.id.listening_instruction);
        listeningText.setText("실시간으로 듣고 있습니다");
        listeningInstruction.setText("화면을 터치하여 메시지 보내기");

        // 실시간 STT 시작
        realtimeSTTService.startRealtimeSTT(new RealtimeSTTService.RealtimeCallback() {
            @Override
            public void onConnectionReady() {
                Log.d(TAG, "실시간 STT 연결 준비 완료");
            }

            @Override
            public void onTextDelta(String deltaText) {
                // 실시간으로 입력창에 텍스트 추가
                Log.d(TAG, "실시간 텍스트 델타: " + deltaText);
                realtimeTextBuffer.append(deltaText);
                inputMessage.setText(realtimeTextBuffer.toString());

                // 커서를 끝으로 이동
                inputMessage.setSelection(inputMessage.getText().length());
            }

            @Override
            public void onTextComplete(String fullText) {
                Log.d(TAG, "실시간 텍스트 완성 (한 문장 끝): " + fullText);
                // VAD가 침묵을 감지하여 한 문장이 끝났지만,
                // 계속 말할 수 있으므로 누적된 버퍼는 유지
                // 공백 추가하여 다음 문장과 구분
                if (!fullText.isEmpty() && realtimeTextBuffer.length() > 0) {
                    realtimeTextBuffer.append(" ");
                    inputMessage.setText(realtimeTextBuffer.toString());
                    inputMessage.setSelection(inputMessage.getText().length());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "실시간 STT 오류: " + error);

                // 오버레이 숨기기
                listeningOverlay.setVisibility(View.GONE);

                // 에러 메시지 표시
                Toast.makeText(ChatActivity.this, "음성 인식 오류: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 다 듣고 입력 모드 시작 (기존 배치 방식)
     */
    private void startBatchListening() {
        Log.d(TAG, "다 듣고 입력 모드 시작");

        // 듣기 모드 오버레이 표시
        listeningOverlay.setVisibility(View.VISIBLE);
        TextView listeningText = listeningOverlay.findViewById(R.id.listening_text);
        TextView listeningInstruction = listeningOverlay.findViewById(R.id.listening_instruction);
        listeningText.setText("듣고 있습니다");
        listeningInstruction.setText("화면을 터치하여 메시지 보내기");

        // 배치 방식 녹음 시작
        boolean started = sttService.startRecording();
        if (!started) {
            Toast.makeText(this, "음성 녹음을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show();
            listeningOverlay.setVisibility(View.GONE);
        }
    }

    /**
     * 음성 듣기 중지 및 전송
     */
    private void stopListeningAndSend() {
        if (currentSTTMode == STTMode.REALTIME) {
            stopRealtimeListeningAndSend();
        } else {
            stopBatchListeningAndSend();
        }
    }

    /**
     * 실시간 입력 모드 중지 및 전송
     */
    private void stopRealtimeListeningAndSend() {
        Log.d(TAG, "실시간 입력 중지 및 전송");

        // 실시간 STT 중지
        realtimeSTTService.stopRealtimeSTT();

        // 오버레이 숨기기
        listeningOverlay.setVisibility(View.GONE);

        // 입력창의 텍스트 확인
        String text = inputMessage.getText() != null ? inputMessage.getText().toString().trim() : "";

        if (!text.isEmpty()) {
            // 자동으로 전송
            sendUserMessage();
        } else {
            Toast.makeText(this, "인식된 음성이 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 다 듣고 입력 모드 중지 및 전송 (기존 배치 방식)
     */
    private void stopBatchListeningAndSend() {
        if (!sttService.isRecording()) {
            listeningOverlay.setVisibility(View.GONE);
            return;
        }

        // 로딩 표시를 위해 오버레이 텍스트 변경
        TextView listeningText = listeningOverlay.findViewById(R.id.listening_text);
        TextView listeningInstruction = listeningOverlay.findViewById(R.id.listening_instruction);
        listeningText.setText("변환 중...");
        listeningInstruction.setText("잠시만 기다려주세요");

        Log.d(TAG, "다 듣고 입력 모드 중지 및 변환 시작");

        // 녹음 중지 및 STT 요청
        sttService.stopRecordingAndTranscribe(new SpeechToTextService.TranscriptionCallback() {
            @Override
            public void onTranscriptionComplete(String transcribedText) {
                Log.d(TAG, "음성 변환 완료: " + transcribedText);

                // 오버레이 숨기기
                listeningOverlay.setVisibility(View.GONE);
                listeningText.setText("듣고 있습니다");
                listeningInstruction.setText("화면을 터치하여 메시지 보내기");

                // 입력창에 텍스트 설정
                inputMessage.setText(transcribedText);

                // 자동으로 전송
                sendUserMessage();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "음성 변환 실패: " + error);

                // 오버레이 숨기기
                listeningOverlay.setVisibility(View.GONE);
                listeningText.setText("듣고 있습니다");
                listeningInstruction.setText("화면을 터치하여 메시지 보내기");

                // 에러 메시지 표시
                Toast.makeText(ChatActivity.this, "음성 인식 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO_STT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 승인됨 - 녹음 시작
                startListening();
            } else {
                // 권한 거부됨
                Toast.makeText(this, "음성 인식을 사용하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
