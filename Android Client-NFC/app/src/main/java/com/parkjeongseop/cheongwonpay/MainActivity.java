package com.parkjeongseop.cheongwonpay;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.zxing.*;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import me.sudar.zxingorient.Barcode;
import me.sudar.zxingorient.ZxingOrient;
import me.sudar.zxingorient.ZxingOrientResult;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import java.util.*;

import android.nfc.*;
import android.nfc.tech.*;
import android.os.*;
import android.provider.*;
import android.app.*;
import android.content.*;
import android.util.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends AppCompatActivity {
    TextView mTextView;
    NfcAdapter mNfcAdapter; // NFC 어댑터
    PendingIntent mPendingIntent; // 수신받은 데이터가 저장된 인텐트
    IntentFilter[] mIntentFilters; // 인텐트 필터
    String[][] mNFCTechLists;

    private TextView result, login_info, balance, Visits, Name;
    private ListView listView;
    private ListViewAdapter adapter;
    private DataOutputStream dos;
    public static Handler mHandler;
    public static String CLUB_ID, Club_Name, User, User_Name;
    private BackPressCloseHandler backPressCloseHandler;
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTextView = (TextView)findViewById(R.id.barcode);
        // NFC 어댑터를 구한다
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // NFC 어댑터가 null 이라면 칩이 존재하지 않는 것으로 간주
        if( mNfcAdapter == null ) {
            mTextView.setText("This phone is not NFC enable.");
            return;
        }

        mTextView.setText("Scan a NFC tag");

        // NFC 데이터 활성화에 필요한 인텐트를 생성
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // NFC 데이터 활성화에 필요한 인텐트 필터를 생성
        IntentFilter iFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            iFilter.addDataType("*/*");
            mIntentFilters = new IntentFilter[] { iFilter };
        } catch (Exception e) {
            mTextView.setText("Make IntentFilter error");
        }
        mNFCTechLists = new String[][] { new String[] { NfcF.class.getName() } };



        // 사용자 인터페이스(UI) 연결.
        setContentView(R.layout.activity_main);
        result = (TextView) findViewById(R.id.barcode);
        login_info = (TextView) findViewById(R.id.tv_login_info);
        login_info.setText(Club_Name);
        balance = (TextView) findViewById(R.id.tv_balance);
        Visits = (TextView) findViewById(R.id.tv_Visits);
        Name = (TextView) findViewById(R.id.tv_Name);
        listView = (ListView) findViewById(R.id.listView);

        // MainAcivity의 Handler
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                Bundle extra = new Bundle();
                switch (msg.what) {// OP-Code에 따라 작동
                    case NetworkThread.OP_RF_BAL:// OP_RF_BAL일 때
                        String temp1[] = msg.obj.toString().split(":");// 핸들러로 받아온 정보를 알맞게 쪼개어 배열에 저장한다.
                        balance.setText("잔액 : " + temp1[0]);// TextView에 잔액표시.
                        Visits.setText("출석체크 : " + temp1[1]);// TextView에 출석체크횟수표시.
                        if(Integer.parseInt(temp1[2])==0){// 출석여부가 X일 때
                            Visits.setTextColor(Color.RED);// 잔액 TextView의 색을 빨간색으로 설정.
                        }else{
                            Visits.setTextColor(Color.BLUE);// 잔액 TextView의 색을 파란색으로 설정.
                        }
                        break;
                    case NetworkThread.OP_GetGoodsList:// OP_GetGoodsList일 때
                        String temp[] = ((String)msg.obj).split(":");// 핸들러로 받아온 정보를 알맞게 쪼개어 배열에 저장한다.
                        adapter.addItem(temp[1], temp[2], temp[0]);// 받아온 정보로 상품목록에 목록을 추가한다.
                        adapter.notifyDataSetChanged();// 리스트뷰 다시 표시하기.
                        break;
                    case NetworkThread.OP_PURCHASE:// OP_PURCHASE일 때
                        if(Integer.parseInt(msg.obj.toString())==NetworkThread.OP_PURCHASE_RS_Success){// 결제 결과가 성공일 때
                            Toast.makeText(MainActivity.this, "결제 성공", Toast.LENGTH_SHORT).show();// 성공 Toast 알림을 띄운다.
                            sendNetworkThread(NetworkThread.OP_RF_BAL, User);// 출석결과를 다시 요청한다.
                        }else{
                            AlertDialog alert = new AlertDialog.Builder( MainActivity.this )// 실패를 Alert창으로 띄운다.
                                    .setIcon( R.mipmap.ic_launcher )
                                    .setTitle( "결제 실패" )// 알림창의 제목
                                    .setMessage( "다시 시도해주시거나 담당자에게 연락바랍니다." )// 알림창의 내용
                                    .setPositiveButton( "확인", new DialogInterface.OnClickListener()// "확인"을 눌렀을 때
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            dialog.dismiss();// 알림창을 닫는다.
                                        }
                                    })
                                    .setNeutralButton( "담당자 전화", new DialogInterface.OnClickListener()// "담당자 전화"를 눌렀을 때
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:010-2077-0204"));// 전화앱으로 연결한다.
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        }
                        break;
                    case NetworkThread.OP_GetName:// OP_GetName일 때
                        User_Name = (String)msg.obj;// 데이터를 User_Name에 저장한다.
                        Name.setText("이름 : " + User_Name);// TextView에 이름을 표시한다.
                        break;
                }
            }
        };

        // Adapter 생성
        adapter = new ListViewAdapter();

        // 리스트뷰 참조 및 Adapter달기
        listView.setAdapter(adapter);

        //상품을 선택했을때 ; 결제
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int goods_Num = Integer.parseInt(((ListViewItem)(adapter.getItem(i))).getItemCode());
                AlertDialog alert = new AlertDialog.Builder( MainActivity.this )// 결제확인창을 Alert창으로 띄운다.
                        .setIcon( R.mipmap.ic_launcher )// 알림창의 아이콘
                        .setTitle( "결제 확인" )// 알림창의 제목
                        .setMessage( User_Name + "의" + ((ListViewItem)(adapter.getItem(i))).getTitle() + "를 결제하시겠습니까?")// 내용에 학생명과 상품명을 표시하여 내용확인을 하도록 한다.
                        .setPositiveButton( "결제", new DialogInterface.OnClickListener()// "결제"를 눌렀을 때
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                sendNetworkThread(NetworkThread.OP_PURCHASE, User + ":" + goods_Num);// 학생과 상품의 고유번호를 결제요청과 함께 전송한다.
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton( "취소", new DialogInterface.OnClickListener()// "취소"를 눌렀을 때
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();// 알림창을 닫는다.
                            }
                        })
                        .show();
            }

        });

        //상품목록에서 상품을 길게 눌렀을때 ; 상품수정, 삭제
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int Goods_Num = Integer.parseInt(((ListViewItem)(adapter.getItem(i))).getItemCode());// 누른 상품의 고유번호를 "Goods_Num"에 정수 데이터타입으로 저장한다.
                // 사용자 인터페이스 정의.
                final Dialog popup = new Dialog(MainActivity.this);
                popup.setContentView(R.layout.activity_goodsadd);
                final EditText goods = (EditText)popup.findViewById(R.id.tf_goods);
                final EditText price = (EditText)popup.findViewById(R.id.tf_price);
                final String Goods_Name = ((ListViewItem)(adapter.getItem(i))).getTitle();
                goods.setText(((ListViewItem)(adapter.getItem(i))).getTitle());
                price.setText(((ListViewItem)(adapter.getItem(i))).getDesc());

                String[] strItems = { "수정", "삭제"};

                AlertDialog alert = new AlertDialog.Builder( MainActivity.this )// "수정", "삭제"를 선택하는 알림창을 띄운다.
                        .setIcon( R.mipmap.ic_launcher )//알림창의 아이콘
                        .setTitle( "상품 수정" )// 알림창의 제목
                        .setSingleChoiceItems(strItems, -1, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                switch ( which )
                                {
                                    case 0:// "수정"을 눌렀을 때
                                        dialog.dismiss();// 알림창을 닫는다.
                                        popup.show();// 상품을 수정하는 팝업창을 띄운다.
                                        popup.findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {// 등록을 눌렀을 때
                                                sendNetworkThread(NetworkThread.OP_EditGoods, Goods_Num + ":" + goods.getText().toString() + ":" + price.getText().toString());// 상품고유번호와 상품명을 수정요청과 함께 전송한다.
                                                Toast.makeText(MainActivity.this,"아이템 수정 완료", Toast.LENGTH_LONG);// 수정완료 Toast 알림을 띄운다.
                                                adapter.clear();// 상품목록을 모두 지운다.
                                                sendNetworkThread(NetworkThread.OP_GetGoodsList);// 상품목록 불러오기 요청을 전송한다.
                                                popup.dismiss();// 팝업창을 닫는다.
                                            }
                                        });
                                        break;
                                    case 1:// "삭제"를 눌렀을 때
                                        dialog.dismiss();// 알림창을 닫는다.
                                        AlertDialog alert = new AlertDialog.Builder( MainActivity.this )// 삭제확인 Alert 알림창을 띄운다,
                                                .setIcon( R.mipmap.ic_launcher )// 알림창의 아이콘
                                                .setTitle( "상품 삭제 확인" )// 알림창의 제목
                                                .setMessage( "정말 '" + Goods_Name + "'을(를) 삭제하시겠습니까?" )// 알림창의 내용
                                                .setPositiveButton( "삭제", new DialogInterface.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which)
                                                    {// "삭제"를 눌렀을 때
                                                        sendNetworkThread(NetworkThread.OP_DeleteGoods, String.valueOf(Goods_Num));// 상품 고유번호를 삭제요청과 함께 전송한다.
                                                        adapter.clear();// 상품목록을 모두 지운다.
                                                        sendNetworkThread(NetworkThread.OP_GetGoodsList);// 상품목록 불러오기 요청을 전송한다.
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
                                        break;
                                }
                            }
                        })
                        .show();

                return false;
            }
        });

        backPressCloseHandler = new BackPressCloseHandler(this);//뒤로버튼으로 종료

        sendNetworkThread(NetworkThread.OP_GetGoodsList);// 상품목록 불러오기 요청을 전송한다.

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // NetworkThread에 Handler를 이용하여 OP-Code와 데이터를 전달한다.
    private void sendNetworkThread(int OP_Code, String Data){
        Message msg = new Message();
        msg.what = OP_Code;
        msg.obj = Data;
        NetworkThread.instance.networkHandler.sendMessage(msg);
    }

    // NetworkThread에 Handler를 이용하여 OP-Code를 전달한다.
    private void sendNetworkThread(int OP_Code){
        Message msg = new Message();
        msg.what = OP_Code;
        NetworkThread.instance.networkHandler.sendMessage(msg);
    }

    // "바코드 인식" 버튼 눌렀을때
    public void onClicked(View view) {
        ZxingOrient integrator = new ZxingOrient(MainActivity.this);// 바코드 인식창 Activity로 이동한다.
        integrator.setIcon(R.mipmap.ic_launcher)// 아이콘
                .setToolbarColor("#32cd32")// 도구바 색상 설정 (청색)
                .setInfoBoxColor("#32cd32")// 정보창 색상 설정 (청색)
                .setInfo("학생증이나 팔찌의 바코드를 인식해주세요.");// 정보창의 알림 내용
        integrator.initiateScan();
    }

    // "부원등록" 버튼을 눌렀을 때
    public void onClickedUserCheck(View view){
        AlertDialog alert = new AlertDialog.Builder( MainActivity.this )// 부원등록 확인 Alert 알림창을 띄운다.
                .setIcon( R.mipmap.ic_launcher )// 알림창의 아이콘
                .setTitle( "부스운영 부원 출석체크 등록" )// 알림창의 제목
                .setMessage( "정말 '" + User_Name + "'을(를) " + Club_Name +"의 동아리 부스운영자로 등록하시겠습니까? 등록하면 출석인정이 됩니다." )// 내용에 학생명과 동아리명을 표시하여 내용확인을 하도록 한다.
                .setPositiveButton( "등록", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {// "등록"을 눌렀을 때
                        sendNetworkThread(NetworkThread.OP_ATD, User);// 부원등록 정보를 보낸다.
                        sendNetworkThread(NetworkThread.OP_RF_BAL, User);// 출석정보를 다시 받아온다.
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

    // 디바이스의 뒤로(Back)버튼 눌렀을때
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }

    // "상품추가" 버튼 눌렀을때
    public void onClickedAdd(View view) {
        // 사용자 인터페이스 연결.
        final Dialog popup = new Dialog(this);
        popup.setContentView(R.layout.activity_goodsadd);
        popup.show();// 상품을 등록하는 팝업창을 띄운다.
        final EditText goods = (EditText)popup.findViewById(R.id.tf_goods);
        final EditText price = (EditText)popup.findViewById(R.id.tf_price);
        popup.findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {// "등록"을 눌렀을 때
                sendNetworkThread(NetworkThread.OP_ADD_ITEM, goods.getText().toString() + ":" + price.getText().toString());// 상품고유번호와 상품명을 등록요청과 함께 전송한다.
                Toast.makeText(MainActivity.this,"아이템 추가 완료", Toast.LENGTH_LONG);// 등록완료 Toast 알림을 띄운다.
                adapter.clear();// 상품목록을 모두 지운다.
                sendNetworkThread(NetworkThread.OP_GetGoodsList);// 상품목록 불러오기 요청을 전송한다.
                popup.dismiss();// 팝업창을 닫는다.
            }
        });
    }

    // 이 Activity로 돌아올 때
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){

        ZxingOrientResult scanResult = ZxingOrient.parseActivityResult(requestCode, resultCode, intent);

        if (scanResult != null && scanResult.getContents() != null) {// "scanResult"가 null이 아니고, "scanResult"의 내용이 null이 아닐 때
            User = scanResult.getContents().toString();// "scanResult"의 내용을 "User"에 문자 데이터타입으로 변환후 저장한다.
            Toast.makeText(this, "바코드 : " + User, Toast.LENGTH_LONG).show();// 바코드정보 Toast 알림을 띄운다.
            result.setText("바코드 : " + User);// TextView에 바코드정보 표시.
            sendNetworkThread(NetworkThread.OP_RF_BAL, User);// 출석결과를 요청한다.
            sendNetworkThread(NetworkThread.OP_GetName, User);// 바코드에 일치하는 학생명을 요청한다.
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.parkjeongseop.cheongwonpay/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.parkjeongseop.cheongwonpay/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}