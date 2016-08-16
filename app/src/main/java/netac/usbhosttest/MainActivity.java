package netac.usbhosttest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final int PID=0XC00;//PID 16进制C00 10进制3072
    private static final int VID=3544;//VID 16进制1111 10进制3544


    private EditText param1_et, param2_et, param3_et, param4_et, log_tv;
    private Button commit_btn, clear_btn, search_device_btn;

    private UsbManager mUsbManager;
    private UsbDevice netacDevice;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initEvent();
    }

    private void initView(){
        param1_et= (EditText) findViewById(R.id.param1_et);
        param2_et= (EditText) findViewById(R.id.param2_et);
        param3_et= (EditText) findViewById(R.id.param3_et);
        param4_et= (EditText) findViewById(R.id.param4_et);
        log_tv= (EditText) findViewById(R.id.log_tv);

        commit_btn= (Button) findViewById(R.id.commit_btn);
        clear_btn= (Button) findViewById(R.id.clear_btn);
        search_device_btn= (Button) findViewById(R.id.search_device_btn);
    }

    private void initData(){
        mUsbManager= (UsbManager) getSystemService(Context.USB_SERVICE);

        pendingIntent = PendingIntent.getBroadcast(this, 100, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReciver, intentFilter);
//        String pwd="1234567890abcdefFEDCBA!@#$%^&*()";
        String pwd="123123312";
        String str="0123456789";
        try {
            String encryptingCode =AESEncrypt.encrypt(pwd, str);
            Log("aaa AES 128 Encrypt = "+encryptingCode );
            System.out.println("encrypt="+encryptingCode);
            String decryptingCode = AESEncrypt.decrypt(pwd, encryptingCode);
            Log("%s AES 128 Decrypt = %s", encryptingCode, decryptingCode );
        } catch (Exception e) {
            Log("AES Encrypt Error e="+e.getMessage());
            e.printStackTrace();
        }
    }

    private void initEvent(){
        commit_btn.setOnClickListener(mClickListener);
        clear_btn.setOnClickListener(mClickListener);
        search_device_btn.setOnClickListener(mClickListener);
    }

    private byte[] getCommitData(){
        return null;
    }

    View.OnClickListener mClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.commit_btn:

                    break;
                case R.id.clear_btn:
                    clearLog();
                    break;
                case R.id.search_device_btn:
                    new UsbSearchThread().start();
                    break;
            }
        }
    };

    BroadcastReceiver mUsbReciver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(ACTION_USB_PERMISSION.equals(action)){
                UsbDevice device=intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                boolean granted=intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                Log("Action %s EXTRA_PERMISSION. Device PID=%s, VID=%s", granted?"Has ":"Not Has ", device.getProductId(), device.getVendorId());
                if(granted && device!=null){
                    Log("USB Host.");
                    new USBHostThread().start();
                }
            }
        }
    };


    //USB 设备检索
    class UsbSearchThread extends Thread{

        @Override
        public void run() {
            super.run();
            try {
                //API 12 运行最低版本是Android3.1
                Map<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
                if(deviceMap.isEmpty())return;
                //检索设备，过滤出固定的设备出来
                Log("Search Device Size=%s;", deviceMap.size());
                for(UsbDevice device : deviceMap.values()){
                    //Log(" Search Info:describeContents=%s, getName=%s, getDeviceId=%s, getProductId=%s, getVendorId=%s, getDeviceName=%s", device.describeContents(), device.getDeviceId(), device.getDeviceName(), device.getProductId(), device.getVendorId(), device,getName());
                    if(device.getProductId()==PID && device.getVendorId()==VID){
                        //Netac Test Device;
                        netacDevice=device;
                        Log("Has Search Netac Device.Device PID=%s, VID=%s", device.getProductId(), device.getVendorId());
                        break;
                    }
                }

                if(netacDevice==null){
                    Log("Not Search Netac Device.");
                    return;
                }

                if(mUsbManager.hasPermission(netacDevice)){
                    Log("Has Device Permission");
                    Log("USB Host.");
                    new USBHostThread().start();
                }else{
                    Log("Not Has Device Permission. mUsbManager.requestPermission");
                    mUsbManager.requestPermission(netacDevice, pendingIntent);
                }
            } catch (Exception e) {
                Log("Exception e="+e.getMessage());
                e.printStackTrace();
            }
        }
    }


    class USBHostThread extends Thread{

        @Override
        public void run() {
            super.run();

            try {
                if(netacDevice==null)return;
                UsbInterface usbInterface=null, anInterface=null;
                UsbEndpoint inEndpoint=null, outEndpoint=null;
                for(int i=0; i<netacDevice.getInterfaceCount(); i++){
                    anInterface = netacDevice.getInterface(i);
                    Log("USB Interface="+anInterface.getInterfaceClass()+"  storage="+UsbConstants.USB_CLASS_MASS_STORAGE);
                    if(UsbConstants.USB_CLASS_MASS_STORAGE==anInterface.getInterfaceClass()){//寻找出大容量的存储设备

                        //寻找指令的输入端口和信息的输出端口
                        for(int j=0; j<anInterface.getEndpointCount(); j++){
                            UsbEndpoint usbEndpoint=anInterface.getEndpoint(j);
                            Log("UsbEndpoint Type="+usbEndpoint.getType());
                            if(UsbConstants.USB_ENDPOINT_XFER_BULK==usbEndpoint.getType()){//寻找执行指令的端口
                                if(usbEndpoint.getDirection()==UsbConstants.USB_DIR_OUT){//寻找主机->设备的输入端口
                                    outEndpoint=usbEndpoint;
                                }else if(usbEndpoint.getDirection()==UsbConstants.USB_DIR_IN){//寻找设备->主机的输出端口
                                    inEndpoint=usbEndpoint;
                                }
                            }
                        }


                    }
                }

                if(anInterface==null || inEndpoint==null || outEndpoint==null){
                    Log("UsbEndpoint find faild.");
                    return;
                }


                usbInterface=anInterface;
                UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(netacDevice);
                boolean clain=usbDeviceConnection.claimInterface(usbInterface, true);
                Log("usbDeviceConnection.claimInterface=%s",clain);
                byte[] buff=USBInstruction.getInstance().getDeviceVersion();
                int sendLength=usbDeviceConnection.bulkTransfer(outEndpoint, buff, buff.length, 3000);
                Log("Send Msg=%s, length=%s", new String(buff), sendLength);
                for(byte b : buff)Log("send data="+b);;
                byte[] reciverBuff=new byte[1024];
                int reciverLength=usbDeviceConnection.bulkTransfer(inEndpoint, reciverBuff, reciverBuff.length, 3000);
                Log("Reciver Msg=%s, length=%s", new String(reciverBuff), reciverLength);



//                UsbDeviceConnection usbDeviceConnection=mUsbManager.openDevice(netacDevice);
//                usbDeviceConnection.claimInterface(usbInterface, true);
//
//                byte[] reciver=new byte[64];
//                byte[] versionINS=USBInstruction.getInstance().getDeviceVersion();
//                Log("Base="+new String(versionINS));
//                int sendLength=usbDeviceConnection.bulkTransfer(inEndpoint, versionINS, versionINS.length, 3000);
//                Log("Send get device Version Ins, send Length="+sendLength);
//
//                int revicerLength=usbDeviceConnection.bulkTransfer(outEndpoint, reciver, reciver.length, 3000);
//                Log("revicer msg length="+revicerLength+", msg="+new String(reciver));
            } catch (Exception e) {
                Log("Exception e="+e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void Log(String format, Object... args){
        String log=String.format(format, args);
        log=log.replaceAll(",",",\t");
        log+="\n";
        mHandler.sendMessage(mHandler.obtainMessage(1, log));
    }

    private void clearLog(){
        mHandler.sendEmptyMessage(2);
    }

    Handler mHandler=new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    if(log_tv==null)return;
                    log_tv.append(msg.obj.toString()+"\n");
                    log_tv.setSelection(log_tv.length());
                    ((ScrollView)log_tv.getParent()).fullScroll(ScrollView.FOCUS_DOWN);
                    break;
                case 2:
                    log_tv.setText("");
                    break;
            }
        }
    };
}
