package netac.usbhosttest;

/**
 * Created by siwei.zhao on 2016/8/15.
 * USB 指令
 */
public class USBInstruction {

    private static USBInstruction sUSBInstruction;

    private final int BASE_INS = 0X7E;//所有私有命令的总命令的开始

    private final byte[] BASE_INS_BYTES =new byte[]{0X7E};

    private USBInstruction(){}

    public static USBInstruction getInstance(){
        if(sUSBInstruction==null)sUSBInstruction=new USBInstruction();
        return sUSBInstruction;
    }


    public byte[] getDeviceVersion(){
        byte b=0X01;
        byte defaultB=0X00;
        return appendINS(b, b, defaultB);
    }

    private byte[] int2byte(int i){
        int byteNum = (40 - Integer.numberOfLeadingZeros (i < 0 ? ~i : i)) / 8;
        byte[] byteArray = new byte[4];

        for (int n = 0; n < byteNum; n++)
            byteArray[3 - n] = (byte) (i >>> (n * 8));
        return byteArray;
    }

    //追加指令
    private byte[] appendINS(byte... bs){
        byte[] insByte=new byte[BASE_INS_BYTES.length+bs.length];
        for(int i=0; i<BASE_INS_BYTES.length; i++)insByte[i]=BASE_INS_BYTES[i];
        for(int i=BASE_INS_BYTES.length; i<insByte.length; i++)insByte[i]=bs[i-BASE_INS_BYTES.length];
        return insByte;
    };

}
