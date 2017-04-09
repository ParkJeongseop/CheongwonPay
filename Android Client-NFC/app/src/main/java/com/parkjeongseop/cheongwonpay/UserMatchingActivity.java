package com.parkjeongseop.cheongwonpay;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AlertDialog;
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

    String User, Student_Identification_Number;
    private BackPressCloseHandler backPressCloseHandler;
    RadioGroup rg;// 남고, 여고 선택버튼
    TextView barcode;
    EditText SI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 사용자 인터페이스(UI) 연결.
        setContentView(R.layout.activity_usermatching);
        rg = (RadioGroup) findViewById(R.id.radioGroup);
        barcode = (TextView) findViewById(R.id.tv_Barcode);
        SI = (EditText) findViewById(R.id.et_Student_ID);

        backPressCloseHandler = new BackPressCloseHandler(this);//뒤로버튼으로 종료
    }

    // NetworkThread에 Handler를 이용하여 OP-Code와 데이터를 전달한다.
    private void sendNetworkThread(int OP_Code, String Data){
        Message msg = new Message();
        msg.what = OP_Code;
        msg.obj = Data;
        NetworkThread.instance.networkHandler.sendMessage(msg);
    }

    // "바코드 인식" 버튼 눌렀을때
    public void onClickedBar(View view) {
        ZxingOrient integrator = new ZxingOrient(this);
        integrator.setIcon(R.mipmap.ic_launcher).setToolbarColor("#32cd32").setInfoBoxColor("#32cd32").setInfo("팔찌의 바코드를 인식해주세요.");
        integrator.initiateScan();
    }

    // "등록" 버튼 눌렀을때
    public void onClicked(View view) {
        Student_Identification_Number = SI.getText().toString();
       AlertDialog alert = new AlertDialog.Builder( UserMatchingActivity.this )// 임시학생증 등록 확인 Alert 알림창을 띄운다.
                .setIcon( R.mipmap.ic_launcher )// 알림창의 아이콘
                .setTitle( "임시학생증 등록" )// 알림창의 제목
                .setMessage( "정말 '" + User + "'을(를) " + Student_Identification_Number +"의 임시학생증으로 등록하시겠습니까?" )// 내용에 임시학생증의 바코드와 학번을 표시하여 내용확인을 하도록 한다.
                .setPositiveButton( "등록", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {// "등록"을 눌렀을 때
                        sendNetworkThread(NetworkThread.OP_UserMatching, User + ":" + Student_Identification_Number + ":" + rg.getCheckedRadioButtonId());// 임시학생증의 바코드와 학교구분(남고, 여고)과 학번을 수정요청과 함께 전송한다.
                        Toast.makeText(UserMatchingActivity.this,Student_Identification_Number + "의 임시학생증 등록 완료", Toast.LENGTH_LONG);// 등록완료 Toast 알림을 띄운다.
                        dialog.dismiss();
                    }
                })
                .setNegativeButton( "취소", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {// "취소"를 눌렀을 때
                        dialog.dismiss();// 알림창을 닫는다.
                    }
                })
                .show();
    }

    //뒤로버튼 눌렀을때
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }

    // 이 Activity로 돌아올 때
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){

        ZxingOrientResult scanResult = ZxingOrient.parseActivityResult(requestCode, resultCode, intent);

        if (scanResult != null && scanResult.getContents() != null) {// "scanResult"가 null이 아니고, "scanResult"의 내용이 null이 아닐 때
            User = scanResult.getContents().toString();// "scanResult"의 내용을 "User"에 문자 데이터타입으로 변환후 저장한다.
            Toast.makeText(this, "바코드 : " + User, Toast.LENGTH_LONG).show();// 바코드정보 Toast 알림을 띄운다.
            barcode.setText("바코드 : " + User);// TextView에 바코드정보 표시.
            }
        super.onActivityResult(requestCode, resultCode, intent);
    }

}