package com.parkjeongseop.cheongwonpay;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import me.sudar.zxingorient.ZxingOrient;
import me.sudar.zxingorient.ZxingOrientResult;

/**
 * Created by 정섭 on 2016-09-22.
 */
public class UserMatchingActivity extends AppCompatActivity {

    String User;
    private BackPressCloseHandler backPressCloseHandler;
    RadioGroup rg;
    TextView barcode;
    EditText SI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usermatching);
        rg = (RadioGroup) findViewById(R.id.radioGroup);
        barcode = (TextView) findViewById(R.id.tv_Barcode);
        SI = (EditText) findViewById(R.id.et_Student_ID);
        backPressCloseHandler = new BackPressCloseHandler(this);

    }

    public void onClickedBar(View view) {
        ZxingOrient integrator = new ZxingOrient(this);
        integrator.setIcon(R.mipmap.ic_launcher).setToolbarColor("#32cd32").setInfoBoxColor("#32cd32").setInfo("팔찌의 바코드를 인식해주세요.");
        integrator.initiateScan();
    }

    public void onClicked(View view) {
        Message msg = new Message();
        msg.what = NetworkThread.OP_UserMatching;
        msg.obj = User + ":" + SI.getText().toString() + ":" + rg.getCheckedRadioButtonId();
        NetworkThread.instance.networkHandler.sendMessage(msg);
    }

    //뒤로버튼 눌렀을때
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){

        ZxingOrientResult scanResult = ZxingOrient.parseActivityResult(requestCode, resultCode, intent);

        if (scanResult != null) {
            User = scanResult.getContents().toString();
            //위의 contents 값에 scan result가 들어온다.
            Toast.makeText(this, User, Toast.LENGTH_LONG).show();
            barcode.setText("바코드 : " + User);
            }
        super.onActivityResult(requestCode, resultCode, intent);
    }

}


