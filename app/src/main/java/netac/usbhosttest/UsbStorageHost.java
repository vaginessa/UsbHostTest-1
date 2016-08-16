package netac.usbhosttest;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by siwei.zhao on 2016/8/15.
 * USB 存储设备host端
 */
public class UsbStorageHost {

    private int mPID, mVID;//设备PID和设备VID
    private Application mAPP;
    private UsbManager mUsbManager;
    private int mConnectionTimeOut=3000;//连接超时时长

    private UsbStorageHost(int pid, int vid, Application app){
        mPID=pid;
        mVID=vid;
        mAPP=app;
        mUsbManager= (UsbManager) app.getSystemService(Context.USB_SERVICE);
    }

    //寻找设备
    private UsbDevice searchDevice(){
        Map<String, UsbDevice> usbDeviceMap = mUsbManager.getDeviceList();
        if(usbDeviceMap==null)return null;
        for(UsbDevice usbDevice : usbDeviceMap.values()){
            if(usbDevice.getProductId()==mPID && usbDevice.getVendorId()==mVID){
                return usbDevice;
            }
        }
        return null;
    }

    //寻找大容量存储设备
    private List<UsbInterface> findUsbInterface(UsbDevice usbDevice){
        List<UsbInterface> usbInterfaces=new ArrayList<>();
        if(usbDevice==null)return usbInterfaces;
        for(int i=0; i<usbDevice.getInterfaceCount(); i++){
            UsbInterface usbInterface=usbDevice.getInterface(i);
            if(usbInterface.getInterfaceClass( )== UsbConstants.USB_CLASS_MASS_STORAGE){
                //寻找大容量存储设备
                usbInterfaces.add(usbInterface);
            }
        }
        return usbInterfaces;
    }

    //获取到输入端和输出端
    private void getUsbEndpoint(UsbInterface usbInterface, UsbEndpoint outEndpoint, UsbEndpoint inEndpoint){
        if(usbInterface==null)return;
        for(int i=0; i<usbInterface.getEndpointCount(); i++){
            UsbEndpoint usbEndpoint=usbInterface.getEndpoint(i);
            if(UsbConstants.USB_ENDPOINT_XFER_BULK==usbEndpoint.getType()){
                if(UsbConstants.USB_DIR_IN==usbEndpoint.getDirection()){
                    inEndpoint=usbEndpoint;
                }else if(UsbConstants.USB_DIR_OUT==usbEndpoint.getDirection()){
                    outEndpoint=usbEndpoint;
                }
            }
        }
    }

    //发送指令
    private byte[] sendIns(UsbDeviceConnection usbDeviceConnections, UsbEndpoint outEndpoints, UsbEndpoint inEndpoint, byte[] send){
        if(usbDeviceConnections==null || outEndpoints==null || send==null)return null;
        byte[] result=null;
        int sendLength = usbDeviceConnections.bulkTransfer(outEndpoints, send, send.length, mConnectionTimeOut);
        if(sendLength>=0 && inEndpoint!=null){
            result=new byte[1024];
            int resultLength = usbDeviceConnections.bulkTransfer(inEndpoint, result, result.length, mConnectionTimeOut);
        }
        return result;
    }

    //连接线程
    private class ConnectThread extends Thread{

        @Override
        public void run() {
            super.run();
        }
    }
}
