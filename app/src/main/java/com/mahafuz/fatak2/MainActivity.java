package com.mahafuz.fatak2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.mahafuz.fatak2.Adapter.DeviceAdapter;
import com.mahafuz.fatak2.Fragments.Home;
import com.mahafuz.fatak2.Interface.DeviceListClick;
import com.mahafuz.fatak2.Interface.UpateUi;
import com.mahafuz.fatak2.Servers.FileHandeler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class MainActivity extends AppCompatActivity implements UpateUi, WifiP2pManager.PeerListListener,
        DeviceListClick,
        WifiP2pManager.ConnectionInfoListener,
        ZXingScannerView.ResultHandler {
    public final static String TAG = "MainActivity";
    WifiP2pManager manager;
    Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;
    List<WifiP2pDevice> p2pDevices = new ArrayList<>();
    Button button, selectFile, receive;
    View customAlertView;
    DeviceAdapter deviceAdapter;
    WifiP2pConfig wifiP2pConfig;
    ServerClass serverClass;
    static FileHandeler fileHandeler;
    FilePickerDialog dialog;
    ServerSocket serverSocket;
    ClientClass clientClass;
    public TextView ipAddressList, status;
    AlertDialog alertDialog;
    boolean isClient = false;
    ImageView qrCodeData;
    LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        assert manager != null;
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WifiBroadcastReciver(manager, channel, this);
        deviceAdapter = new DeviceAdapter(this, p2pDevices);
        wifiP2pConfig = new WifiP2pConfig();
        ipAddressList = findViewById(R.id.ipAdressList);
        status = findViewById(R.id.status);
        getSupportFragmentManager().beginTransaction().replace(R.id.mainHome, new Home(this)).commit();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        button = findViewById(R.id.send);
        receive = findViewById(R.id.receive);
        selectFile = findViewById(R.id.selectFile);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                try {
                    serverSocket = new ServerSocket(8888);
                    serverClass = new ServerClass(serverSocket, MainActivity.this);
                    serverClass.start();
                    ipAddressList.setText(getLocalIpAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;
        properties.show_hidden_files = false;
        dialog = new FilePickerDialog(MainActivity.this, properties);
        dialog.setTitle("Select a File");


        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (files.length > 0) {
                    try {
                        fileHandeler.sentData(new File(files[0]));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


    }

    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 505) {
            File file = new File(data.getData().getPath());
            Log.i(MainActivity.this.getLocalClassName(), data.getData().getPath());
            Log.i(MainActivity.this.getLocalClassName(), file.getName());

        }
    }

    @Override
    public void onThreadWorkDone(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText(message);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        InetAddress ownerAddress = wifiP2pInfo.groupOwnerAddress;
        Log.i(TAG, "" + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            Log.i(TAG, "Owner");
            isClient = false;
            try {
                ServerSocket serverSocket = new ServerSocket(8888);
                serverClass = new ServerClass(serverSocket, this);
                serverClass.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Log.i(TAG, "Client");
            isClient = true;
            clientClass = new ClientClass(wifiP2pInfo.groupOwnerAddress.getHostAddress(), this);
            clientClass.start();
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

    }

    @Override
    public void deviceOnClick(WifiP2pDevice device) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void findPeer() {
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        customAlertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.devicelist, null);
        qrCodeData = customAlertView.findViewById(R.id.qrCodeData);
        lottieAnimationView = customAlertView.findViewById(R.id.device_detail_loading);
        alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Scan the QrCode")
                .setView(customAlertView)
                .setNegativeButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).setCancelable(false)
                .create();
        alertDialog.show();
        assert wifiManager != null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            configApState(MainActivity.this);
        else
        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback(){
            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                Log.i(getLocalClassName(),"WifiOn");
                String ssid = reservation.getWifiConfiguration().SSID;
                String password = reservation.getWifiConfiguration().preSharedKey;
                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                try{
                    BitMatrix bitMatrix = multiFormatWriter.encode(ssid+" "+ password, BarcodeFormat.QR_CODE,200,200);
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                    qrCodeData.setImageBitmap(bitmap);
                    qrCodeData.setVisibility(View.VISIBLE);
                    lottieAnimationView.setVisibility(View.GONE);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(),reservation.getWifiConfiguration().SSID+" "+ reservation.getWifiConfiguration().preSharedKey , Toast.LENGTH_SHORT).show();
                super.onStarted(reservation);
            }

            @Override
            public void onStopped() {
                super.onStopped();
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
            }
        }, new Handler());
    }

    @Override
    public void handleResult(Result rawResult) {
        //TODO:WIFI CONNECTION
        getSupportFragmentManager().popBackStack();
        String[] rawDatas = rawResult.getText().split(" ");
        String ssid = rawDatas[0];
        String key = rawDatas[1];
        Toast.makeText(this, ""+rawResult.getText(), Toast.LENGTH_SHORT).show();
        Log.i(getLocalClassName(),ssid+" "+key);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

        assert wifiManager != null;
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

    //check whether wifi hotspot on or off
    public static boolean isApOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        try {
            assert wifimanager != null;
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        }
        catch (Throwable ignored) {}
        return false;
    }
    // toggle wifi hotspot on or off
    public static void configApState(Context context) {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiConfiguration wificonfiguration = null;
        try {
            // if WiFi is on, turn it on
            if(isApOn(context)) {
                assert wifimanager != null;
                wifimanager.setWifiEnabled(true);
            }
            assert wifimanager != null;
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, wificonfiguration, !isApOn(context));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class ServerClass extends Thread {
        ServerSocket serverSocket;
        Socket socket;
        MainActivity activity;

        public ServerClass(ServerSocket serverSocket, MainActivity activity) {
            this.serverSocket = serverSocket;
            this.activity = activity;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    socket = serverSocket.accept();
                    Log.i(TAG, "CLIENT CONNECTED");
                    activity.onThreadWorkDone("CLIENT CONNECTED");
                    fileHandeler = new FileHandeler(socket, MainActivity.this);
                    fileHandeler.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public class ClientClass extends Thread {
        Socket socket;
        String inetAddress;
        MainActivity activity;

        public ClientClass(String inetAddress, MainActivity activity) {
            this.inetAddress = inetAddress;
            this.activity = activity;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    socket = new Socket(inetAddress, 8888);
                    Log.i(TAG, "CONNECTED TO HOST");
                    activity.onThreadWorkDone("CONNECTED TO HOST");
                    fileHandeler = new FileHandeler(socket, MainActivity.this);
                    fileHandeler.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(getLocalClassName(), ex.toString());
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}