package vn.iostar.doan.api;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import vn.iostar.doan.utils.Constants;
public class ApiService1 {
    private static Retrofit goongRetrofit;
    private static GoongApiService goongApiService;

    public static GoongApiService getGoongApiService() {
        if (goongRetrofit == null) {
            goongRetrofit = new Retrofit.Builder()
                    .baseUrl(Constants.GOONG_API_BASE_URL) // Sử dụng URL gốc của Goong
                    .addConverterFactory(GsonConverterFactory.create()) // Có thể dùng Gson converter nếu cần
                    .build();
        }
        if (goongApiService == null) {
            goongApiService = goongRetrofit.create(GoongApiService.class);
        }
        return goongApiService;
    }}
