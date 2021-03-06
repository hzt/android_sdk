package com.anythink.network.toutiao;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATMediationSetting;
import com.anythink.core.api.ErrorCode;
import com.anythink.interstitial.unitgroup.api.CustomInterstitialAdapter;
import com.anythink.interstitial.unitgroup.api.CustomInterstitialListener;
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.TTInteractionAd;

import java.util.Map;

public class TTATInterstitialAdapter extends CustomInterstitialAdapter {
    private final String TAG = getClass().getSimpleName();

    String slotId = "";
    boolean isVideo = false;

    private TTFullScreenVideoAd mTTFullScreenVideoAd;


    //TT Advertising event listener
    TTInteractionAd.AdInteractionListener interactionListener = new TTInteractionAd.AdInteractionListener() {

        @Override
        public void onAdClicked() {
            if (mImpressListener != null) {
                mImpressListener.onInterstitialAdClicked(TTATInterstitialAdapter.this);
            }
        }

        @Override
        public void onAdShow() {
            if (mImpressListener != null) {
                mImpressListener.onInterstitialAdShow(TTATInterstitialAdapter.this);
            }
        }

        @Override
        public void onAdDismiss() {
            if (mImpressListener != null) {
                mImpressListener.onInterstitialAdClose(TTATInterstitialAdapter.this);
            }
        }

    };


    TTAdNative.FullScreenVideoAdListener ttFullScrenAdListener = new TTAdNative.FullScreenVideoAdListener() {
        @Override
        public void onError(int code, String message) {
            if (mLoadResultListener != null) {
                mLoadResultListener.onInterstitialAdLoadFail(TTATInterstitialAdapter.this, ErrorCode.getErrorCode(ErrorCode.noADError, String.valueOf(code), message));
            }
        }

        @Override
        public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ad) {
            mTTFullScreenVideoAd = ad;

            if (mLoadResultListener != null) {
                mLoadResultListener.onInterstitialAdDataLoaded(TTATInterstitialAdapter.this);
            }
        }

        @Override
        public void onFullScreenVideoCached() {
            if (mLoadResultListener != null) {
                mLoadResultListener.onInterstitialAdLoaded(TTATInterstitialAdapter.this);
            }
        }

    };

    TTFullScreenVideoAd.FullScreenVideoAdInteractionListener ttFullScreenEventListener = new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {

        @Override
        public void onAdShow() {
            if (mImpressListener != null) {
                mImpressListener.onInterstitialAdShow(TTATInterstitialAdapter.this);
                mImpressListener.onInterstitialAdVideoStart(TTATInterstitialAdapter.this);
            }
        }

        @Override
        public void onAdVideoBarClick() {
            if (mImpressListener != null) {
                mImpressListener.onInterstitialAdClicked(TTATInterstitialAdapter.this);
            }
        }

        @Override
        public void onAdClose() {
            if (mImpressListener != null) {
                mImpressListener.onInterstitialAdClose(TTATInterstitialAdapter.this);
            }
        }

        @Override
        public void onVideoComplete() {
            if (mImpressListener != null) {
                mImpressListener.onInterstitialAdVideoEnd(TTATInterstitialAdapter.this);
            }
        }

        @Override
        public void onSkippedVideo() {
        }

    };


    @Override
    public void loadInterstitialAd(final Context context, Map<String, Object> serverExtras, final ATMediationSetting mediationSetting, final CustomInterstitialListener customRewardVideoListener) {

        mLoadResultListener = customRewardVideoListener;

        if (serverExtras == null) {
            if (mLoadResultListener != null) {
                mLoadResultListener.onInterstitialAdLoadFail(this, ErrorCode.getErrorCode(ErrorCode.noADError, "", "This placement's params in server is null!"));
            }
            return;
        }

        String appId = (String) serverExtras.get("app_id");
        slotId = (String) serverExtras.get("slot_id");

        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(slotId)) {
            if (mLoadResultListener != null) {
                mLoadResultListener.onInterstitialAdLoadFail(this, ErrorCode.getErrorCode(ErrorCode.noADError, "", "app_id or slot_id is empty!"));
            }
            return;
        }

        if (serverExtras.containsKey("is_video")) {
            if (serverExtras.get("is_video").toString().equals("1")) {
                isVideo = true;
            }
        }

        if (!(context instanceof Activity)) {
            if (mLoadResultListener != null) {
                mLoadResultListener.onInterstitialAdLoadFail(this, ErrorCode.getErrorCode(ErrorCode.noADError, "", "context must be activity"));
            }
            return;
        }

        int layoutType = 0;
        if (serverExtras.containsKey("layout_type")) {
            layoutType = Integer.parseInt(serverExtras.get("layout_type").toString());
        }


        final int finalLayoutType = layoutType;
        TTATInitManager.getInstance().initSDK(context, serverExtras, new TTATInitManager.InitCallback() {
            @Override
            public void onFinish() {
                startLoad(context, mediationSetting, finalLayoutType);
            }
        });
    }

    private void startLoad(Context context, ATMediationSetting mediationSetting, int layoutType) {
        TTAdManager ttAdManager = TTAdSdk.getAdManager();

        /**Get the width set by the developer**/
        TTATInterstitialSetting setting;
        int developerSetExpressWidth = 0;
        if (mediationSetting instanceof TTATInterstitialSetting) {
            setting = (TTATInterstitialSetting) mediationSetting;
            developerSetExpressWidth = setting.getInterstitialWidth();
        }


        TTAdNative mTTAdNative = ttAdManager.createAdNative(context);//baseContext is recommended for Activity
        AdSlot.Builder adSlotBuilder = new AdSlot.Builder().setCodeId(slotId);
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        adSlotBuilder.setImageAcceptedSize(width, height); //must be set
        adSlotBuilder.setAdCount(1);

        //Only support fullscreen video
        AdSlot adSlot = adSlotBuilder.build();
        mTTAdNative.loadFullScreenVideoAd(adSlot, ttFullScrenAdListener);
    }

    @Override
    public boolean isAdReady() {
        return mTTFullScreenVideoAd != null;
    }

    @Override
    public void show(Context context) {
        try {
            if (mTTFullScreenVideoAd != null && context instanceof Activity) {
                mTTFullScreenVideoAd.setFullScreenVideoAdInteractionListener(ttFullScreenEventListener);
                mTTFullScreenVideoAd.showFullScreenVideoAd((Activity) context);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void clean() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {

    }

    @Override
    public String getSDKVersion() {
        return TTATConst.getNetworkVersion();
    }

    @Override
    public String getNetworkName() {
        return TTATInitManager.getInstance().getNetworkName();
    }
}
