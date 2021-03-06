package com.example.hc06project;

import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.appcompat.widget.SwitchCompat;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_ENABLE_BT = 10;
    BluetoothAdapter mBluetoothAdapter;
    int mPairedDeviceCount = 0;
    Set<BluetoothDevice> mDevices;
    BluetoothDevice mRemoteDevice;
    BluetoothSocket mSocket = null;
    OutputStream mOutputStream = null;
    InputStream mInputStream = null;

    Thread mWorkerThread = null;
    String mStrDelimiter = "\n";
    char mCharDelimiter = '\n';
    byte[] readBuffer;
    int readBufferPosition;


    AppCompatToggleButton btn_bluetooth;
    SwitchCompat switch_auto, switch_control;                   // @SEO 자동 제어스위치, 창문제어 스위치 제어를 위한 변수할당
    TextView txt_isWindow, txt_gasValue, txt_rainValue;         // @SEO 창문상태, 가스센서값, 빗물 센서값을 제어를 위한 변수할당
    ProgressDialog progressDialog;                              // @SEO 로딩바 변수

    /** @SEO
     * [1] onCraete 함수
     * - 어플리케이션이 처음 켜질때 읽는 함수입니다.
     * 현재 하고 있는 기능
     *  1. 전송버튼(sendButton)에 대한 아이디할당
     *  2. 전송버튼에 이벤트(ButtonClickListener) 장착
     *  3. checkBluetooth 함수 호출
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_bluetooth = findViewById(R.id.btn_bluetooth);               // @SEO 블루투스 버튼아이디 할당
        switch_auto = findViewById(R.id.switch_auto);                   // @SEO 자동스위치 아이디 할당
//        switch_control = findViewById(R.id.switch_control);              // @SEO 창문제어스위치 아이디 할당
        txt_isWindow = findViewById(R.id.txt_isWindow);                 // @SEO 창문상태 텍스트뷰 아이디할당
        txt_gasValue = findViewById(R.id.txt_gasValue);                 // @SEO 가스값 텍스트뷰 아이디할당
        txt_rainValue = findViewById(R.id.txt_rainValue);               // @SEO 빗물 텍스트뷰 아이디할당

//        btn_bluetooth.setOnClickListener(new ButtonClickListener());         // @SEO 블루투스 리스너이벤트 장착
        switch_auto.setOnCheckedChangeListener(new SwitchClickListener());
//        switch_control.setOnCheckedChangeListener(new SwitchClickListener());

        progressDialog = new ProgressDialog(this); // @SEO 창문제어할때 빙빙도는 ui 생성



        // 블루투스 이미지변경
//        if(mRemoteDevice == null) btn_bluetooth.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth_enable));

    }

    class ButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            if(mRemoteDevice == null) { // 연결이 안되어있으면 블루투스검색
                checkBluetooth();
            }
            else{ // 연결이 되어있으면 연결끊기
                try{
                    mWorkerThread.interrupt();
                    mInputStream.close();
                    mOutputStream.close();
                    mSocket.close();
                    mRemoteDevice = null;
                    Toast.makeText(MainActivity.this,"연결을 해제하였습니다.",Toast.LENGTH_SHORT).show();

                    // 블루투스 이미지변경
//                    btn_bluetooth.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth_enable));
                }catch(Exception e){}

            }
        }
    }

    class SwitchClickListener implements CompoundButton.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()){
                case R.id.switch_auto :
                    Log.i("seo","auto status : " + b);
                    if(b == true)  sendData("1");
                    else sendData("2");
                    break;
//                case R.id.switch_control :
//                    progressDialog.show();
//                    Log.i("seo","control status : " + b);
//                    if(b == true)  sendData("F");
//                    else sendData("0");
//
//                    break;
            }
        }
    }

    /**
     * [2]
     *  checkBluetooth() 설명: 휴대폰이 블루투스가 활성화 되어있는지를 검사하고
     *  활성화 되어있는경우 : 블루투스모듈을 검색
     *  활성화 안되어있는경우 : 사용자 동의 요청 창이 출력
     *  휴대폰이 블루투스를 지원하지 않을경우 : 어플레케이션 종료를 합니다.
      */
    void checkBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){ // 장치가 블루투스를 지원하지 않는 경우

            finish(); // 어플리케이션 종료
        }
        else {
            // 장치가 블루투스를 지원하는 경우
            if (!mBluetoothAdapter.isEnabled()) {
                // 블루투스를 지원하지만 비활성 상태인 경우
                // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요청
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                // 현재 java 파일의 onActivityResult 호출
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else {
                // 블루투스를 지원하며 활성 상태인 경우
                // 페어링 된 기기 목록을 보여주고 연결할 장치를 선택
                selectDevice();
            }
        }
    }

    /**
     * [3]
     *  selectDevice() 설명: 블루투스 모듈을 검색합니다.
     *  AlertDialog 라는 창을 열어서, 그안에 제목과 검색된 블루투스 모듈, 취소버튼을 추가합니다.
     *  또한 연결한 블루투스 모듈을 클릭하면, connectToSelectedDevice()를 호출해서 연결을 시도합니다.
     */
    void selectDevice(){
        mDevices = mBluetoothAdapter.getBondedDevices();
        mPairedDeviceCount = mDevices.size();

        if(mPairedDeviceCount == 0){
            // 페어링 된 장치가 없는 경우
            finish(); // 어플리케이션 종료
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 장치 선택");

        // 페어링 된 블루투스 장치의 이름 목록 작성
        List<String> listItems = new ArrayList<String>();
        for (BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
            Log.i("seo","device.getName() : " + device.getName());
        }
        listItems.add("취소"); // 취소 항목 추가

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int item){
                if(item == mPairedDeviceCount){
                    // 연결할 장치를 선택하지 않고 ‘취소’를 누른 경우
//                    finish();
                }
                else{
                    // 연결할 장치를 선택한 경우
                    // 선택한 장치와 연결을 시도함
                    connectToSelectedDevice(items[item].toString());
                }
            }
        });

        builder.setCancelable(false);// 뒤로 가기 버튼 사용 금지
        AlertDialog alert = builder.create();
        alert.show();
    }


    /**
     * [4]
     *  connectToSelectedDevice() 설명: 블루투스 연결을 시도.
     *  selectDevice()에서 받은 디바이스의 정보를 가지고 연결을 시도합니다.
     *  연결에 성공할경우 beginListenForData()를 호출하고,
     *  연결에 실패할경우 토스트알림을 통해 연결을 실패했다고 알려줍니다.
     */
    void connectToSelectedDevice(String selectedDeviceName){
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try{
            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();

            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();

            beginListenForData();

            // 블루투스 이미지변경
//            btn_bluetooth.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth_disable));

        }catch (Exception e){
            Toast.makeText(MainActivity.this,"연결을 실패했습니다.",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * [4]c
     *  beginListenForData() 설명
     *   Thread를 통해서 연결을 통해 수신받은 데이터를 처리합니다.
     *   Thread를 간단히 설명하면, 사람으로 따지면 "두뇌"라고 보시면 될거같습니다.
     *   하나의 Thread(두뇌)를 추가로 만들어서 그 Thread(두뇌)는 그 정해져있는 일만 수행합니다.(앱을 종료하거나, 스레드가 종료될때까지)
     *
     *   현재코드는 하나의 Thread를 생성해서 , 앱이 종료될때까지 아두이노 블루투스모듈에서 들어온 데이터를 처리합니다.
     */
    void beginListenForData() {
        final Handler handler = new Handler();

        readBuffer = new byte[1024]; // 수신 버퍼
        readBufferPosition = 0; // 버퍼 내 수신 문자 저장 위치
        // 문자열 수신 쓰레드
        mWorkerThread = new Thread(new Runnable(){
            public void run(){
                while(!Thread.currentThread().isInterrupted()){
                    try {
                        // @SEO 로딩 UI가 돌고 있으면 취소하기

                        int bytesAvailable = mInputStream.available(); // 수신 데이터 확인
                        if(bytesAvailable > 0){ // 데이터가 수신된 경우
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for(int i = 0; i < bytesAvailable; i++){
                                byte b = packetBytes[i];
                                if(b == mCharDelimiter){
                                    progressDialog.dismiss();
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0,
                                            encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable(){
                                        // 아두이노로부터 들어온 수신데이터
                                        public void run(){
                                            String result[] = data.split("/"); // @SEO   1. GAS/123 혹은 RAIN/123.. 이런식으로 들어오는것중 / 를 기준으로 잘라 배열로 저장합니다.
                                            String sensor = result[0];                //        2. 그중에 앞에것만 잘라서 따로저장합니다. ex) GAS , RAIN => 아두이노로부터 받은 센서의 종류를 뜻합니다
                                            String value = result[1];                 //        3. 그중에 뒤에것만 잘라서 따로 저장합니다. ex) 123, 123 => 아두이노로부터 받은 센서의 값을 뜻합니다.
                                            switch (sensor){
                                                case "RAIN": // @SEO 빗물감지값
                                                    txt_rainValue.setText(value);
                                                    break;
                                                case "GAS" : // @SEO 가스값
                                                    txt_gasValue.setText(value);
                                                    break;
                                                case "WINDOW" : // @SEO 창문제어 상태값
                                                    Log.i("seo","data : " + data);
                                                    if( data.indexOf("1") > -1){ // 문장에 1이 포함되어있다면 ex WINDOW/1
                                                        txt_isWindow.setText("열림");
//                                                        switch_control.setChecked(true);
                                                    }                            // 문장에 1이 포함되어 있지않다면 ex WINDOW/0
                                                    else { // 문장에 0이 포함되어있다면
                                                        txt_isWindow.setText("닫힘");
//                                                        switch_control.setChecked(false);
                                                    }
                                                    break;
                                                case "AUTO" : // @SEO 자동제어 상태값
                                                    Log.i("seo","data : " + data);
                                                    if( data.indexOf("1") > -1){ // 문장에 1이 포함되어있다면 ex AUTO/1
                                                        switch_auto.setChecked(true);
                                                    }
                                                else {                          // 문장에 0이 포함되어있다면 ex AUTO/0
                                                        switch_auto.setChecked(false);
                                                    }
                                                    break;
                                            }
                                        }
                                    });
                                }
                                else{
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex){
                        // 데이터 수신 중 오류 발생
                        finish();
                    }
                }
            }
        });
        mWorkerThread.start();
    }

    /**
     * 함수 1 sendData() : 현재 연결된 블루투스에 MSG를 전송합니다.
     */
    void sendData(String msg){
        msg += mStrDelimiter;
        // 문자열 종료 표시
        try{
            mOutputStream.write(msg.getBytes()); // 문자열 전송
        }catch(Exception e){
            Toast.makeText(MainActivity.this,"문자열전송을 실패했습니다.",Toast.LENGTH_SHORT).show();
        }
    }


    BluetoothDevice getDeviceFromBondedList(String name){
        BluetoothDevice selectedDevice = null;
        for (BluetoothDevice device : mDevices) {
            if(name.equals(device.getName())){
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    /**
     * 앱이종료될때 호출됨.
     * 현재코드에서는 앱이 종료될때 Thread를 종료합니다.
     */
    @Override
    protected void onDestroy() {
        try{
            mWorkerThread.interrupt();// 데이터 수신 쓰레드 종료
            mInputStream.close();
            mOutputStream.close();
            mSocket.close();
        }catch(Exception e){}

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK){
                    // 블루투스가 활성 상태로 변경됨
                    selectDevice();
                }
                else if(resultCode == RESULT_CANCELED){
                    finish(); // 어플리케이션 종료
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}