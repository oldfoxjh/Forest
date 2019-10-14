package kr.go.forest.das.network;

import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.LoginResponse;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface IRealTimeVideo {
    // Get Test
    @Headers("Content-Type: application/json")
    @GET("api/info/{user}")
    Call<LoginResponse> getTest(@Path("user")  String user);

    // Post Test - user Converter
    @Headers("Content-Type: application/json")
    @POST("api/info")
    Call<LoginResponse> postTest(@Body RequestBody info);

    // Post Test - user Converter
    @Headers("Content-Type: application/json")
    @POST("api/info")
    Call<LoginResponse> postTestConvert(@Body DroneInfo info);
}
