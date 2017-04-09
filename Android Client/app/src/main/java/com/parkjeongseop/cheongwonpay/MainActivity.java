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
    private TextView balance;
    private ListView listView;
    private ListViewAdapter adapter;
    private DataOutputStream dos;

    public static Handler mHandler;

    public static String CLUB_ID;
    public static String Phone_Num;
    public static final int LOGIN_REQUEST = 100;
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
        balance = (TextView) findViewById(R.id.tv_balance);
        listView = (ListView) findViewById(R.id.listView);

        NetworkThread.prepare();
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                Bundle extra = new Bundle();
                if (msg.what==1) {
                    balance.setText("잔액 : "+msg.obj);
                } else {
                }
            }
        };


        // Adapter 생성
        adapter = new ListViewAdapter();

        // 리스트뷰 참조 및 Adapter달기
        listView.setAdapter(adapter);

        // 첫 번째 아이템 추가.
        adapter.addItem("체험1", "1000원");
        // 두 번째 아이템 추가.
        adapter.addItem("먹거리", "2000원");
        // 세 번째 아이템 추가.
        adapter.addItem("놀기", "300000원");


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                // get item
                ListViewItem item = (ListViewItem) parent.getItemAtPosition(position);

                String titleStr = item.getTitle();
                String descStr = item.getDesc();

                // TODO : use item data.
            }
        });


        /*// Android에서 제공하는 string 문자열 하나를 출력 가능한 layout으로 어댑터 생성
        adapter1 = new ArrayAdapter<String>(getApplicationContext(), android.R.id.item);

        // Xml에서 추가한 ListView 연결
        listView = (ListView) findViewById(R.id.listView);

        // ListView에 어댑터 연결
        listView.setAdapter(adapter1);

        // ListView 아이템 터치 시 이벤트 추가
        //listView.setOnItemClickListener(onClickListItem);

        // ListView에 아이템 추가
        Log.d("ErrorLog", Environment.getExternalStorageDirectory().toString());
*/


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
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    public void onClicked(View view) {
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "ALL");
        startActivityForResult(intent, 0);
    }

    int a;

    public void onClickedAdd(View view) {
//        Intent intent = new Intent(MainActivity.this, GoodsAddActivity.class);
//        startActivity(intent);
        Dialog popup = new Dialog(this);
        popup.setContentView(R.layout.activity_goodsadd);
        popup.show();
        final EditText goods = (EditText)popup.findViewById(R.id.tf_goods);
        final EditText price = (EditText)popup.findViewById(R.id.tf_price);
        popup.findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String temp1 = goods.getText().toString();
                String temp2 = price.getText().toString();
                adapter.addItem(temp1,temp2);
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
//                balance.setText("잔액 : 2000원");
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
    /*
    int price = 0;
    public void onClicked500(){
        int price = price + 500;
    }

    public void onClicked1000(){
        int price = price + 1000;
    }

    public void onClicked1500(){
        int price = price + 1500;
    }

    public void onClicked2000(){
        int price = price + 2000;
    }*/
}

/*class Item{
    String name;
    int price;

    Item(int p, String n){
       name = n;
        price = p;
    }
}*/