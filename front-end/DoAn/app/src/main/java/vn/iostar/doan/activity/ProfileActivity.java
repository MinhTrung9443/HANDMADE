package vn.iostar.doan.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// --- THÊM CÁC IMPORT CHO ACTIVITY RESULT API ---
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
// --- HẾT PHẦN IMPORT MỚI ---

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.model.Address;
import vn.iostar.doan.model.User;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView emailTextView, fullNameTextView, phoneTextView, addressTextView;
    private ImageView avatarImageView;
    private Button orderHistoryButton;
    private Button editProfileButton, shippingAddressButton;

    private String token;
    private User currentUser;
    private Toolbar toolbar;
    private ActivityResultLauncher<Intent> editProfileLauncher;
    private ActivityResultLauncher<Intent> shippingAddressLauncher;
    private ActivityResultLauncher<Intent> orderHistoryLauncher; // Launcher cho Order History
    // --- HẾT KHAI BÁO LAUNCHER ---

    private boolean userInfoHasChanged = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        token = getIntent().getStringExtra("token");
        Log.d(TAG, "Token received: " + token);

        // Kiểm tra token trước khi làm gì khác
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token is null or empty. Cannot proceed.");
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            // Có thể chuyển về màn hình Login ở đây
            // Intent loginIntent = new Intent(this, LoginActivity.class);
            // startActivity(loginIntent);
            finish(); // Đóng activity profile nếu không có token
            return;
        }

        AnhXa();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "EditProfileLauncher callback received. ResultCode: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Log.i(TAG, "EditProfileActivity finished with RESULT_OK. Refreshing user info.");
                        if (token != null && !token.isEmpty()) {
                            getUserInfo(token);
                            userInfoHasChanged = true;
                        } else {
                            Log.e(TAG, "Token is null during edit profile activity result. Cannot refresh user info.");
                        }
                    } else {
                        Log.i(TAG, "EditProfileActivity finished with non-OK result or cancelled.");
                    }
                });

        shippingAddressLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "ShippingAddressLauncher callback received. ResultCode: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Log.i(TAG, "ShippingAddressActivity finished with RESULT_OK. Refreshing user info.");
                        if (token != null && !token.isEmpty()) {
                            getUserInfo(token);
                            userInfoHasChanged = true;
                        } else {
                            Log.e(TAG, "Token is null during address activity result. Cannot refresh user info.");
                        }

                    } else {
                        Log.i(TAG, "ShippingAddressActivity finished with non-OK result or cancelled.");
                    }
                });

        orderHistoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "OrderHistoryLauncher callback received. ResultCode: " + result.getResultCode());
                });

        getUserInfo(token);

        setupButtonClickListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (userInfoHasChanged) {
                setResult(RESULT_OK);
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void AnhXa(){
        emailTextView = findViewById(R.id.emailTextView);
        fullNameTextView = findViewById(R.id.fullNameTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        addressTextView = findViewById(R.id.addressTextView);
        avatarImageView = findViewById(R.id.avatarImageView);
        orderHistoryButton = findViewById(R.id.orderHistoryButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        shippingAddressButton = findViewById(R.id.shippingAddressButton);
        toolbar = findViewById(R.id.toolbarProfile);

    }

    private void setupButtonClickListeners() {
        editProfileButton.setOnClickListener(v -> navigateToEditProfile());
        shippingAddressButton.setOnClickListener(v -> navigateToShippingAddress());
        orderHistoryButton.setOnClickListener(v -> navigateToOrderHistory());
    }

    private void getUserInfo(String token) {
        Log.d(TAG, "Attempting to get user info with token...");
        ApiService.apiService.getUserInfo("Bearer " + token)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            currentUser = response.body(); // Store the user object
                            Log.d(TAG, "User info received successfully. User ID: " + currentUser.getUserId()); // Log user ID

                            updateUI(currentUser);

                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) errorBody = response.errorBody().string();
                            } catch (Exception e) { Log.e(TAG, "Error reading error body", e); }
                            Log.e(TAG, "Failed to get user info. Code: " + response.code() + ", Message: " + response.message() + ", Body: " + errorBody);
                            Toast.makeText(ProfileActivity.this, "Error loading profile: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Log.e(TAG, "API call failed: " + t.getMessage(), t);
                        Toast.makeText(ProfileActivity.this, "Network error. Please check connection.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(User userInfo) {
        if (userInfo == null) {
            Log.w(TAG, "updateUI called with null userInfo.");
            return;
        }
        currentUser = userInfo;

        emailTextView.setText(userInfo.getEmail() != null ? userInfo.getEmail() : "N/A");
        fullNameTextView.setText(userInfo.getFullName() != null ? userInfo.getFullName() : "N/A");
        phoneTextView.setText(userInfo.getPhone() != null ? userInfo.getPhone() : "N/A");

        StringBuilder addressBuilder = new StringBuilder();
        List<Address> addresses = userInfo.getAddresses();
        Log.d(TAG, "Addresses list received. Size: " + (addresses != null ? addresses.size() : "null"));
        Address defaultAddress = null;

        if (addresses != null && !addresses.isEmpty()) {
            for (Address address : addresses) {
                if (address != null && address.isDefaultAddress()) {
                    defaultAddress = address;
                    break;
                }
            }

            if (defaultAddress != null) {
                addressBuilder.append(defaultAddress.getHouseNumber() != null ? defaultAddress.getHouseNumber() + ", " : "");
                if (addressBuilder.length() > 2 && addressBuilder.substring(addressBuilder.length() - 2).equals(", ")) {
                    addressBuilder.setLength(addressBuilder.length() - 2);
                }
                Log.d(TAG, "Default address found and displayed.");
            } else {
                addressBuilder.append("Chưa có địa chỉ mặc định được thiết lập.");
                Log.w(TAG, "No default address found among the list.");
            }

        } else {
            addressBuilder.append("Chưa có địa chỉ.");
            Log.d(TAG, "Address list is null or empty.");
        }

        addressTextView.setText(addressBuilder.toString().trim());

        if (userInfo.getAvatar() != null && !userInfo.getAvatar().isEmpty()) {
            Glide.with(ProfileActivity.this)
                    .load(userInfo.getAvatar())
                    .placeholder(R.drawable.icon_avatar) // Ảnh mặc định khi đang load
                    .error(R.drawable.icon_avatar) // Ảnh mặc định nếu load lỗi
                    .circleCrop() // Bo tròn ảnh
                    .into(avatarImageView);
        } else {
            Log.d(TAG, "updateUI - Avatar URL is null or empty, setting default image.");
            Glide.with(ProfileActivity.this)
                    .load(R.drawable.icon_avatar)
                    .circleCrop()
                    .into(avatarImageView);
        }
    }

    private void navigateToEditProfile() {
        // Check if user data is loaded before attempting to pass it
        if (currentUser == null) {
            Toast.makeText(this, "User information not loaded yet.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "navigateToEditProfile called but currentUser is null.");
            return;
        }
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Authentication error, cannot edit profile.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "navigateToEditProfile called but token is null or empty.");
            return;
        }

        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        intent.putExtra("CURRENT_FULL_NAME", currentUser.getFullName());
        intent.putExtra("CURRENT_PHONE", currentUser.getPhone());
        intent.putExtra("CURRENT_AVATAR_URL", currentUser.getAvatar()); // Pass current avatar URL
        intent.putExtra("TOKEN", token); // Pass token needed for the PUT request in EditProfileActivity

        Log.i(TAG, "Launching EditProfileActivity using launcher...");
        editProfileLauncher.launch(intent);
    }

    private void navigateToShippingAddress() {
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Authentication error, cannot view addresses.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "navigateToShippingAddress called but token is null or empty.");
            return;
        }
        Intent intent = new Intent(ProfileActivity.this, ShippingAddressActivity.class);
        intent.putExtra("TOKEN", token); // Pass the token to the address activity

        Log.i(TAG, "Launching ShippingAddressActivity using launcher...");
        shippingAddressLauncher.launch(intent);
    }

    private void navigateToOrderHistory() {
        if (currentUser == null || currentUser.getUserId() <= 0) {
            Toast.makeText(this, "User information not loaded yet or invalid.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "navigateToOrderHistory called but currentUser or User ID is invalid: " + (currentUser != null ? currentUser.getUserId() : "null"));
            return;
        }
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Authentication error, cannot view order history.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "navigateToOrderHistory called but token is null or empty.");
            return;
        }

        Intent intent = new Intent(ProfileActivity.this, OrderHistoryActivity.class);
        intent.putExtra("userId", currentUser.getUserId());
        intent.putExtra("token", token); // Pass token if OrderHistoryActivity needs it for API calls

        Log.i(TAG, "Launching OrderHistoryActivity using launcher with User ID: " + currentUser.getUserId());
        orderHistoryLauncher.launch(intent);
    }

    @Override
    public void finish() {
        if (userInfoHasChanged) {
            setResult(RESULT_OK);
        }
        super.finish();
    }
}