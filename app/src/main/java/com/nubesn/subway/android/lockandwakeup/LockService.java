package com.nubesn.subway.android.lockandwakeup;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class LockService extends Service {
    private MyBinder binder;
    private Activity activity;
    private int RECEIVER_TAG=0;

    public LockService() {
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public static String getS(String a){
        return "LockService:"+a;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        if (binder==null){
            binder=new MyBinder();
        }
        return binder;
    }

    class MyBinder extends Binder {
        public LockService getService() {
            return LockService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registers();
        Log.d("-----", "service onCreate");
    }

    public void registers(){
        if (RECEIVER_TAG==0){
            //注册屏幕变亮广播
            IntentFilter onFilter = new IntentFilter("android.intent.action.SCREEN_ON");
            registerReceiver(lockOnReceiver, onFilter);
            //注册屏幕变暗广播
            IntentFilter offFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
            registerReceiver(lockOffReceiver, offFilter);
            RECEIVER_TAG=1;
        }
    }

    public void unregisters(){
        if (RECEIVER_TAG==1){
            unregisterReceiver(lockOnReceiver);
            unregisterReceiver(lockOffReceiver);
            RECEIVER_TAG=0;
        }
        mKeyguardLock.disableKeyguard();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (RECEIVER_TAG==1){
            unregisterReceiver(lockOnReceiver);
            unregisterReceiver(lockOffReceiver);
            RECEIVER_TAG=0;
        }
        Log.d("-----", "service onDestroy");
        mKeyguardLock.reenableKeyguard();
//        startService(new Intent(this, LockService.class));
    }

    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private BroadcastReceiver lockOnReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_ON")){
                Log.d("-------","屏幕变亮");
                mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                mKeyguardLock = mKeyguardManager.newKeyguardLock("");
                mKeyguardLock.disableKeyguard();
                Log.d("-------", "屏幕变亮2");
                Intent intent1=new Intent(LockService.this,LockActivity.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent1);
            }
        }
    };

    private BroadcastReceiver lockOffReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")){
                Log.d("-------","屏幕变暗");
            }
        }
    };
}
