package vn.iostar.doan.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import vn.iostar.doan.model.goong.GoongAutocompleteResponse; // Need to create this model

public interface GoongApiService {

    @GET("Place/Autocomplete")
    Call<GoongAutocompleteResponse> autocomplete(
            @Query("api_key") String apiKey,
            @Query("input") String input,
            @Query("limit") int limit
    );

}