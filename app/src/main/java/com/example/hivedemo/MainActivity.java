package com.example.hivedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.ContentInspectConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class MainActivity extends AppCompatActivity {

    //ToDo: put your agora appId here
    // Fill the App ID of your project generated on Agora Console.
    private String appId = "";
    // Fill the channel name.
    private String channelName = "HiveTest";
    // Fill the temp token generated on Agora Console.
    private String token = "";

    private RtcEngine mRtcEngine;

    private Button switchCameraBtn;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        // Listen for the remote host joining the channel to get the uid of the host.
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Call setupRemoteVideo to set the remote video view after getting uid from the onUserJoined callback.
                    setupRemoteVideo(uid);
                }
            });
        }
    };

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            initializeAndJoinChannel();
        }

        switchCameraBtn = findViewById(R.id.switch_camera_button);
        switchCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRtcEngine.switchCamera();
            }
        });
    }

    private void initializeAndJoinChannel() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = this;
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Check the error.");
        }
        // By default, video is disabled, and you need to call enableVideo to start a video stream.
        mRtcEngine.enableVideo();
        // Start local preview.
        mRtcEngine.startPreview();
        mRtcEngine.setLogLevel(1);

//        ContentInspectConfig config = new ContentInspectConfig();
//        config.extraInfo = "try Hive";
//        config.moduleCount = 1;
//        config.ContentWorkType = ContentInspectConfig.CONTENT_INSPECT_TYPE_WORK_DEVICE_CLOUD;
//        config.ContentWorkType = ContentInspectConfig.CONTENT_INSPECT_DEVICE_AGORA;
//        config.modules[0].type = ContentInspectConfig.CONTENT_INSPECT_TYPE_MODERATION; // Be sure to set type as MODERATION
//        config.modules[0].vendor = ContentInspectConfig.CONTENT_INSPECT_VENDOR_HIVE; // Set Hive as the content moderation service provider
//        //ToDo: change the callback url to your url
//        config.modules[0].callbackUrl = "https://webhook.site/64e02dc6-37ef-4ec9-9919-52eab79c2e91"; // Add the URL to receive the callbacks sent from Hive
//        config.modules[0].frequency = 2;   // Set content moderation to run every 2 seconds
//        mRtcEngine.enableContentInspect(true, config);

        ContentInspectConfig config = new ContentInspectConfig();
        config.extraInfo = "try Hive";
        config.moduleCount = 1;
        config.ContentWorkType = ContentInspectConfig.CONTENT_INSPECT_TYPE_WORK_DEVICE_CLOUD;
        //config.DeviceWorkType = ContentInspectConfig.CONTENT_INSPECT_DEVICE_AGORA;
        config.modules[0].type = ContentInspectConfig.CONTENT_INSPECT_TYPE_MODERATION; // Be sure to set type as MODERATION
        config.modules[0].vendor = ContentInspectConfig.CONTENT_INSPECT_VENDOR_HIVE; // Set Hive as the content moderation service provider
        config.modules[0].callbackUrl = "https://webhook.site/64e02dc6-37ef-4ec9-9919-52eab79c2e91"; // Add the URL to receive the callbacks sent from Hive
        config.modules[0].frequency = 2;   // Set content moderation to run every 2 seconds
        mRtcEngine.enableContentInspect(true, config);

        FrameLayout container = findViewById(R.id.local_video_view_container);
        // Ceate a SurfaceView object and add it as a child to the FrameLayout.
        SurfaceView surfaceView = new SurfaceView (getBaseContext());
        container.addView(surfaceView);
        // Pass the SurfaceView object to Agora so that it renders the local video.
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0));

        ChannelMediaOptions options = new ChannelMediaOptions();
        // Set both clients as the BROADCASTER.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        // For a video call scenario, set the channel profile as BROADCASTING.
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;

        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        mRtcEngine.joinChannel(token, channelName, 0, options);
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        SurfaceView surfaceView = new SurfaceView (getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRtcEngine.stopPreview();
        mRtcEngine.leaveChannel();
        RtcEngine.destroy();
    }
}