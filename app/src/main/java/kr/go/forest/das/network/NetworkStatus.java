package kr.go.forest.das.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 모바일 장치의 인터넷 연결 상태 확인하는 클래스
 */
public class NetworkStatus {
    /**
     * 인터넷이 연결되었는지 확인
     */
    public static boolean isInternetConnected(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);

        // 네트워크 정보 불러오기
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        // 네트워크 정보가 있으면 3G/4G 또는 와이파이인지 확인
        if(networkInfo != null){
            int type = networkInfo.getType();
            if(type == ConnectivityManager.TYPE_MOBILE || type == ConnectivityManager.TYPE_WIFI){
                return true;
            }
        }

        // 네트워크 정보 없음.
        return false;
    }
}
