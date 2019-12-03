package kr.go.forest.das.network;

import kr.go.forest.das.Model.DroneInfoRequest;
import kr.go.forest.das.Model.DroneInfoResponse;
import kr.go.forest.das.Model.LoginRequest;
import kr.go.forest.das.Model.LoginResponse;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IApiService {

    public final Retrofit retrofit = new Retrofit.Builder()
            //.baseUrl("http://192.168.1.10/weatherforecast/")
            //.baseUrl("http://mvst2.iptime.org:8081/portalo/dim/flnMngme/")
            .baseUrl("http://192.168.10.152:8081/portalo/dim/flnMngme/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    /**
     * 사용자 로그인 요청
     * @param info 로그인 정보
     * @return
     */
    @Headers({
        "content-type:application/json",
        "accept:application/json"
    })
    @POST("appLogin.do")
    //@POST("login")
    Call<LoginResponse> postLogin(@Body LoginRequest info);

    /**
     * 드론 위치 및 상태정보 전송
     * @param info 드론위치정보, 드론 기체상태 정보
     * @return
     */
    @Headers({
            "content-type:application/json",
            "accept:application/json"
    })
    @POST("insertFlnInfo.do")
    Call<DroneInfoResponse> postDroneInfo(@Body DroneInfoRequest info);

    /**
     * 실시간 동영상 url 요청
     * @param info 전송장치 ID
     * @return
     */
    @Headers({
            "content-type:application/json",
            "accept:application/json"
    })
    @POST("stream")
    Call<LoginResponse> streamUrl(@Body LoginRequest info);
}
