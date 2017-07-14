package com.stdioh.messagedemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {

    SmsHandler handler;

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("enter onReceive");
        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (msgs.length > 0) {
            // 读第一条短信
            SmsMessage  msg = msgs[0];
            handler.processNewMsg(msg.getOriginatingAddress(), msg.getDisplayMessageBody());
        }
    }

    public interface SmsHandler {
        void processNewMsg(String phoneNumber, String content);
    }

    public void setSmsHandler(SmsHandler handler) {
        this.handler = handler;
    }
}
