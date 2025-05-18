package vn.iostar.doan.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import vn.iostar.doan.R;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.model.User1;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etUsername, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ImageView googleSignIn;

    private boolean isRegisterCooldown = false; // Biến cờ cooldown

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ ID từ layout
        etFullName = findViewById(R.id.etFullname);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        googleSignIn = findViewById(R.id.googleSignIn);

        // Xử lý sự kiện khi nhấn nút Đăng ký
        btnRegister.setOnClickListener(v -> {
            if (isRegisterCooldown) {
                Toast.makeText(RegisterActivity.this, "Vui lòng chờ 1 phút trước khi gửi lại!", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullName = etFullName.getText().toString().trim();
            String email = etUsername.getText().toString().trim();  // Email hoặc username
            String password = etPassword.getText().toString().trim();

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            } else {
                User1 user = new User1();
                user.setFullName(fullName);
                user.setEmail(email);
                user.setPassword(password);

                ApiService.apiService.registerUser(user).enqueue(new retrofit2.Callback<User1>() {
                    @Override
                    public void onResponse(Call<User1> call, retrofit2.Response<User1> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng xác thực OTP.", Toast.LENGTH_SHORT).show();

                            // Bắt đầu cooldown 1 phút
                            startRegisterCooldown();

                            // Gửi email qua VerifyOtpRegisterActivity
                            Intent intent = new Intent(RegisterActivity.this, VerifyOtpRegisterActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Đăng ký thất bại!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<User1> call, Throwable t) {
                        Toast.makeText(RegisterActivity.this, "Lỗi kết nối server!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Xử lý sự kiện đăng nhập bằng Google
        googleSignIn.setOnClickListener(v -> {
            Toast.makeText(RegisterActivity.this, "Tính năng đăng ký với Google chưa được triển khai!", Toast.LENGTH_SHORT).show();
        });
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);

            startActivity(intent);
            finish();
        });

    }

    private void startRegisterCooldown() {
        isRegisterCooldown = true;
        btnRegister.setEnabled(false);

        // Sử dụng CountDownTimer để chờ 60 giây
        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                btnRegister.setText("Chờ " + millisUntilFinished / 1000 + "s");
            }

            public void onFinish() {
                isRegisterCooldown = false;
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký");
            }
        }.start();
    }
}
