package com.parkjeongseop.cheongwonpay;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Network;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import java.io.*;
import java.net.Socket;

public class LoginActivity extends AppCompatActivity{
    public final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    public static final int LOGIN_OK = 1, LOGIN_FAIL = 2;
    private String id, password;
    private BackPressCloseHandler backPressCloseHandler;
    public static Handler mHandler;
    // 사용자 인터페이스(UI) 정의(references).
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView, mLoginFormView;
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);// 사용자 인터페이스(UI) 연결.

        backPressCloseHandler = new BackPressCloseHandler(this);//뒤로버튼으로 종료

        // LoginActivity의 Handler
        NetworkThread.prepare();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                showProgress(false);

                Bundle extra = new Bundle();

                final Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                if (msg.what == LOGIN_OK) {// 로그인 성공일 때
                    extra.putString("LOGIN_RESULT", mEmailView.getText().toString());
                    intent.putExtras(extra);
                    setResult(RESULT_OK, intent);

                    // 디바이스내에 ID와 암호화된 패스워드를 저장한다.
                    SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putString("ID", id);
                    edit.putString("PW", encryptSHA512(password));
                    edit.commit();
                    if (id.equals("관리자")) {// 관리자계정일 때
                        // 임시학생증 발급 Activity로 이동한다.
                        Intent intent1 = new Intent(getApplicationContext(), UserMatchingActivity.class);
                        startActivity(intent1);
                        finish();
                    }else{
                        if (password.equals("12345678")) {// 기본 설정된 패스워드(12345678)일 경우
                            AlertDialog.Builder alert = new AlertDialog.Builder(LoginActivity.this);
                            alert.setTitle("패스워드 변경");
                            alert.setMessage("기본패스워드입니다. 변경하실 패스워드를 입력해주세요. (패스워드 변경 후 부원과 공유) (5자 이상)");

                            final EditText PW = new EditText(LoginActivity.this);
                            alert.setView(PW);

                            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String InputPW = PW.getText().toString();
                                    if (!TextUtils.isEmpty(InputPW) && !isPasswordValid(InputPW)) {
                                        Toast.makeText(LoginActivity.this, "5자이상으로 설정해주세요", Toast.LENGTH_LONG).show();
                                    }else {
                                        // NetworkThread에 Handler를 이용하여 변경할 패스워드와 함께 패스워드변경요청을 전달한다..
                                        sendNetworkThread(NetworkThread.OP_EditPW, encryptSHA512(InputPW));

                                        // 디바이스내에 ID와 변경한 암호화된 패스워드를 각각 저장한다,
                                        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                                        SharedPreferences.Editor edit = pref.edit();
                                        edit.putString("ID", id);
                                        edit.putString("PW", encryptSHA512(InputPW));
                                        edit.commit();
                                        finish();
                                        startActivity(intent);
                                    }
                                }
                            });

                            alert.show();
                        } else {
                            finish();
                            startActivity(intent);
                        }
                    }
                } else {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                }

            }
        };

        cameraPermission();
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    //       attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (NetworkThread.instance.networkHandler == null) {
                    Log.d("Test", "Trying..");
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                autoLogin();
            }
        }).start();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    //뒤로버튼 눌렀을때
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        backPressCloseHandler.onBackPressed();
    }

    // 카메라 권한요청하기.
    private void cameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {// 카메라권한이 없을 때
            AlertDialog alert = new AlertDialog.Builder(this)// 안드로이드상에서 요청하기전 팝업으로 권한이 필요함을 안내한다.
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle("권한요청")
                    .setMessage("앱을 이용하시려면 카메라권한이 필요합니다. 카메라 권한을 허용해주세요.")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {// 확인을 눌렀을 때
                            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);// 안드로이드상에서 카메라권한을 요청한다.
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("종료", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {// 종료를 눌렀을 때
                            finish();// 앱을 종료한다.
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    cameraPermission();
                }
                return;
            }
        }
    }

    // 자동로그인
    public void autoLogin() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        id = pref.getString("ID", "");// 디바이스내에 저장된 ID값을 앱 내부의 변수"id"에 저장한다.
        password = pref.getString("PW", "");// 디바이스내에 저장된 PW값을 앱 내부의 변수"password"에 저장한다.
        if (id.length() == 0 || password.length() == 0) return;// id이나 password의 길이가 0이면 함수 반환한다.

        // NetworkThread에 Handler를 이용하여 ID,패스워드와 함께 로그인요청을 전달한다.
        sendNetworkThread(NetworkThread.OP_LOGIN, id + ":" + password);
    }

    // NetworkThread에 Handler를 이용하여 OP-Code와 데이터를 전달한다.
    private void sendNetworkThread(int OP_Code, String Data){
        Message msg = new Message();
        msg.what = OP_Code;
        msg.obj = Data;
        NetworkThread.instance.networkHandler.sendMessage(msg);
    }

    // 패스워드를 SHA-512 알고리즘을 이용해 암호화한다.
    public final static String encryptSHA512(String target) {
        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-512");
            sh.update(target.getBytes());
            StringBuffer sb = new StringBuffer();
            for (byte b : sh.digest()) sb.append(Integer.toHexString(0xff & b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // 로그인 시도
    private void attemptLogin() {

        // 로그인field 오류 애니메이션을 null로 초기화한다.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // 로그인시도할 때의 정보들을 저장한다.
        id = mEmailView.getText().toString();
        password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // 유효한 패스워드인지 확인
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {//password가 비어있거나
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // 유효한 ID인지 확인
        if (TextUtils.isEmpty(id)) {// id이 비어있을 때
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {// 로그인 조건에 충족하지 않을 때
            focusView.requestFocus();// 조건에 충족하지 않은 field를 표시한다.
        } else {
            showProgress(true);// 로딩화면을 표시한다.

            // NetworkThread에 Handler를 이용하여 ID,암호화된 패스워드와 함께 로그인요청을 전달한다..
            sendNetworkThread(NetworkThread.OP_LOGIN, id + ":" + encryptSHA512(password));
        }
    }

    // 유효한 패스워드인지 확인
    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Login Page", // TODO: Define a title for the content shown.
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
                "Login Page", // TODO: Define a title for the content shown.
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