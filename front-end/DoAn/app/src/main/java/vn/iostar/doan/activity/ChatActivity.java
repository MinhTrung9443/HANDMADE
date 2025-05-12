package vn.iostar.doan.activity; // Đảm bảo package này đúng

import android.content.Intent; // *** Thêm import Intent ***
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity; // Kế thừa từ AppCompatActivity
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // *** Thêm import Objects (cho null safety) ***

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import vn.iostar.doan.R; // Đảm bảo package R đúng
import vn.iostar.doan.adapter.ChatAdapter; // Đảm bảo package Adapter đúng
import vn.iostar.doan.model.Message; // Đảm bảo package Model đúng
import vn.iostar.doan.modelRequest.ChatRequest; // Đảm bảo package ModelRequest đúng
import vn.iostar.doan.modelResponse.ChatResponse; // Đảm bảo package ModelResponse đúng

// *** Quan trọng: Import đúng lớp HomeActivity của bạn ***
import vn.iostar.doan.activity.HomeActivity; // Ví dụ, thay thế nếu tên/package khác

public class ChatActivity extends AppCompatActivity { // Phải kế thừa từ AppCompatActivity

    private static final String TAG = "ChatActivity";
    // ***** Đảm bảo URL này đúng với địa chỉ IP và cổng backend của bạn *****
    private static final String API_BASE_URL = "http://10.0.2.2:8080/";
    private static final String CHAT_ENDPOINT = "api/chat";

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private Toolbar toolbarChat; // Biến cho Toolbar

    private ChatAdapter chatAdapter;
    private List<Message> messageList;

    private OkHttpClient okHttpClient;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // Đảm bảo layout này đúng

        // --- Khởi tạo Views ---
        toolbarChat = findViewById(R.id.toolbar_chat); // Tìm Toolbar
        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);

        // --- Thiết lập Toolbar làm ActionBar ---
        setSupportActionBar(toolbarChat); // Đặt Toolbar làm ActionBar chính
        // Sử dụng Objects.requireNonNull để code an toàn hơn
        Objects.requireNonNull(getSupportActionBar()).setTitle("MANJHA Bot"); // Đặt tiêu đề mong muốn
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút back (<-) dựa trên navigationIcon trong XML/theme
        getSupportActionBar().setDisplayShowHomeEnabled(true); // Đảm bảo nút back được hiển thị
        // --------------------------------------


        // --- Khởi tạo OkHttp và Gson ---
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();
        // --------------------------------


        // --- Khởi tạo RecyclerView ---
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(chatAdapter);
        // -----------------------------


        // Thêm tin nhắn chào mừng ban đầu
        addBotMessage("Xin chào! Mình là MANJHA bot. Bạn cần hỗ trợ gì ạ?");

        // Xử lý sự kiện nút gửi
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    // --- Xử lý sự kiện click nút Back trên Toolbar (Navigation Icon) ---
    @Override
    public boolean onSupportNavigateUp() {
        // Tạo Intent để chuyển đến HomeActivity
        // *** Thay thế HomeActivity.class bằng tên lớp Activity chính xác của bạn ***
        Intent intent = new Intent(ChatActivity.this, HomeActivity.class);

        // (Quan trọng) Thêm flags để xử lý back stack đúng cách:
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Khởi chạy HomeActivity
        startActivity(intent);

        // Kết thúc ChatActivity hiện tại sau khi đã điều hướng đi
        finish();

        return true; // Báo rằng chúng ta đã xử lý sự kiện này
    }
    // ------------------------------------------------------------------

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            addUserMessage(messageText);
            editTextMessage.setText("");

            ChatRequest requestPayload = new ChatRequest(messageText);
            callChatApiDirectly(requestPayload);

        } else {
            Toast.makeText(this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Hàm gọi API dùng OkHttp trực tiếp ---
    private void callChatApiDirectly(ChatRequest chatRequest) {
        // Hiển thị loading và lưu vị trí
        final int loadingMessagePosition = addBotMessage("Bot đang soạn tin...");

        String jsonRequestBody = gson.toJson(chatRequest);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonRequestBody, JSON);

        Request request = new Request.Builder()
                .url(API_BASE_URL + CHAT_ENDPOINT)
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "OkHttp call failed", e);
                runOnUiThread(() -> {
                    removeLoadingMessage(loadingMessagePosition); // Xóa loading
                    addBotMessage("Lỗi kết nối: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // Luôn chạy việc xóa loading trên UI thread trước
                runOnUiThread(() -> removeLoadingMessage(loadingMessagePosition));

                final String responseBodyString;
                final int responseCode = response.code(); // Lưu response code

                try {
                    // Đọc body trong background thread
                    responseBodyString = response.body() != null ? response.body().string() : null;
                } catch (IOException e) {
                    Log.e(TAG, "Error reading response body", e);
                    runOnUiThread(() -> addBotMessage("Lỗi đọc phản hồi từ bot."));
                    // Đảm bảo đóng body ngay cả khi có lỗi đọc
                    if (response.body() != null) {
                        try { response.body().close(); } catch (Exception ignored) {}
                    }
                    return; // Thoát sớm
                } finally {
                    // Đảm bảo response body luôn được đóng
                    if (response.body() != null) {
                        try { response.body().close(); } catch (Exception ignored) {}
                    }
                }


                if (response.isSuccessful() && responseBodyString != null) {
                    try {
                        final ChatResponse chatResponse = gson.fromJson(responseBodyString, ChatResponse.class);
                        runOnUiThread(() -> {
                            if (chatResponse != null && chatResponse.getBotReply() != null && !chatResponse.getBotReply().isEmpty()) {
                                addBotMessage(chatResponse.getBotReply());
                            } else {
                                Log.w(TAG, "Bot reply is null or empty in response");
                                addBotMessage("Bot không có phản hồi hợp lệ.");
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                        runOnUiThread(() -> addBotMessage("Lỗi đọc phản hồi từ bot."));
                    }
                } else {
                    Log.e(TAG, "OkHttp call unsuccessful: " + responseCode + " - " + response.message());
                    Log.e(TAG, "Response body (error): " + responseBodyString);
                    runOnUiThread(() -> addBotMessage("Lỗi từ server: " + responseCode));
                }
            }
        });
    }
    // ------------------------------------------

    // Hàm tiện ích để xóa tin nhắn loading
    private void removeLoadingMessage(int position) {
        if (position >= 0 && position < messageList.size()) {
            // Chỉ xóa nếu đó đúng là tin nhắn loading
            if ("Bot đang soạn tin...".equals(messageList.get(position).getText())) {
                messageList.remove(position);
                chatAdapter.notifyItemRemoved(position);
                // Cập nhật lại range nếu cần (an toàn hơn)
                chatAdapter.notifyItemRangeChanged(position, messageList.size());
            }
        }
    }

    // Hàm thêm tin nhắn người dùng
    private void addUserMessage(String text) {
        Message userMessage = new Message(text, Message.Sender.USER);
        messageList.add(userMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    // Hàm thêm tin nhắn bot và trả về vị trí đã thêm
    private int addBotMessage(String text) {
        Message botMessage = new Message(text, Message.Sender.BOT);
        messageList.add(botMessage);
        int position = messageList.size() - 1;
        chatAdapter.notifyItemInserted(position);
        scrollToBottom();
        return position; // Trả về vị trí để có thể xóa/cập nhật tin nhắn loading
    }

    // Hàm cuộn xuống tin nhắn cuối cùng
    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            recyclerViewMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }
}