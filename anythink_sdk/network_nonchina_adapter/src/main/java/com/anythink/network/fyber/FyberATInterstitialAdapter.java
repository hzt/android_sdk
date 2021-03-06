package com.anythink.network.fyber;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.anythink.core.api.ATMediationSetting;
import com.anythink.core.api.AdError;
import com.anythink.core.api.ErrorCode;
import com.anythink.interstitial.unitgroup.api.CustomInterstitialAdapter;
import com.anythink.interstitial.unitgroup.api.CustomInterstitialListener;
import com.fyber.inneractive.sdk.config.k;
import com.fyber.inneractive.sdk.external.ImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerWithImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.fyber.inneractive.sdk.external.VideoContentListener;

import java.util.Map;

public class FyberATInterstitialAdapter extends CustomInterstitialAdapter {

    InneractiveAdSpot mSpot;

    @Override
    public void loadInterstitialAd(Context context, Map<String, Object> serverExtras, ATMediationSetting mediationSetting, CustomInterstitialListener customInterstitialListener) {


        //todo  mock data
//        serverExtras.put("app_id", "102960");
//        serverExtras.put("spot_id", "150946");
//        serverExtras.put("video_muted", "1");

//        serverExtras.put("app_id", "112759");
//        serverExtras.put("spot_id", "217617");
//        serverExtras.put("video_muted", "1");
        //end  mock data

        String appId = "";
        String spotId = "";
        String isMute = "0";

        mLoadResultListener = customInterstitialListener;

        if (serverExtras.containsKey("app_id")) {
            appId = (String) serverExtras.get("app_id");
        }

        if (serverExtras.containsKey("spot_id")) {
            spotId = (String) serverExtras.get("spot_id");
        }
        if (serverExtras.containsKey("video_muted")) {//1: mute, 0: sound
            isMute = (String) serverExtras.get("video_muted");
        }

        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(spotId)) {
            if (this.mLoadResultListener != null) {
                AdError adError = ErrorCode.getErrorCode(ErrorCode.noADError, "", "Fyber app_id、spot_id could not be null.");
                this.mLoadResultListener.onInterstitialAdLoadFail(this, adError);
            }
            return;
        }

        FyberATInitManager.getInstance().initSDK(context, serverExtras);

        // spot integration for display Square
        mSpot = InneractiveAdSpotManager.get().createSpot();

        // adding the adview controller
        InneractiveFullscreenUnitController controller = new InneractiveFullscreenUnitController();
        mSpot.addUnitController(controller);

        InneractiveAdRequest adRequest = new InneractiveAdRequest(spotId);

        // enriching with user and keywords data
        // for gender values see: InneractiveUserConfig.Gender
//        adRequest.setUserParams( new InneractiveUserConfig()
//                .setGender(<gender>)
//                .setZipCode("<zip_code>")
//                .setAge(<age>));
        // Add keywords. Separated by a comma
//        adRequest.setKeywords("pop,rock,music");

        // Optionally, mute non rewarded videos by using the following -
        if (TextUtils.equals(isMute, "1")) {
            adRequest.setMuteVideo(true);
        }


        mSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot inneractiveAdSpot) {
                if (mLoadResultListener != null) {
                    mLoadResultListener.onInterstitialAdLoaded(FyberATInterstitialAdapter.this);
                }
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot inneractiveAdSpot, InneractiveErrorCode inneractiveErrorCode) {
                if (mLoadResultListener != null) {
                    mLoadResultListener.onInterstitialAdLoadFail(FyberATInterstitialAdapter.this,
                            ErrorCode.getErrorCode(ErrorCode.noADError, "", inneractiveErrorCode.name() + ", " + inneractiveErrorCode.getMetricable()));
                }
            }
        });

        //when ready to perform the ad request
        mSpot.requestAd(adRequest);

    }

    @Override
    public void show(Context context) {
        if (isAdReady()) {

            final boolean isVideoAd = mSpot.getAdContent().isVideoAd();

            InneractiveFullscreenUnitController fullscreenUnitController = new InneractiveFullscreenUnitController();
            fullscreenUnitController.setEventsListener(new InneractiveFullscreenAdEventsListenerWithImpressionData() {
                @Override
                public void onAdImpression(InneractiveAdSpot inneractiveAdSpot, ImpressionData impressionData) {
                    if (mImpressListener != null) {
                        mImpressListener.onInterstitialAdShow(FyberATInterstitialAdapter.this);
                    }

                    if (isVideoAd) {
                        if (mImpressListener != null) {
                            mImpressListener.onInterstitialAdVideoStart(FyberATInterstitialAdapter.this);
                        }
                    }
                }

                @Override
                public void onAdImpression(InneractiveAdSpot inneractiveAdSpot) {

                }

                @Override
                public void onAdClicked(InneractiveAdSpot inneractiveAdSpot) {
                    if (mImpressListener != null) {
                        mImpressListener.onInterstitialAdClicked(FyberATInterstitialAdapter.this);
                    }
                }

                @Override
                public void onAdWillOpenExternalApp(InneractiveAdSpot inneractiveAdSpot) {

                }

                @Override
                public void onAdEnteredErrorState(InneractiveAdSpot inneractiveAdSpot, InneractiveUnitController.AdDisplayError adDisplayError) {
                    if (mImpressListener != null) {
                        mImpressListener.onInterstitialAdVideoError(FyberATInterstitialAdapter.this, ErrorCode.getErrorCode(ErrorCode.rewardedVideoPlayError, "", adDisplayError.getMessage()));
                    }
                }

                @Override
                public void onAdWillCloseInternalBrowser(InneractiveAdSpot inneractiveAdSpot) {

                }

                @Override
                public void onAdDismissed(InneractiveAdSpot inneractiveAdSpot) {
                    if (mImpressListener != null) {
                        mImpressListener.onInterstitialAdClose(FyberATInterstitialAdapter.this);
                    }
                }
            });

            mSpot.addUnitController(fullscreenUnitController);


            InneractiveFullscreenVideoContentController videoContentController = new InneractiveFullscreenVideoContentController();

            // full screen video ad callbacks
            videoContentController.setEventsListener(new VideoContentListener() {

                @Override
                public void onProgress(int totalDurationInMsec, int positionInMsec) {

                }

                @Override
                public void onCompleted() {
                    if (mImpressListener != null) {
                        mImpressListener.onInterstitialAdVideoEnd(FyberATInterstitialAdapter.this);
                    }
                }

                @Override
                public void onPlayerError() {
                    /**
                     * Please note that onPlayerError callback method is deprecated starting from VAMP v7.3.0,
                     and won't be trigged when an error is occurred.
                     * Note: The SDK handles such errors internally and no further action is required.
                     */
                }
            });


            // Now add the content controller to the unit controller
            InneractiveFullscreenUnitController controller = (InneractiveFullscreenUnitController) mSpot.getSelectedUnitController();
            controller.addContentController(videoContentController);

            // showing the ad using the Activity's context
            controller.show(context);
        }
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public boolean isAdReady() {
        // checking if we have ad content
        return mSpot != null && mSpot.isReady();
    }

    @Override
    public String getSDKVersion() {
        return FyberATConst.getNetworkVersion();
    }

    @Override
    public void clean() {
        if (mSpot != null) {
            mSpot.destroy();
            mSpot = null;
        }
    }

    @Override
    public String getNetworkName() {
        return FyberATInitManager.getInstance().getNetworkName();
    }
}
