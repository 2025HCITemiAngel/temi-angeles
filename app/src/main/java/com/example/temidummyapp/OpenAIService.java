package com.example.temidummyapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OpenAI API 통신 서비스
 * RAG 구조를 위한 행사장 정보를 시스템 프롬프트에 포함
 */
public class OpenAIService {
    private static final String TAG = "OpenAIService";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    private String apiKey;

    // RAG: 행사장 정보 시스템 프롬프트
    private static final String SYSTEM_PROMPT = "당신은 2025 CO-SHOW 행사 안내 도우미입니다. 방문객에게 친절하고 정확하게 정보를 제공해야 합니다.\n\n" +
            "=== 행사 정보 ===\n" +
            "행사명: 2025 CO-SHOW (코쇼)\n" +
            "일정: 2025년 11월 26일(수) ~ 11월 29일(토), 4일간\n" +
            "장소: 부산 BEXCO 제1전시장 2홀, 3A홀\n" +
            "대상: 초·중·고등학생, 대학생, 전 국민 누구나\n" +
            "입장료: 무료\n\n" +
            "=== 안내 규칙 ===\n" +
            "- 아래 표와 제공된 행사 정보만 사용해 안내합니다.\n" +
            "- 사용자가 처음 질문하면 바로 프로그램을 나열하지 않고,\n" +
            "  어떤 활동을 원하는지·누구와 왔는지·흥미 있는 분야 등을 1~2가지 자연스럽게 확인합니다.\n" +
            "- 대화를 2~3턴 진행하며 연령(stage), 관심 분야(tags), 체험 시간(time_max), 희망 분위기 등을 파악합니다.\n" +
            "- 조건에 맞는 프로그램을 최대 3개 추천하고 짧은 이유를 함께 안내합니다.\n" +
            "- 추측하거나 만들어내지 않고, 정보가 없으면 모른다고 답합니다.\n" +
            "- 사용자가 한국어로 질문하면 한국어로, 영어로 질문하면 영어로 답변합니다.\n" +
            "- 이벤트 관련 문의가 들어오면 제공된 이벤트 정보를 기반으로 정확하게 안내합니다.\n" +
            "- 항상 친절하고 부드럽고 간결하게 응답합니다.\n\n" +
            "=== 프로그램 데이터 형식 ===\n" +
            "booth_no\tstage\ttags\ttitle\tintroduction\ttime_max\tmethod\n\n" +
            "=== 프로그램 목록 ===\n" +
            "B04\t중학생 이상\t인공지능\tAICOSS 캠퍼스 탐구생활\tAI 전공-특화 교과목(20-20)을 메타버스 캠퍼스에서 페험하고 숨겨진 미션을 해결하고 보상받자!\t10분\t현장접수\n" +
            "B04\t초등학생 고학년 이상\t인공지능\t3분 아티스트 - 나만의 AI 아트 팝업 갤러리\t카메라에 찍힌 내 얼굴이 단 3분만에 영화 스타일의 미디어 아트로 변신!\t10분\t현장접수\n"
            +
            "B04\t초등학생 고학년 이상\t인공지능\t우리동네 AI 안전교실 체험\t아이들의 행동을 AI가 즉시 분석하고 안전리포트를 만들어주는 실시간 체험\t10분\t현장접수\n" +
            "B07\t고등학생 이상\t데이터보안 활용융합\tDrone Under Attack: 하늘 위의 드론 위협\t하늘 위에서 벌어지는 사이버 위협의 실체를 직접 체험해보세요!\t15분\t현장접수 / 사전접수\n"
            +
            "B07\t중학생 이상\t데이터보안 활용융합\t우리의 친절한 로봇의 반란\t해커의 손에 넘어가 멈추고 조작되는 순간, 로봇 해킹의 실체를 체험!\t15분\t현장접수 / 사전접수\n" +
            "B07\t중학생 이상\t데이터보안 활용융합\t탈출하라, 사이버 보안 위기속 탈출기\t주변 단서를 조합해 사이버 위기 속 위협을 탈출하는 방탈출 게임!\t30분\t현장접수 / 사전접수\n" +
            "B07\t중학생 이상\t데이터보안 활용융합\t해킹이 부른 의료 사고\t실제 의료기기 해킹 시연으로 환자 생명에 미치는 치명적 위험을 확인!\t10분\t현장접수 / 사전접수\n" +
            "B07\t초등학생 이상\t데이터보안 활용융합\tAI가 만든 가짜를 잡아라\t숨은 단서를 찾아 AI가 만든 가짜와 사람의 진짜를 구분해보세요.\t10분\t현장접수 / 사전접수\n" +
            "B07\t초등학생 이상\t데이터보안 활용융합\t스마트홈 캠 해킹, 누군가 지켜보고 있다.\t스마트홈 해킹 시연을 통해 사생활 침해의 실체를 확인하세요.\t10분\t현장접수 / 사전접수\n" +
            "A04\t고등학생 이상\t차세대 디스플레이\t차세대디스플레이 공정 XR 체험\t디스플레이 6.5G 공정 프로세스(증착기 내부, 실제 크기 등)을 체험\t10분\t현장접수\n" +
            "A04\t초등학생 이상\t차세대 디스플레이\tT-OLED 액자\t투명 OLED를 사용하여 원하는 이미지를 액자로 찰칵!\t5분\t현장접수\n" +
            "A04\t초등학생 이상\t차세대 디스플레이\t키네틱LED Interactive\t실시간 동작 감지로 키네틱LED를 체험\t10분\t현장접수\n" +
            "B02\t누구나\t반도체 소부장\t반도체 장비를 분해해보자\t반도체를 만드는 장비 내부를 보고, 뜯고, 조립해보는 VR 체험\t15~30분\t현장접수 / 사전접수\n" +
            "B09\t누구나\t에코업\t세종기후환경 네트워크 체험1 · 대기전력과 에너지소비효율등급\t가전제품 대기전력과 에너지소비효율등급 알아보기\t10분\t현장접수 / 사전접수\n" +
            "B09\t초등학생 고학년 이상\t에코업\t에코업 6대 과제 탈출놀이\t에코업 6대 과제를 통해 탈출하는 프로그램\t15분\t현장접수 / 사전접수\n" +
            "B09\t초등학생 고학년 이상\t에코업\t세종기후환경네트워크 체험2 LED로 그리는 탄소 중립\t탄소중립 조명 만들기(아크릴 LED등 만들기)\t20분\t현장접수 / 사전접수\n" +
            "B01\t누구나\t사물 인터넷\t센서가 듣고, AI가 생각한다\t센서가 내 목소리와 심장 소리를 읽고, AI가 판단하면 로봇이 곧바로 움직인다. 센서와 AI가 만드는 연결된 세상의 실험실.\t5분\t현장접수\n"
            +
            "B01\t중학생 이상\t사물 인터넷\t스마트음식 디지털 트윈 체험\t모바일 로봇과 수질센서를 활용한 자율 환경 관리 체험\t30분\t현장접수\n" +
            "B01\t초등학생 고학년 이상\t사물 인터넷\tAI IoT 스마트 조명\t경량 AI와 IoT로 만드는 나만의 스마트 조명\t10분\t현장접수\n" +
            "A06\t누구나\t이차전지\t이차전지 제조공정 VR체험\tHMD를 착용하고, 이차전지 제조과정에 대한 VR영상을 통한 체험\t30분\t사전접수\n" +
            "A06\t중학생 이상\t이차전지\t보조배터리 제작 체험 (보조배터리 만들어 봤어? 안 만들어 봤으면 말을 하지마!)\t보조배터리 툴킷(Toolkit)을 활용한 제작 체험\t1시간\t사전접수\n"
            +
            "B06\t누구나\t에너지 신산업\t플러스에너지 빌딩\t건물 자체에서 소비하는 에너지보다 더 많은 에너지를 생산하여 순에너지 이익을 창출하는 차세대 친환경 건물을 직접 체험\t10분\t현장접수\n"
            +
            "B06\t중학생 이상\t에너지 신산업\t수소자동차 롱롱\t수소자동차의 원리를 이해하고 직접 만들어보는 체험형 프로그램\t40분\t사전접수\n" +
            "B06\t중학생 이상\t에너지 신산업\t수소연료전지 자동차 만들기\t수소연료전자의 원리를 이해하고 친환경 자동차를 직접 만들어보는 체험형 교육 프로그램\t40분\t사전접수\n" +
            "B06\t중학생 이상\t에너지 신산업\t작은발전기\t친환경에너지를 이해하고 내손으로 만들어보는 발전기\t40분\t사전접수\n" +
            "B06\t중학생 이상\t에너지 신산업\t태양광 강아지 로봇 만들기\t태양광의 원리를 배우고 직접 전지판 실험 및 강아지로봇 제작을 통해 친환경 에너지를 체험\t60분\t현장접수 / 사전접수\n"
            +
            "B05\t초등학생 이상\t지능형 로봇\tAI 드론 및 로봇·오목 로봇 체험\t로봇과의 오목두기·로봇이 그려주는 캐리커쳐 체험을 선택하여 진행\t10분\t현장접수\n" +
            "B05\t초등학생 이상\t지능형 로봇\tROBO SHOW\t4족 보행로봇·모바일로봇 등을 활용한 로봇쇼\t상시\t현장접수\n" +
            "B05\t초등학생 이상\t지능형 로봇\t경주로봇 만들기\t경주로봇을 만들어 트랙에서 다른 사람과 경기를 해본다\t60분\t현장접수\n" +
            "B05\t초등학생 고학년 이상\t지능형 로봇\t청소로봇 만들기\t로봇청소기의 원리를 배울 수 있는 자율주행청소로봇 만들기!\t60분\t사전접수\n" +
            "B05\t초등학생 고학년 이상\t지능형 로봇\t자이로 외발주행로봇 만들기\t신기방기한 자이로 레일카의 원리에 빠져보아요\t60분\t사전접수\n" +
            "B05\t초등학생 고학년 이상\t지능형 로봇\t유선 스파이더로봇 만들기\t유선으로 조종이 가능한 다족로봇을 만들어 보아요\t60분\t사전접수\n" +
            "B05\t초등학생 고학년 이상\t지능형 로봇\t로봇아 멍멍해봐\t4족보행로봇의 원리를 학습하고 트랙 운행을 통해 로봇의 움직임을 직접 체험하는 프로그램\t60분\t현장접수\n" +
            "B05\t중학생 이상\t지능형 로봇\t휴머노이드 이론교육 및 미션수행\t4족보행로봇의 원리를 학습하고 트랙 운행을 통해 로봇의 움직임을 직접 체험하는 프로그램\t90분\t사전접수\n" +
            "A07\t초등학생 고학년 이상\t실감 미디어\t꿈의 학교 속으로! 메타버스 캠퍼스 & 콘텐츠 여행\t실감 미디어 메타버스 플랫폼을 활용한 대학 캠퍼스 투어 및 우수작품 쇼케이스\t10분\t현장접수\n"
            +
            "A07\t초등학생 고학년 이상\t실감 미디어\t“디지털숲 영상 오디세이”\t가상현실로 구현된 숲 숲속 기상 변화 등을 배경으로 시각과 청각을 활용해 정신과 신체의 이완을 돕는 명상 프로그램\t30분\t사전접수\n"
            +
            "A07\t초등학생 고학년 이상\t실감 미디어\t“진짜처럼 짜릿! 햅틱 손맛 체험존”(VR 기반의 햅틱 디바이스 체험)\t햅틱 디바이스(슈트와 장갑)를 착용한 후 복싱과 롤러코스터 체험\t10분\t현장접수\n"
            +
            "A07\t초등학생 고학년 이상\t실감 미디어\t톡톡 튀는 콘텐츠! 실감 팡팡 체험존\t실감미디어 학생들의 독창성 있는 우수 작품들을 직접 체험\t10분\t현장접수\n" +
            "B08\t초등학생 고학년 이상\t미래 자동차\tAWS DeepRacer 자율주행 체험\t자율주행 AI VS 인간 운전 대결 승자는?!\t10분\t현장접수 / 사전접수\n" +
            "B11\t초등학생 고학년 이상\t항공드론\tUAM 조종 시뮬레이터 체험\t블록코딩을 활용한 드론 실습 및 체험교육\t30분 이내\t현장접수 / 사전접수\n" +
            "B11\t초등학생 고학년 이상\t항공드론\t드론 장애물 경주 체험\t드론을 수동으로 조종하여 다양한 미션을 수행하는 체험 프로그램\t30분 이내\t현장접수\n" +
            "B11\t초등학생 고학년 이상\t항공드론\t블록코딩을 활용한 드론제어\t블록코딩을 활용한 드론 실습 및 체험교육\t90분 이내\t현장접수 / 사전접수\n" +
            "A02\t초등학생 고학년 이상\t차세대 반도체\t어서와, 반도체 회로제작은 처음이지?\t내 손으로 만들어보는 반도체 PCB자 미니 게임기\t30분\t현장접수\n" +
            "B10\t초등학생 고학년 이상\t첨단소재 나노융합\t첨단소재워터랩: 모래의 비밀\t모래가 씨앗의 수분 공급과 싹틔움에 미치는 원리를 통해 첨단소재의 개념 체험\t40분\t사전접수\n" +
            "B10\t초등학생 이상\t첨단소재 나노융합\t3D펜으로 만드는 첨단소재 창의공작소\t3D펜과 첨단소재로 상상을 입체로 구현하는 창의 체험 프로그램\t30분\t현장접수\n" +
            "B10\t초등학생 이상\t첨단소재 나노융합\t손끝에서 펼쳐지는 스트레처블 첨단연구소재\t쭈욱 늘어나는 스트레처블(유연 고분자) 소재 과학 체험\t40분\t사전접수\n" +
            "A01\t초등학생 이상\t그린 바이오\tSpace Green 우주 농부 인증 미션을 위한 탑승을 시작합니다!\t애그테크(Ag-Tech) 접목 우주 농사 체험! 상추 생장 환경을 제어해 우주 농부 인증 받기\t25분\t현장접수\n"
            +
            "A05\t초등학생 이상\t바이오 헬스\t기술로 되찾는 움직임, 로봇이 전하는 회복의 미래\t첨단 로봇 재활 기기가 만들어가는 새로운 걸음과 삶의 가능성\t10분\t현장접수 / 사전접수\n" +
            "A05\t초등학생 이상\t바이오 헬스\t생명의 코드, 우리 몸과 DNA\t코끼리 치약부터 DNA추출까지 실험으로 배우는 유전자 과학\t60분\t사전접수\n" +
            "A05\t초등학생 이상\t바이오 헬스\t이동의 경계를 넘어, 게임으로 확장된 세상\t휠체어와 게임이 만드는 새로운 경험과 가능성\t4분\t현장접수 / 사전접수\n" +
            "A05\t초등학생 이상\t바이오 헬스\t피부 위의 예술, 인체 속의 과학\t바디페인팅으로 경험하는 인체과학과 예술\t15분\t현장접수 / 사전접수\n" +
            "B06\t초등학생 이상\t에너지 신산업\t수소밸류체인 VR 체험 프로그램\tVR을 통해 수소 생산부터 활용까지 수소벨류체인의 전 과정을 생생하게 체험\t10분\t사전접수\n" +
            "B06\t초등학생 이상\t에너지 신산업\t업사이클링 카드지갑 만들기\t친환경 소재로 만들어보는 나만의 카드지갑\t30분\t사전접수\n" +
            "B03\t초등학생 이상\t차세대 통신\t인공위성 통신 체험관\t인공위성 발사부터 통신까지! 위성 통신 체험하기!\t15분\t현장접수 / 사전접수\n" +
            "A03\t초등학생 이상\t빅데이터\tScent Memory\t나의 취향을 입력하면 데이터가 향수 레시피를 추천해준다구?!\t5분\t현장접수\n" +
            "A03\t초등학생 이상\t빅데이터\t기택이의 대모험\t오류 속에 숨은 진실 기택이와 데이터 디버깅 체험\t5~10분\t현장접수\n" +
            "A03\t초등학생 이상\t빅데이터\t나는 누구일까?\t빅데이터가 랜덤키워드 5개를 줄게 정답은 누가 맞출래?\t5~10분\t현장접수\n" +
            "A03\t초등학생 이상\t빅데이터\t나의 왕자님, 공주님을 찾아라!\t이상형과 관련된 단어만 알려줘! AI가 이상형을 그려줄게!\t5~10분\t현장접수\n" +
            "A03\t초등학생 이상\t빅데이터\t너 내 동료가 되라!\t데이터분석? 시각화? 빅데이터 전공이 궁금하면 따라와! 전공체험 해보자\t5분\t현장접수\n" +
            "A03\t초등학생 이상\t빅데이터\t빅데이터 어워즈\t챗봇과 함께하는 빅데이터 컨소시엄 학생들이 만든 우수 성과 체험\t5분\t현장접수\n\n\n" +
            "=== CO-SHOW 이벤트 안내 ===\n\n" +

            "=== 1) 수험생 특별 이벤트 ===\n" +
            "이벤트명: 2025 CO-SHOW 수험생 이벤트\n" +
            "대상: 수험표 지참 수험생\n" +
            "혜택: 도장 2개 즉시 지급 및 모든 도장 획득시 경품 뽑기 진행\n" +
            "조건: CO-SHOW 방문 당일 실물 수험표 소지 필수, 모든 도장 획득 필요\n\n" +

            "운영 기간: 2025년 11월 26일(수) ~ 11월 29일(토) 상시 운영\n" +
            "참여 위치: 전시장 내 이벤트 운영부스 (등록데스크 리플렛 수령 후 진행)\n\n" +

            "참여 방법:\n" +
            "1) 등록데스크에서 CO-SHOW 리플렛 수령\n" +
            "2) 이벤트 운영부스에서 수험표 인증 및 도장 2개 지급\n" +
            "3) 체험·교육 프로그램 참여 후 도장 추가 획득\n" +
            "4) 도장 완성 후 스탬프 용지 제출\n" +
            "5) 경품 뽑기 진행 (랜덤)\n" +
            "※ 1인 1회 참여 가능\n\n" +

            "스페셜 경품:\n" +
            "- 갤럭시 워치 8\n" +
            "- 갤럭시 버즈 3 프로\n" +
            "- 키크론 V10 Pro Max 키보드\n" +
            "- 키크론 B6 Pro 저소음 블루투스 키보드\n" +
            "- COSS-BALL 키링\n\n" +

            "유의 사항:\n" +
            "- 수험표는 반드시 실물 지참\n" +
            "- 현장 방문 인증 필수\n" +
            "- 도장은 행사 기간 내 상시 운영\n\n" +

            "홍보 문구 예시:\n" +
            "수험표 들고 CO-SHOW로 출발!\n" +
            "수능 끝! 이제 SHOW 보러 가자!\n" +
            "스탬프투어 참여하고 선물까지 GET!\n\n" +

            "=== 2) CO-SHOW 스탬프투어 이벤트 ===\n" +
            "이벤트명: 2025 CO-SHOW 스탬프투어 이벤트\n" +
            "설명: 전시장 체험 프로그램 참여 후 스탬프를 8개 이상 모아 경품에 참여하는 프로그램\n\n" +

            "운영 기간: 2025년 11월 26일(수) ~ 11월 29일(토) 상시 운영\n" +
            "참여 제한: 1일 선착순 1,000명 / 1인 1회 참여 가능\n\n" +

            "참여 위치:\n" +
            "- 전시장 내 스탬프 투어 이벤트 부스\n" +
            "- 부산 BEXCO 메인무대 앞\n\n" +

            "참여 방법:\n" +
            "1) 등록데스크에서 리플렛 수령\n" +
            "2) 원하는 프로그램 참여 후 스탬프 획득\n" +
            "3) 스탬프 8개 이상 모으면 이벤트 부스 방문\n" +
            "4) 리플렛 제출 후 경품 뽑기 진행\n\n" +

            "수험생 추가 혜택:\n" +
            "- 수험표 인증 시 스탬프 2개 즉시 지급\n" +
            "- 현장 운영부스 방문 필수\n\n" +

            "경품 라인업:\n" +
            "- 갤럭시 워치 8\n" +
            "- 갤럭시 버즈 프로 3\n" +
            "- 애플워치 SE 3\n" +
            "- 에어팟 프로 3\n" +
            "- 키크론 V10 MAX\n" +
            "- 키크론 B6\n" +
            "- COSS-BALL 키링\n\n" +

            "홍보 문구 예시:\n" +
            "CO-SHOW 즐기고 스탬프 찍고 선물까지 받자!\n" +
            "스탬프 8개 모으면 경품 쏟아진다!\n" +
            "놓치면 아쉬운 선착순 1,000명 이벤트!\n\n" +

            "관련 해시태그:\n" +
            "#코쇼 #2025코쇼 #COSHOW #2025COSHOW #COSS사업 #2025COSS\n" +
            "#첨단교육 #스탬프투어 #수험생이벤트 #부산 #BEXCO\n";

    public interface ChatCallback {
        void onSuccess(String response);

        void onError(String error);
    }

    public interface StreamCallback {
        void onStream(String chunk);

        void onComplete();

        void onError(String error);
    }

    public OpenAIService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * API 키 설정
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * API 키가 설정되어 있는지 확인
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * 채팅 완료 요청 (스트리밍 방식)
     * 
     * @param messages 대화 기록
     * @param callback 스트림 콜백
     */
    public void sendMessageStreaming(List<ChatMessage> messages, StreamCallback callback) {
        if (!hasApiKey()) {
            callback.onError("API 키가 설정되지 않았습니다.");
            return;
        }

        new Thread(() -> {
            try {
                JsonObject requestBody = buildRequestBody(messages, true);
                RequestBody body = RequestBody.create(requestBody.toString(), JSON);

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.e(TAG, "API 오류 응답: " + errorBody);
                    mainHandler.post(() -> callback.onError("API 오류 (코드: " + response.code() + ")"));
                    return;
                }

                // SSE 스트림 읽기
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body().byteStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);

                        // 스트림 종료 신호
                        if (data.equals("[DONE]")) {
                            mainHandler.post(callback::onComplete);
                            break;
                        }

                        try {
                            // 청크 파싱
                            String chunk = parseStreamChunk(data);
                            if (chunk != null && !chunk.isEmpty()) {
                                mainHandler.post(() -> callback.onStream(chunk));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "청크 파싱 오류: " + e.getMessage());
                        }
                    }
                }

                reader.close();

            } catch (Exception e) {
                Log.e(TAG, "스트리밍 오류", e);
                mainHandler.post(() -> callback.onError("스트리밍 오류: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 채팅 완료 요청 (일반 방식 - 호환성 유지)
     * 
     * @param messages 대화 기록
     * @param callback 응답 콜백
     */
    public void sendMessage(List<ChatMessage> messages, ChatCallback callback) {
        if (!hasApiKey()) {
            callback.onError("API 키가 설정되지 않았습니다.");
            return;
        }

        try {
            JsonObject requestBody = buildRequestBody(messages, false);
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API 호출 실패", e);
                    mainHandler.post(() -> callback.onError("네트워크 오류: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String responseBody = response.body().string();

                        if (!response.isSuccessful()) {
                            Log.e(TAG, "API 오류 응답: " + responseBody);
                            mainHandler.post(() -> callback.onError("API 오류 (코드: " + response.code() + ")"));
                            return;
                        }

                        String botMessage = parseResponse(responseBody);
                        mainHandler.post(() -> callback.onSuccess(botMessage));

                    } catch (Exception e) {
                        Log.e(TAG, "응답 파싱 오류", e);
                        mainHandler.post(() -> callback.onError("응답 처리 오류: " + e.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "요청 생성 오류", e);
            callback.onError("요청 생성 오류: " + e.getMessage());
        }
    }

    /**
     * OpenAI API 요청 본문 생성
     */
    private JsonObject buildRequestBody(List<ChatMessage> messages, boolean stream) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini"); // 비용 효율적인 모델
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 500);
        requestBody.addProperty("stream", stream); // 스트리밍 여부

        JsonArray messagesArray = new JsonArray();

        // 시스템 프롬프트 추가 (RAG)
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", SYSTEM_PROMPT);
        messagesArray.add(systemMessage);

        // 대화 기록 추가
        for (ChatMessage message : messages) {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", message.isUser() ? "user" : "assistant");
            msgObj.addProperty("content", message.getMessage());
            messagesArray.add(msgObj);
        }

        requestBody.add("messages", messagesArray);
        return requestBody;
    }

    /**
     * 스트림 청크 파싱
     */
    private String parseStreamChunk(String chunkData) {
        try {
            JsonObject json = gson.fromJson(chunkData, JsonObject.class);

            if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();

                if (choice.has("delta")) {
                    JsonObject delta = choice.getAsJsonObject("delta");

                    if (delta.has("content")) {
                        return delta.get("content").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "청크 파싱 실패: " + e.getMessage());
        }

        return null;
    }

    /**
     * OpenAI API 응답 파싱
     */
    private String parseResponse(String responseBody) throws Exception {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);

        if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
            JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");
            return message.get("content").getAsString();
        }

        throw new Exception("응답에서 메시지를 찾을 수 없습니다.");
    }

    /**
     * 시스템 프롬프트 가져오기 (테스트/디버깅용)
     */
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}
