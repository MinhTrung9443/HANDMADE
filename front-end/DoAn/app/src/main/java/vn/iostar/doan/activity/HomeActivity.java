package vn.iostar.doan.activity; // Thay thế bằng package name thực tế của bạn

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.iostar.doan.R; // Thay thế bằng package name thực tế của bạn
import vn.iostar.doan.adapter.CategoryAdapter;
import vn.iostar.doan.adapter.ProductAdapter;
import vn.iostar.doan.adapter.ViewFlipperManager;
import vn.iostar.doan.api.ApiService;
import vn.iostar.doan.databinding.ActivityAboutappBinding;
import vn.iostar.doan.model.Address;
import vn.iostar.doan.model.Category;
import vn.iostar.doan.model.Product;
import vn.iostar.doan.model.User;
import vn.iostar.doan.utils.SharedPreferencesUtils;


public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private String authToken;
    private RecyclerView rcCate, rclProduct;
    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private List<Product> filteredProductList = new ArrayList<>();
    private List<Category> categoryList = new ArrayList<>();
    private SearchView searchView;
    private ViewFlipper viewFlipperMain;
    private ImageView imgUser, ivCart, ivAboutUs, ivLocation, chatBotIcon, ivHome; // Thêm ivHome
        private TextView tvLocationAddress;
    private ActivityResultLauncher<Intent> profileActivityLauncher;
    private BroadcastReceiver defaultAddressChangeReceiver;

    // Khai báo View cho các layout được include
    private View headerLayout;
    private View bottomNavLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        AnhXa();
        setupLaunchers();
        setupBroadcastReceivers();
        loadUserInfoAndInitData();
        setupSearchView();
        setupUserMenu();
        setupBottomNavigation();
    }
    private void setupLaunchers() {
        profileActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "ProfileActivity returned RESULT_OK, refreshing user info on Home.");
                        // loadUserInfoAndInitData(); // Hoặc một phương thức chỉ cập nhật UI người dùng
                        fetchUserAddressForHeader(); // Gọi phương thức chỉ cập nhật địa chỉ
                    }
                }
        );
    }

    private void setupBroadcastReceivers() {
        defaultAddressChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ShippingAddressActivity.ACTION_DEFAULT_ADDRESS_CHANGED.equals(intent.getAction())) {
                    Log.d(TAG, "HomeActivity received ACTION_DEFAULT_ADDRESS_CHANGED broadcast, refreshing address.");
                    fetchUserAddressForHeader(); // Gọi phương thức chỉ cập nhật địa chỉ
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                defaultAddressChangeReceiver,
                new IntentFilter(ShippingAddressActivity.ACTION_DEFAULT_ADDRESS_CHANGED)
        );
    }


    // Tách riêng logic lấy và hiển thị địa chỉ để dễ gọi lại
    private void fetchUserAddressForHeader() {
        if (this.authToken == null || this.authToken.isEmpty()) {
            Log.w(TAG, "Auth token is null/empty in fetchUserAddressForHeader. Cannot fetch.");
            if (tvLocationAddress != null) tvLocationAddress.setText("Lỗi xác thực.");
            return;
        }
        ApiService.apiService.getUserInfo("Bearer " + this.authToken)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            User user = response.body();
                            List<Address> addresses = user.getAddresses();
                            Address defaultAddress = null;
                            if (addresses != null && !addresses.isEmpty()) {
                                for (Address address : addresses) {
                                    if (address != null && address.isDefaultAddress()) {
                                        defaultAddress = address;
                                        break;
                                    }
                                }
                            }
                            if (tvLocationAddress != null) {
                                if (defaultAddress != null) {
                                    //String fullAddress = defaultAddress.getHouseNumber() + ", " + defaultAddress.getWard() + ", " + defaultAddress.getDistrict() + ", " + defaultAddress.getCity() + ", " + defaultAddress.getCountry();
                                    String fullAddress = defaultAddress.getHouseNumber() + ", "+
                                            defaultAddress.getCountry();
                                    tvLocationAddress.setText(fullAddress);
                                    Log.d(TAG, "Updated default address on Home: " + fullAddress);
                                } else {
                                    tvLocationAddress.setText("Chưa có địa chỉ mặc định.");
                                }
                            }
                        } else {
                            Log.e(TAG, "Error fetching user info for Home header: " + response.code());
                            if (tvLocationAddress != null) tvLocationAddress.setText("Lỗi tải địa chỉ.");
                        }
                    }
                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Log.e(TAG, "API failure fetching user info for Home header", t);
                        if (tvLocationAddress != null) tvLocationAddress.setText("Lỗi kết nối.");
                    }
                });
    }
    private void AnhXa() {
        // Ánh xạ các thành phần chính (trực tiếp trong activity_home.xml)
        rcCate = findViewById(R.id.rc_category);
        rclProduct = findViewById(R.id.rclcon);
        searchView = findViewById(R.id.searchView); // SearchView nằm trong LinearLayout có ID searchLayout
        viewFlipperMain = findViewById(R.id.viewFlipperMain);
        chatBotIcon = findViewById(R.id.chatBotIcon); // ChatBotIcon nằm trực tiếp trong activity_home.xml

        // Ánh xạ các View include layout
        headerLayout = findViewById(R.id.headerLayout); // Tìm include header
        bottomNavLayout = findViewById(R.id.bottom_navigation); // Tìm include bottom navigation

        // Ánh xạ các thành phần bên trong Header (nếu include header được tìm thấy)
        if (headerLayout != null) {
            imgUser = headerLayout.findViewById(R.id.imgUser);
            tvLocationAddress = headerLayout.findViewById(R.id.tvLocationAddress);
            if (imgUser == null) Log.w(TAG, "imgUser not found inside headerLayout.");
            if (tvLocationAddress == null) Log.w(TAG, "tvLocationAddress not found inside headerLayout.");
        } else {
            Log.e(TAG, "Header layout include with ID headerLayout not found! Header views will be null.");
        }

        // Ánh xạ các thành phần bên trong Bottom Navigation (nếu include bottom navigation được tìm thấy)
        if (bottomNavLayout != null) {
            // Lưu ý: Các icon nằm trong LinearLayout con có ID bottomNav trong bottom_nav_menu.xml
            View bottomNavLinearLayout = bottomNavLayout.findViewById(R.id.bottomNav);
            if (bottomNavLinearLayout != null) {
                ivHome = bottomNavLinearLayout.findViewById(R.id.ivMenuBottom); // Icon Home trong bottom nav
                ivLocation = bottomNavLinearLayout.findViewById(R.id.ivLocation); // Icon Location trong bottom nav
                ivAboutUs = bottomNavLinearLayout.findViewById(R.id.ivaboutus); // Icon About Us trong bottom nav
                ivCart = bottomNavLinearLayout.findViewById(R.id.ivcart); // Icon Cart trong bottom nav

                if (ivHome == null) Log.w(TAG, "ivMenuBottom (Home) not found inside bottomNav LinearLayout.");
                if (ivLocation == null) Log.w(TAG, "ivLocation not found inside bottomNav LinearLayout.");
                if (ivAboutUs == null) Log.w(TAG, "ivaboutus not found inside bottomNav LinearLayout.");
                if (ivCart == null) Log.w(TAG, "ivcart not found inside bottomNav LinearLayout.");
            } else {
                Log.e(TAG, "LinearLayout with ID bottomNav not found inside bottom_navigation layout!");
            }

        } else {
            Log.e(TAG, "Bottom navigation layout include with ID bottom_navigation not found! Bottom nav icons will be null.");
        }

        // Log lỗi nghiêm trọng nếu các thành phần chính không tìm thấy
        if (rcCate == null) Log.e(TAG, "rcCate is null after AnhXa!");
        if (rclProduct == null) Log.e(TAG, "rclProduct is null after AnhXa!");
        if (searchView == null) Log.e(TAG, "searchView is null after AnhXa!");
        if (viewFlipperMain == null) Log.e(TAG, "viewFlipperMain is null after AnhXa!");
        if (chatBotIcon == null) Log.e(TAG, "chatBotIcon is null after AnhXa!");
    }

    private void loadUserInfoAndInitData() {
        String tokenFromIntent = getIntent().getStringExtra("token");
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        if (tokenFromIntent != null && !tokenFromIntent.isEmpty()) {
            this.authToken = tokenFromIntent;
            SharedPreferencesUtils.saveString(this, "token", authToken);
            prefs.edit().putString("token", authToken).apply();
        } else {
            this.authToken = prefs.getString("token", "");
        }

        if (this.authToken == null || this.authToken.isEmpty()) {
            Log.e(TAG, "Token is missing! Redirecting to Login.");
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        productAdapter = new ProductAdapter(this, new ArrayList<>(), this.authToken);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        rclProduct.setLayoutManager(gridLayoutManager);
        rclProduct.setAdapter(productAdapter);

        ApiService.apiService.getUserInfo("Bearer " + this.authToken)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            User user = response.body();
                            List<Address> addresses = user.getAddresses();

                            Log.d(TAG, "Addresses list received for Home. Size: " + (addresses != null ? addresses.size() : "null"));

                            Address defaultAddress = null;

                            if (addresses != null && !addresses.isEmpty()) {
                                for (Address address : addresses) {
                                    if (address != null && address.isDefaultAddress()) {
                                        defaultAddress = address;
                                        Log.d(TAG, "Found default address in the list for Home.");
                                        break;
                                    }
                                }

                                if (defaultAddress != null) {
                                    String fullAddress = defaultAddress.getHouseNumber() + ", "+
                                            defaultAddress.getCountry();

                                    if (tvLocationAddress != null) {
                                        tvLocationAddress.setText(fullAddress);
                                        Log.d(TAG, "Default address displayed on Home: " + fullAddress);
                                    }
                                } else {
                                    if (tvLocationAddress != null) {
                                        tvLocationAddress.setText("Chưa có địa chỉ mặc định.");
                                        Log.w(TAG, "Address list is not empty, but no default address found for Home.");
                                    }
                                }

                            } else {
                                if (tvLocationAddress != null) {
                                    tvLocationAddress.setText("Chưa có địa chỉ.");
                                    Log.d(TAG, "Address list is null or empty for Home.");
                                }
                            }
                        } else {
                            Log.e(TAG, "Error fetching user info for Home: " + response.code() + " - " + response.message());
                            if (tvLocationAddress != null) {
                                tvLocationAddress.setText("Lỗi tải địa chỉ.");
                            }
                            if (response.code() == 401 || response.code() == 403) {
                                Log.e(TAG, "Authentication error fetching user info for Home. Token may be invalid.");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Log.e(TAG, "Failed to connect to server for Home", t);
                        if (tvLocationAddress != null) {
                            tvLocationAddress.setText("Lỗi kết nối.");
                        }
                    }
                });


        getAllProducts();
        GetCategory();
        setupViewFlipper();
    }

    private void getAllProducts() {
        if (productAdapter == null || this.authToken == null) {
            Log.e(TAG, "Adapter or token is null. Cannot fetch all products.");
            return;
        }
        ApiService.apiService.getAllProducts()
                .enqueue(new Callback<List<Product>>() {
                    @Override
                    public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            productList = response.body();
                            productAdapter.updateList(productList);
                            // Tùy chọn: Hiện lại flipper khi tải xong SP nếu search bar trống
                            if (viewFlipperMain != null && searchView != null && searchView.getQuery().toString().isEmpty()) {
                                // viewFlipperMain.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Log.e(TAG, "Response error loading all products: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Product>> call, Throwable t) {
                        Log.e(TAG, "Request failed loading all products", t);
                    }
                });
    }

    private void setupViewFlipper() {
        if (viewFlipperMain != null) {
            ViewFlipperManager.setupViewFlipper(viewFlipperMain, this);
        } else {
            Log.e(TAG, "viewFlipperMain is null! Cannot setup ViewFlipper.");
        }
    }

    private void GetCategory() {
        ApiService.apiService.getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList = response.body();

                    categoryAdapter = new CategoryAdapter(HomeActivity.this, categoryList, category -> {
                        getProductsByCategory(category.getCategoryId());
                    });
                    rcCate.setHasFixedSize(true);

                    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(
                            HomeActivity.this, LinearLayoutManager.HORIZONTAL, false
                    );
                    rcCate.setLayoutManager(layoutManager);
                    rcCate.setAdapter(categoryAdapter);

                } else {
                    Log.e(TAG, "Response Error loading categories: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e(TAG, "Failure loading categories: " + t.getMessage(), t);
            }
        });
    }

    private void getProductsByCategory(Long categoryId) {
        if (productAdapter == null || this.authToken == null) {
            Log.e(TAG, "Adapter or token is null. Cannot fetch products by category.");
            return;
        }
        ApiService.apiService.getProductsByCategory(categoryId).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    if (viewFlipperMain != null && viewFlipperMain.getVisibility() == View.VISIBLE) {
                        viewFlipperMain.setVisibility(View.GONE);
                    }
                    productAdapter.updateList(products);
                } else {
                    Log.e(TAG, "Response error loading products by category: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.e(TAG, "Failure loading products by category: " + t.getMessage(), t);
            }
        });
    }

    private void setupSearchView() {
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterProducts(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterProducts(newText);
                    return true;
                }
            });
            searchView.setOnCloseListener(() -> {
                filterProducts("");
                if (viewFlipperMain != null) {
                    viewFlipperMain.setVisibility(View.VISIBLE);
                }
                return false;
            });

        } else {
            Log.e(TAG, "searchView is null! Cannot set listener.");
        }
    }

    private void filterProducts(String keyword) {
        if (productList == null || productAdapter == null) {
            Log.w(TAG, "productList or productAdapter is null. Cannot filter.");
            return;
        }

        if (viewFlipperMain != null && viewFlipperMain.getVisibility() == View.VISIBLE) {
            viewFlipperMain.setVisibility(View.GONE);
        }

        filteredProductList.clear();
        if (keyword == null || keyword.isEmpty()) {
            productAdapter.updateList(productList);
        } else {
            String lowerCaseKeyword = keyword.toLowerCase();
            for (Product product : productList) {
                if (product.getName() != null && product.getName().toLowerCase().contains(lowerCaseKeyword)) {
                    filteredProductList.add(product);
                }
            }
            productAdapter.updateList(filteredProductList);
        }
    }

    private void setupUserMenu() {
        if (imgUser != null) {
            imgUser.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(HomeActivity.this, imgUser);
                popupMenu.getMenuInflater().inflate(R.menu.menu_user, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_profile) {
                        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                        intent.putExtra("token", authToken);
                        if (profileActivityLauncher != null) {
                            profileActivityLauncher.launch(intent);
                        } else {
                            Log.e(TAG, "profileActivityLauncher is null! Cannot launch ProfileActivity for result.");
                            startActivity(intent); // Fallback nếu launcher null
                        }
                        return true;
                    } else if (id == R.id.menu_logout) {
                        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                        prefs.edit().remove("token").apply();

                        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        } else {
            Log.e(TAG, "imgUser is null! Cannot setup user menu.");
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (defaultAddressChangeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(defaultAddressChangeReceiver);
        }
    }
    private void setupBottomNavigation() {
        // Xử lý click cho icon Home (ivMenuBottom) - Thường ở trang chủ thì không làm gì hoặc cuộn lên đầu
        if (ivHome != null) {
            ivHome.setOnClickListener(v -> {
                Log.d(TAG, "Home icon clicked");
                // Không cần làm gì đặc biệt vì đã ở trang chủ
                // Tùy chọn: rcCate.scrollToPosition(0); rclProduct.scrollToPosition(0);
                // Tùy chọn: Nếu bạn có SwipeRefreshLayout, có thể kích hoạt refresh.
            });
        } else {
            Log.e(TAG, "ivHome (ivMenuBottom) is null! Cannot setup Home click listener.");
        }

        // Xử lý click cho icon Giỏ hàng (Cart)
        if (ivCart != null) {
            ivCart.setOnClickListener(v -> {
                Log.d(TAG, "Cart icon clicked");
                Intent intent = new Intent(HomeActivity.this, CartActivity.class);
                intent.putExtra("token", authToken);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "ivCart is null! Cannot setup cart click listener.");
        }

        // Xử lý click cho icon Chatbot
        if (chatBotIcon != null) { // ChatBotIcon nằm trực tiếp trong activity_home.xml
            chatBotIcon.setOnClickListener(v -> {
                Log.d(TAG, "Chatbot icon clicked");
                Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
                startActivity(intent);
            });
        } else {
            // Log lỗi này đã có trong AnhXa, nhưng giữ ở đây để đảm bảo nếu AnhXa không báo lỗi
            Log.e(TAG, "chatBotIcon is null! Cannot setup chatbot click listener.");
        }


        // Xử lý click cho icon About Us (ivaboutus trong bottom nav)
        if (ivAboutUs != null) {
            ivAboutUs.setOnClickListener(v -> {
                Log.d(TAG, "About Us icon clicked");
                Intent aboutIntent = new Intent(HomeActivity.this, AboutUsActivity.class); // Sử dụng AboutUsActivity như bạn đã đề cập
                startActivity(aboutIntent);
            });
        } else {
            Log.e(TAG, "ivAboutUs is null! Cannot setup About Us click listener.");
        }

        // Xử lý click cho icon Location (ivLocation trong bottom nav)
        if (ivLocation != null) {
            ivLocation.setOnClickListener(v -> {
                Log.d(TAG, "Location icon clicked");
                // Thay đổi action mong muốn ở đây. Ví dụ mở màn hình AddressActivity:
                Intent locationIntent = new Intent(HomeActivity.this, AboutAppActivity.class); // Giả định bạn có AddressActivity
                locationIntent.putExtra("token", authToken); // Truyền token nếu cần
                startActivity(locationIntent);

                // Nếu bạn thực sự muốn mở AboutAppActivity như code cũ:
                // Intent aboutIntent = new Intent(HomeActivity.this, AboutAppActivity.class);
                // startActivity(aboutIntent);

            });
        } else {
            Log.e(TAG, "ivLocation is null! Cannot setup Location click listener.");
        }

        // <<< CẦN THÊM XỬ LÝ CHO CÁC ICON KHÁC TRONG BOTTOM NAV CỦA BẠN NẾU CÓ >>>
        // Đảm bảo bạn đã ánh xạ chúng trong AnhXa và thêm listener ở đây.
    }
}

