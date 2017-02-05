package com.yusoxn.openredpacket;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private Intent intent;
    private RadioGroup rgMethod;
    private SharedPreferences redPacketPre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        redPacketPre = getSharedPreferences("REDPACKET", MODE_PRIVATE);

        intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);

        findViewById(R.id.btn_open_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent);
            }
        });

        String text = "○ 抢红包需要您开启辅助服务\n" +
                "○ 点击下面按钮进入辅助功能界面，找到【抢红包服务】\n" +
                "○ 打开【抢红包服务】\n" +
                "○ 辅助服务容易被系统杀掉，可以把本软件加入白名单以延长存活时间\n\n" +
                "注：本软件做了去重功能，防止重复点击同个红包，有两种去重方法：\n" +
                "○ 去重方法一：\n  同一个人发的多个红包有可能只能领取一个，但能自动领取界面中的所有非同一人发的红包\n" +
                "○ 去重方法二：\n  同一个人发的多个非连续的红包可以领取，但每次只能领取界面中的最后发的那一个红包";
        ((TextView) findViewById(R.id.tv_tip_content)).setText(text);

        rgMethod = (RadioGroup) findViewById(R.id.rg_method);
        rgMethod.check(redPacketPre.getBoolean("ISFIRST", true) ? R.id.rg_method_1 : R.id.rg_method_2);
        rgMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rg_method_1) {
                    redPacketPre.edit().putBoolean("ISFIRST", true).apply();
                } else if (checkedId == R.id.rg_method_2) {
                    redPacketPre.edit().putBoolean("ISFIRST", false).apply();
                }
            }
        });

    }
}
