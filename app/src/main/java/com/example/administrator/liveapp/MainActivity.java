package com.example.administrator.liveapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private PaintView paintView;
    private Context mContext;
    private int userId = 0;

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private RtcEngine mRtcEngine;// Tutorial Step 1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        // 初始化语音直播sdk
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            initAgoraEngineAndJoinChannel();
        }
    }

    private void initView() {
        mContext = this;
        paintView = findViewById(R.id.paintView);

        Button btnBack = findViewById(R.id.btn_live_back);
        Button btnClear = findViewById(R.id.btn_live_clear);
        Button btnEsc = findViewById(R.id.btn_live_esc);
        btnBack.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnEsc.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            // 撤销
            case R.id.btn_live_back:
                paintView.undo();
                break;

            // 清除
            case R.id.btn_live_clear:
                paintView.clear();
                break;

            // 离开
            case R.id.btn_live_esc:
                finish();
                break;
        }
    }


    private IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1

        @Override
        public void onApiCallExecuted(int error, String api, String result) {
            Log.e("测试", "onApiCallExecuted(),错误码：" + error + "，api：" + api + " ,调用结果：" + result);
        }

        /**
         * 加入频道回调
         * @param channel 频道名
         * @param uid 用户 ID
         * @param elapsed 从 joinChannel() 开始到该事件产生的延迟（毫秒)
         */
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.e("测试", "自动创建用户id,channel:" + channel + "， uid:" + uid + " ，elapsed" + elapsed);
        }

        /**
         * 连接中断回调
         * 该回调方法表示 SDK 和服务器失去了网络连接
         * 与 onConnectionLost 回调的区别是: onConnectionInterrupted 回调在 SDK 刚失去和服务器连接时触发，
         * onConnectionLost 在失去连接且尝试自动重连失败后才触发。
         * 失去连接后，除非 APP 主动调用 leaveChannel()，不然 SDK 会一直自动重连。
         */
        @Override
        public void onConnectionInterrupted() {
            Log.e("测试", "onConnectionInterrupted(),连接中断");
        }

        @Override
        public void onConnectionLost() {
            Log.e("测试", "onConnectionLost(),自动重连");
        }

        /**
         * 连接已被禁止回调
         */
        @Override
        public void onConnectionBanned() {
            Log.e("测试", "onConnectionBanned(),连接已被禁止回调");
        }

        /**
         * 接收到对方数据流消息的回调
         * @param uid 用户 ID
         * @param streamId 数据流 ID
         * @param data 接收到的数据
         */
        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            String str = new String(data);
            Log.e("测试", "onStreamMessage(),接收到对方数据流消息的回调,uid:" + uid + "，streamId:" + streamId + ",data:" + str);

        }

        /**
         * 接收对方数据流消息错误的回调
         * @param uid 用户 ID
         * @param streamId 数据流 ID
         * @param error 错误码
         * @param missed 丢失的消息数量
         * @param cached 数据流中断时，后面缓存的消息数量
         */
        @Override
        public void onStreamMessageError(int uid, int streamId, int error, int missed, int cached) {
            Log.e("测试", "onStreamMessageError(),接收对方数据流消息错误的回调");
        }

        /**
         * 其他用户离开当前频道回调
         * @param uid
         * @param reason
         */
        @Override
        public void onUserOffline(final int uid, final int reason) { // Tutorial Step 4
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        @Override
        public void onRtcStats(RtcStats stats) {
            Log.e("测试", "onRtcStats()");
        }

    };

    /**
     * 检查权限
     *
     * @param permission
     * @param requestCode
     * @return
     */
    public boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel();
                } else {
//                    ToastUtils.showLongToast(mContext, "你没有授权录音权限");
                    finish();
                }
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 离开频道
        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
        mRtcEventHandler = null;
    }

    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();     // Tutorial Step 1
        joinChannel();               // Tutorial Step 2
    }

    // 第一步：初始化SDK,申请AppId
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), "890962ef28484982901d39ddf611c48c", mRtcEventHandler);
            //设置频道模式为直播
            // 直播模式有主播和观众两种角色，可以通过调用 setClientRole 设置。主播可收发语音和视频，但观众只能收，不能发
            //CHANNEL_PROFILE_COMMUNICATION = 0: 通信模式 (默认)
            //CHANNEL_PROFILE_LIVE_BROADCASTING = 1: 直播模式
            //CHANNEL_PROFILE_GAME = 2: 游戏语音模式
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            mRtcEngine.setSpeakerphoneVolume(255);
            mRtcEngine.enableAudio();
            // 该方法用于设置音频参数和应用场景。设置音质
            mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT,Constants.AUDIO_SCENARIO_EDUCATION);
            // 该方法改变本地说话人声音的音调 语音频率可以 [0.5, 2.0] 范围内设置。默认值为 1.0
            mRtcEngine.setLocalVoicePitch(1.0);
            // 打开外放
            mRtcEngine.setEnableSpeakerphone(true);
            // 建立数据通道
            // 参数一：True：接收方 5 秒内会收到发送方所发送的数据，否则连接中断，数据通道会向应用程序报错。 False：接收方不保证收到，就算数据丢失也不会报错。
            // 参数二：True：接收方 5 秒内会按照发送方发送的顺序收到数据包。False：接收方不保证按照发送方发送的顺序收到数据包。
            mRtcEngine.createDataStream(true,true);

            // 设置用户角色为主播（切换主播可连麦）
//            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

        } catch (Exception e) {
            throw new RuntimeException("声网初始化失败 error\n" + Log.getStackTraceString(e));
        }
    }

    // 第二步：加入频道
    private void joinChannel() {
        // 参数一：token 安全要求不高: 将值设为 null
        // 参数二：标识通话的频道名称，长度在64字节以内的字符串。以下为支持的字符集范围（共89个字符）: a-z,A-Z,0-9,space,! #$%&,()+, -,:;<=.#$%&,()+,-,:;<=.,>?@[],^_,{|},~
        // 参数三：(非必选项) 开发者需加入的任何附加信息。一般可设置为空字符串，或频道相关信息。该信息不会传递给频道内的其他用户
        // 参数四：用户 ID，32 位无符号整数 建议设置范围：1 到 (2^32-1)，并保证唯一性。如果不指定（即设为0），SDK 会自动分配一个，并在 onJoinChannelSuccess 回调方法中返回，App 层必须记住该返回值并维护，SDK 不对该返回值进行维护
        mRtcEngine.joinChannel(null, "test", null, 7); // if you do not specify the uid, we will generate the uid for you
    }

    // Tutorial Step 3
    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

}
