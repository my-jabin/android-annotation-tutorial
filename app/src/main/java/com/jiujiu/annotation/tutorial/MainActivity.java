package com.jiujiu.annotation.tutorial;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jiujiu.annotation.binder_annotation.BindView;
import com.jiujiu.annotation.binder_annotation.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_content)
    TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @OnClick(R.id.bt_left)
    public void click1(View v){
        tvContent.setText("Left button clicked");
    }

    @OnClick(R.id.bt_right)
    public void click2(View v){
        tvContent.setText("Right button clicked");
    }
}
