package com.nubesn.subway.android.lockandwakeup;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements ServiceConnection, View.OnClickListener {

    private LockService lockService;
    private PowerManager powerMannger;
    private PowerManager.WakeLock mWakelock;
    private boolean ACQUIRE_FLAG=true;
    private static final int LOCK_REQUEST_CODE=0;
    private String canonicalName;
    private Handler handler=new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (ACQUIRE_FLAG){
                powerMannger = ((PowerManager) MainActivity.this.getSystemService(Context.POWER_SERVICE));
                // 得到一个WakeLock唤醒锁
                mWakelock = powerMannger.newWakeLock(PowerManager.FULL_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.ON_AFTER_RELEASE, canonicalName);
                if (!mWakelock.isHeld()){
                    mWakelock.acquire();
                }
                handler.sendEmptyMessageDelayed(0, 5000);
                Log.d("------------", "acquire");
            }
        }
    };
    private Intent intent;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.acquire).setOnClickListener(this);
        findViewById(R.id.unacquire).setOnClickListener(this);
        findViewById(R.id.release).setOnClickListener(this);
        findViewById(R.id.lock).setOnClickListener(this);
        findViewById(R.id.unlock).setOnClickListener(this);

        canonicalName = getClass().getCanonicalName();
        powerMannger = ((PowerManager) MainActivity.this.getSystemService(Context.POWER_SERVICE));
        // 得到一个WakeLock唤醒锁
        mWakelock = powerMannger.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, getClass().getCanonicalName());
    }

    private void beginService() {
        if (lockService == null) {
            intent=new Intent(MainActivity.this,LockService.class);
            startService(intent);
            bindService(intent, this, 0);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.acquire:
                //自动唤醒屏幕
                ACQUIRE_FLAG=true;
                handler.sendEmptyMessageDelayed(0,5000);
                break;
            case R.id.unacquire:
                //取消自动唤醒屏幕
                ACQUIRE_FLAG=false;
                break;
            case R.id.release:
                //锁屏
                mylock();
                break;
            case R.id.unlock:
                //自定义锁屏
                beginService();
                if (lockService != null) {
                    lockService.registers();
                }
                break;
            case R.id.lock:
                //清除自定义锁屏
                if (lockService != null) {
                    lockService.unregisters();
                }else{
                    Toast.makeText(MainActivity.this, "再点一次", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lockService != null) {
            unbindService(this);
        }
    }

    private void mylock() {
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        name = new ComponentName(this, MyReceiver.class);
        boolean active=devicePolicyManager.isAdminActive(name);
        if (!active){
            //有权限
            // 启动设备管理(隐式Intent) - 在AndroidManifest.xml中设定相应过滤器
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            //权限列表
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, name);
            //描述
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "开启后可以程序锁屏");
            startActivityForResult(intent, LOCK_REQUEST_CODE);
        }else{
            devicePolicyManager.lockNow();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case LOCK_REQUEST_CODE:
                boolean active=devicePolicyManager.isAdminActive(name);
                if(active){
                    devicePolicyManager.lockNow();
                }else{
                    Toast.makeText(MainActivity.this, "未获得权限，请重新授权", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        LockService.MyBinder myBinder = (LockService.MyBinder) service;
        lockService = myBinder.getService();
        lockService.setActivity(MainActivity.this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
