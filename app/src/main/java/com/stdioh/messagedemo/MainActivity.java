package com.stdioh.messagedemo;

import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import ai.olami.android.IRecorderSpeechRecognizerListener;
import ai.olami.android.RecorderSpeechRecognizer;
import ai.olami.cloudService.APIConfiguration;
import ai.olami.cloudService.APIResponse;
import ai.olami.cloudService.TextRecognizer;
import ai.olami.nli.NLIResult;
import ai.olami.nli.Semantic;
import ai.olami.nli.slot.Slot;

import static android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

public class MainActivity extends AppCompatActivity implements SmsReceiver.SmsHandler, IRecorderSpeechRecognizerListener {

    //static final String AID = "595b554684aea6f385319cee";
    static final String KEY = "a4cbc5be360c46bbace648cdc7378bbe";
    static final String SECRET = "bfc8d0d4bbfa463c90821e0379cba52e";

    SpeechSynthesizer tts = null;
    SmsReceiver receiver;
    RecorderSpeechRecognizer recognizer;
    TextRecognizer textRecognizer;
    EditText contentInput;
    boolean recording = false;

    String number;
    String lastMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentInput = (EditText) findViewById(R.id.editText);

        // 注册短信广播接收器
        receiver = new SmsReceiver();
        receiver.setSmsHandler(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(SMS_RECEIVED_ACTION);
        registerReceiver(receiver, filter);

        // 初始化olami服务
        // 创建 APIConfiguration 对象
        APIConfiguration config = new APIConfiguration(KEY, SECRET, APIConfiguration.LOCALIZE_OPTION_SIMPLIFIED_CHINESE);
        recognizer = RecorderSpeechRecognizer.create(this, config);
        recognizer.setLengthOfVADEnd(2000);
        textRecognizer = new TextRecognizer(config);
        textRecognizer.setEndUserIdentifier("Someone");
        textRecognizer.setTimeout(1000);

        // 初始化讯飞服务。APPID注册讯飞平台，创建应用即可获得
        SpeechUtility uti = SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID + "=595da10d");

        if (uti == null) {
            System.out.println("create Utility failed. ");
        }

        tts = SpeechSynthesizer.createSynthesizer(getApplicationContext(), new InitListener() {
            @Override
            public void onInit(int i) {
                System.out.println("tts初始化完成");
                tts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
                tts.setParameter(SpeechConstant.ENGINE_MODE, SpeechConstant.MODE_AUTO);
                tts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
                tts.setParameter(SpeechConstant.SPEED, "40");
            }
        });
    }

    public void onButtonClick(View view) {
        final String content = contentInput.getText().toString();
        if (!content.isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        APIResponse response = textRecognizer.requestNLI(content);
                        onRecognizeResultChange(response);
                    } catch (NoSuchAlgorithmException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void onButton2Click(View view) {
        RecorderSpeechRecognizer.RecordState recordState = recognizer.getRecordState();

        // Check to see if we should start recording or stop manually.
        if (recordState == RecorderSpeechRecognizer.RecordState.STOPPED) {
            try {

                // * Request to start voice recording and recognition.
                recognizer.start();

            } catch (InterruptedException e) {

                e.printStackTrace();

            }

        } else if (recordState == RecorderSpeechRecognizer.RecordState.RECORDING) {

            // * Request to stop voice recording when manually stop,
            //   and then wait for the final recognition result.
            recognizer.stop();

        }
    }

    public void replyMsg(String content) {
        if (number != null) {
            SmsManager.getDefault().sendTextMessage(number, null, content, null, null);
            speak("好的");
        } else {
            speak("我还不知道要发给谁。");
        }
    }

    public void speak(String content) {
        tts.startSpeaking(content, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {

            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {

            }

            @Override
            public void onSpeakPaused() {

            }

            @Override
            public void onSpeakResumed() {

            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {

            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });
        //tts.speak(content, QUEUE_ADD, null, content);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        recognizer.release();
        tts.destroy();
        super.onDestroy();
    }

    private void repeatMsg() {
        if (lastMsg != null) {
            speak(lastMsg);
        } else {
            speak("我不知道你想听的是哪条短信。");
        }
    }

    @Override
    public void onRecordStateChange(RecorderSpeechRecognizer.RecordState state) {
        if (state == RecorderSpeechRecognizer.RecordState.RECORDING) {
            recording = true;
        } else if (state == RecorderSpeechRecognizer.RecordState.STOPPED) {
            recording = false;
        }
        System.out.println("record state: " + state.name());
    }

    @Override
    public void onRecognizeStateChange(RecorderSpeechRecognizer.RecognizeState state) {
        System.out.println("recognize state: " + state.name());
    }

    @Override
    public void onRecognizeResultChange(APIResponse response) {
        if (response.ok() && response.hasData()) {
            // 提取语音转文字识别结果
            //SpeechResult sttResult = response.getData().getSpeechResult();
            // 提取 NLI 语义或 IDS 数据
            if (response.getData().hasNLIResults()) {
                NLIResult[] nliResults = response.getData().getNLIResults();
                for (NLIResult result : nliResults) {
                    if (result.getSemantics() != null && result.getSemantics().length > 0) {
                        for (Semantic semantic : result.getSemantics()) {
                            if (semantic.getAppModule().equals("sms")) {
                                switch (semantic.getGlobalModifiers()[0]) {
                                    case "reply": {
                                        for (Slot slot : semantic.getSlots()) {
                                            if (slot.getName().equals("content")) {
                                                replyMsg(slot.getValue());
                                                return;
                                            }
                                        }
                                    }
                                    break;
                                    case "repeat": {
                                        repeatMsg();
                                    }
                                    default:
                                        break;
                                }
                            }
                        }
                    } else {
                        speak(result.getDescObject().getReplyAnswer());
                    }
                }
            }
        }
    }

    @Override
    public void onRecordVolumeChange(int volumeValue) {

    }

    @Override
    public void onServerError(APIResponse response) {

    }

    @Override
    public void onError(RecorderSpeechRecognizer.Error error) {

    }

    @Override
    public void onException(Exception e) {

    }

    @Override
    public void processNewMsg(String phoneNumber, String content) {
        StringBuilder finalAddress = new StringBuilder();

        // 播报号码中加入空格，以免读成数量
        for (int i = 0; i < phoneNumber.length(); i ++) {
            finalAddress.append(phoneNumber.charAt(i));
            finalAddress.append(' ');
        }
        String tts = String.format("收到来自%s的短信，内容是%s，你想回复什么？", finalAddress, content);

        speak(tts);

        // 记录短信内容
        number = phoneNumber;
        lastMsg = content;

        // 打开录音
        try {
            recognizer.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
