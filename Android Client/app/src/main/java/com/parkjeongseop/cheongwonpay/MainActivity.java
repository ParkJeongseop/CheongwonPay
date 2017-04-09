package com.parkjeongseop.cheongwonpay;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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

public class MainActivity extends AppCompatActivity {

    private TextView result;
    private TextView login_info;
    private TextView balance;
    private ListView listView;
    private ListViewAdapter adapter;
    private DataOutputStream dos;

    public static Handler mHandler;

    public static String CLUB_ID;
    public static String Club_Name;
    public static String Phone_Num;
    public static String User;
    public static final int LOGIN_REQUEST = 100;
    private BackPressCloseHandler backPressCloseHandler;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = (TextView) findViewById(R.id.barcode);
        login_info = (TextView) findViewById(R.id.tv_login_info);
        login_info.setText(Club_Name);
        balance = (TextView) findViewById(R.id.tv_balance);
        listView = (ListView) findViewById(R.id.listView);

        NetworkThread.prepare();
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                Bundle extra = new Bundle();
                switch (msg.what) {
                    case NetworkThread.OP_RF_BAL:
                        Log.d("Test", "Balance");
                        balance.setText("잔액 : "+msg.obj);
                        break;
                    case NetworkThread.OP_GetGoodsList:
                        String temp[] = ((String)msg.obj).split(":");
                        Log.d("Test", "op_getgoodslist");
                        adapter.addItem(temp[1], temp[2], temp[0]);
                        adapter.notifyDataSetChanged();
                        break;
                    case NetworkThread.OP_PURCHASE:
                        if((int)msg.obj==105){
                            Toast.makeText(MainActivity.this, "Purchase Success", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(MainActivity.this, "Purchase Failed Try again", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };


        // Adapter 생성
        adapter = new ListViewAdapter();

        // 리스트뷰 참조 및 Adapter달기
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int goods_Num = Integer.parseInt(((ListViewItem)(adapter.getItem(i))).getItemCode());
                Log.d("Test","goodsnum : " + goods_Num);
                Message msgp = new Message();
                msgp.what = NetworkThread.OP_PURCHASE;
                msgp.obj = User + ":" + goods_Num;
                NetworkThread.instance.networkHandler.sendMessage(msgp);

            }

        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int goods_Num = Integer.parseInt(((ListViewItem)(adapter.getItem(i))).getItemCode());
                final Dialog popup = new Dialog(MainActivity.this);
                popup.setContentView(R.layout.activity_goodsadd);
                popup.show();
                final EditText goods = (EditText)popup.findViewById(R.id.tf_goods);
                final EditText price = (EditText)popup.findViewById(R.id.tf_price);
                goods.setText(((ListViewItem)(adapter.getItem(i))).getTitle());
                price.setText(((ListViewItem)(adapter.getItem(i))).getDesc());
                popup.findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Message msg = new Message();
                        msg.what = NetworkThread.OP_EditGoods;
                        msg.obj = goods_Num + ":" + goods.getText().toString() + ":" + price.getText().toString();
                        NetworkThread.instance.networkHandler.sendMessage(msg);
                        Toast.makeText(MainActivity.this,"아이템 수정 완료", Toast.LENGTH_LONG);

                        adapter.clear();
                        Message msgg = new Message();
                        msgg.what = NetworkThread.OP_GetGoodsList;
                        NetworkThread.instance.networkHandler.sendMessage(msgg);
                        popup.dismiss();
                    }
                });
                return false;
            }
        });

        backPressCloseHandler = new BackPressCloseHandler(this);//뒤로버튼으로 종료

        Message msg = new Message();
        msg.what = NetworkThread.OP_GetGoodsList;
        NetworkThread.instance.networkHandler.sendMessage(msg);

        File datafile = new File(Environment.getExternalStorageDirectory().toString() + "/datafile.txt");
        if (!datafile.exists()) {
            try {
                datafile.createNewFile();
                Toast.makeText(getApplicationContext(), "New File Created", Toast.LENGTH_LONG);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "New File Error", Toast.LENGTH_LONG);
                Log.d("ErrorLog", "Error1 : " + e.getMessage());
            }
        }

        try {
            dos = new DataOutputStream(new FileOutputStream(datafile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d("ErrorLog", "Error2 : " + e.getMessage());
        }


        /*try {
            dos.writeUTF(adapter1.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
//
//        Intent loginActivity = new Intent( this, LoginActivity.class);
//        startActivityForResult(loginActivity, LOGIN_REQUEST);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    //Start버튼 눌렀을때
    public void onClicked(View view) {
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "ALL");
        startActivityForResult(intent, 0);
    }

    //뒤로버튼 눌렀을때
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }

    //ADD버튼 눌렀을때
    public void onClickedAdd(View view) {
        final Dialog popup = new Dialog(this);
        popup.setContentView(R.layout.activity_goodsadd);
        popup.show();
        final EditText goods = (EditText)popup.findViewById(R.id.tf_goods);
        final EditText price = (EditText)popup.findViewById(R.id.tf_price);
        popup.findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Message msg = new Message();
                msg.what = NetworkThread.OP_ADD_ITEM;
                msg.obj = goods.getText().toString() + ":" + price.getText().toString();
                NetworkThread.instance.networkHandler.sendMessage(msg);
                Toast.makeText(MainActivity.this,"아이템 추가 완료", Toast.LENGTH_LONG);

                adapter.clear();
                Message msgg = new Message();
                msgg.what = NetworkThread.OP_GetGoodsList;
                NetworkThread.instance.networkHandler.sendMessage(msgg);
                popup.dismiss();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 0) {

            if (resultCode == Activity.RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                //위의 contents 값에 scan result가 들어온다.
                Toast.makeText(this, contents, Toast.LENGTH_LONG).show();
                result.setText("바코드 : " + contents);
                User = contents;
                Message msg = new Message();
                msg.what = NetworkThread.OP_RF_BAL;
                msg.obj = contents;
                NetworkThread.instance.networkHandler.sendMessage(msg);
            }

        } else if (requestCode == LOGIN_REQUEST && resultCode == Activity.RESULT_OK) {
            String result = data.getStringExtra("LOGIN_RESULT");
            ((TextView) findViewById(R.id.tv_login_info)).setText(result);
        }

        super.onActivityResult(requestCode, resultCode, data);
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