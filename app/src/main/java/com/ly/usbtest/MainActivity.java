package com.ly.usbtest;

import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
;

public class MainActivity extends AppCompatActivity {
    private String PC_START_BROADCAST = "NotifyServiceStart";
    private String PC_STOP_BROADCAST = "NotifyServiceStop";
    SocketServerThread debugThread;
    private PcBroadCastReceiver pcBroadCastReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pcBroadCastReceiver = new PcBroadCastReceiver();
        registerPcBroadCast();
        debugThread = new SocketServerThread();
        debugThread.start();
//        debugThread.SendMsg("你好 USB");

    }

    //动态注册广播
    private void registerPcBroadCast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PC_START_BROADCAST);
        intentFilter.addAction(PC_STOP_BROADCAST);
        registerReceiver(pcBroadCastReceiver,intentFilter);
    }

    //注销广播
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(pcBroadCastReceiver);
    }

    class SocketServerThread extends Thread {

        private BufferedOutputStream out;
        private Socket client;

        @Override
        public void run() {
            try {
                Log.e("[lylog]", "等待连接");
                System.out.println("[lylog]----socket 通信线程----等待连接");
                ServerSocket serverSocket = new ServerSocket(10010);
                while (true) {
                    client = serverSocket.accept();
                    out = new BufferedOutputStream(client.getOutputStream());
                    // 开启子线程去读去数据
                    new Thread(new SocketReadThread(new BufferedInputStream(client.getInputStream()))).start();
                    SendMsg("你好 USB");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //暴露给外部调用写入流的方法
        public void SendMsg(String msg) {
            String msg_1 = msg;
            try {
                out.write(msg_1.getBytes("UTF-8"));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        class SocketReadThread implements Runnable {

            private BufferedInputStream in;

            public SocketReadThread(BufferedInputStream inStream) throws IOException {
                this.in = inStream;
            }

            public void run() {
                try {
                    String readMsg = "";
                    while (true) {
                        try {
                            if (!client.isConnected()) {
                                break;
                            }
                            //   读到后台发送的消息  然后去处理
                            readMsg= readMsgFromSocket(in);
                            //    处理读到的消息(主要是身份证信息),然后保存在sp中;
                            if (readMsg.length() == 0) {
                                break;
                            }
                            //  将要返回的数据发送给 pc
                            out.write((readMsg + "flag").getBytes());
                            out.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //读取PC端发送过来的数据
            private String readMsgFromSocket(InputStream in) {
                String msg = "";
                byte[] temp = new byte[1024];
                try {
                    int readedBytes = in.read(temp, 0, temp.length);
                    msg = new String(temp, 0, readedBytes, "utf-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return msg;
            }
        }
    }


}

