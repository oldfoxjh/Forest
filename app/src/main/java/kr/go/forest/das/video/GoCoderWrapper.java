package kr.go.forest.das.video;

import android.content.Context;

import com.wowza.gocoder.sdk.api.WowzaGoCoder;

import kr.go.forest.das.Log.LogWrapper;

public class GoCoderWrapper {

    private static final String SDK_SAMPLE_APP_LICENSE_KEY = "GOSK-CC46-010C-71BE-14E1-199B";

    protected static WowzaGoCoder sGoCoderSDK = null;
    protected GoCoderSDKPrefs mGoCoderSDKPrefs;

    public void startSDKRegistration(Context context)
    {
        if(sGoCoderSDK == null)
        {
            sGoCoderSDK = WowzaGoCoder.init(context, SDK_SAMPLE_APP_LICENSE_KEY);

            if(sGoCoderSDK == null)
            {
                LogWrapper.e("GoCoderWrapper", WowzaGoCoder.getLastError().toString());

                // 인터페이스 콜
                return;
            }
        }


    }
}
