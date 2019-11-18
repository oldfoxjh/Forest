package kr.go.forest.das.MAVLink;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import io.dronefleet.mavlink.Mavlink2Message;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.dronefleet.mavlink.ardupilotmega.AutopilotVersionRequest;
import io.dronefleet.mavlink.common.Altitude;
import io.dronefleet.mavlink.common.Attitude;
import io.dronefleet.mavlink.common.AutopilotVersion;
import io.dronefleet.mavlink.common.BatteryStatus;
import io.dronefleet.mavlink.common.CommandAck;
import io.dronefleet.mavlink.common.CommandInt;
import io.dronefleet.mavlink.common.CommandLong;
import io.dronefleet.mavlink.common.EstimatorStatus;
import io.dronefleet.mavlink.common.ExtendedSysState;
import io.dronefleet.mavlink.common.Heartbeat;
import io.dronefleet.mavlink.common.HighresImu;
import io.dronefleet.mavlink.common.HomePosition;
import io.dronefleet.mavlink.common.LocalPositionNed;
import io.dronefleet.mavlink.common.MavAutopilot;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavState;
import io.dronefleet.mavlink.common.MavType;
import io.dronefleet.mavlink.common.ParamRequestList;
import io.dronefleet.mavlink.common.ParamRequestRead;
import io.dronefleet.mavlink.common.Ping;
import io.dronefleet.mavlink.common.RcChannels;
import io.dronefleet.mavlink.common.RcChannelsOverride;
import io.dronefleet.mavlink.common.ServoOutputRaw;
import io.dronefleet.mavlink.common.SysStatus;
import io.dronefleet.mavlink.common.UtmGlobalPosition;
import io.dronefleet.mavlink.common.Vibration;
import kr.go.forest.das.Log.LogWrapper;

import static io.dronefleet.mavlink.common.MavCmd.MAV_CMD_GET_HOME_POSITION;
import static io.dronefleet.mavlink.common.MavCmd.MAV_CMD_REQUEST_AUTOPILOT_CAPABILITIES;

public class MavDataManager implements Runnable{

    private String TAG = "MAV Data Manager";
    public static final String ACTION_USB_PERMISSION = "kr.go.forest.das.USB_PERMISSION";

    public final static int MAVLINK_TYPE_1 = 1;
    public final static int MAVLINK_TYPE_2 = 2;

    public final static int BAUDRATE_57600 = 57600;
    public final static int BAUDRATE_115200 = 115200;

    private Context context;
    private UsbSerialPort usb_port = null ;
    private int baudrate;
    private MavlinkConnection mav_connection = null ;
    public UsbDeviceConnection usb_connection = null ;
    private boolean mavThread_exit = false;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private volatile SerialPortInputStream inputStream = null;
    private volatile SerialPortOutputStream outputStream = null;
    private MavEventListener listener = null;

    private int system_id;
    private int component_id;


    public MavDataManager(Context context, int baudrate, MavEventListener listener){
        this.context = context;
        this.baudrate = baudrate;
        inputStream = new SerialPortInputStream();
        outputStream = new SerialPortOutputStream();
        this.listener = listener;
    }

    public boolean open(){
        if ( usb_port != null ) {
            try { usb_port.close() ; } catch (IOException e) {System.out.println(e);}
            usb_port = null ;
        }

        UsbManager usb_manager = (UsbManager) context.getSystemService(android.content.Context.USB_SERVICE);
        if ( usb_manager == null ) {
            LogWrapper.e(TAG, "usb_manager == null");
            return false;
        }

        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usb_manager);
        if (availableDrivers.isEmpty()) {
            LogWrapper.e(TAG, "availableDrivers.isEmpty()");
            return false ;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection usb_connection = usb_manager.openDevice(driver.getDevice());
        if (usb_connection == null) {
            LogWrapper.e(TAG, "usb_connection == null");
            return false ;
        }

        usb_port = driver.getPorts().get(0);
        try {
            usb_port.open(usb_connection);
            usb_port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            onDeviceStateChange() ;
        } catch (IOException e) {
            usb_port = null ;
        }

        return (usb_port != null) ;
    }

    public boolean open(UsbDeviceConnection connection,UsbSerialPort port)  {
        try {
            Log.e("Command Ack", "open usb");
            usb_port = port ;
            usb_connection = connection ;
            usb_port.open(usb_connection);
            usb_port.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            onDeviceStateChange() ;
        } catch (IOException e) {
            Log.e("Command Ack", "open usb fail");
            return false ;
        }

        return (usb_port != null) ;
    }

    public void close() {
        mavThread_exit = true ;
        mExecutor.shutdown();
        if ( usb_port != null ) {
            try {
                usb_port.close();
            } catch (IOException e) {
                e.getStackTrace() ;
            }
        }
        usb_port = null ;

        try {
            mExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch ( InterruptedException e) {
            e.printStackTrace();
        }
        LogWrapper.i(TAG, "PX4 mavlink close");
    }

    public void send(Object payload){
        try {
            mav_connection.send2(255, 0, payload);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void sendHeartBeat(){
        Heartbeat heartbeat = Heartbeat.builder()
                .type(MavType.MAV_TYPE_GCS)
                .autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID)
                .systemStatus(MavState.MAV_STATE_UNINIT)
                .mavlinkVersion(3)
                .build();

        send(heartbeat);
    }

    /**
     * AutopilotVersion 정보 요청
     */
    public void requestAutopilotVersion(){
        CommandInt request = CommandInt.builder()
                .targetComponent(component_id)
                .targetSystem(system_id)
                .command(MavCmd.MAV_CMD_REQUEST_AUTOPILOT_CAPABILITIES)
                .build();

        send(request);
    }

    public void requestParameterList(){
        ParamRequestList request =
                ParamRequestList.builder()
                    .targetSystem(system_id)
                    .targetComponent(component_id)
                    .build();
        send(request);
    }

    public void requestHomePosition(){
        CommandLong request = CommandLong.builder()
                                         .targetComponent(component_id)
                                         .targetSystem(system_id)
                                         .command(MavCmd.MAV_CMD_GET_HOME_POSITION)
                                         .build();
        send(request);
    }

    public void requestTakeOff(){
        CommandLong request = CommandLong.builder()
                .targetComponent(component_id)
                .targetSystem(system_id)
                .command(MavCmd.MAV_CMD_NAV_TAKEOFF)
                .param7(1.2f)
                .build();

        send(request);
    }

    public void requestLand(){
        CommandLong request = CommandLong.builder()
                .targetComponent(component_id)
                .targetSystem(system_id)
                .command(MavCmd.MAV_CMD_NAV_LAND)
                .param7(0.0f)
                .build();

        send(request);
    }

    private void onDeviceStateChange() {
        if (mav_connection != null && !mavThread_exit) {
            mavThread_exit = true;
        }

        if (mav_connection == null)
            mav_connection = MavlinkConnection.create(inputStream, outputStream);
        mavThread_exit = false;

        mExecutor.submit(this);
    }

    @Override
    public void run() {
        try {
            while ( !mavThread_exit ) {
                step();
            }
        } catch (Exception e) {

        }
    }

    private void step() throws IOException{
        if ( mav_connection == null ) return ;

        MavlinkMessage message;
        if ( (message = mav_connection.next()) != null) {
            int type = MAVLINK_TYPE_1;
            if (message instanceof Mavlink2Message) type = MAVLINK_TYPE_2;

            system_id = message.getOriginSystemId();
            component_id = message.getOriginComponentId();

            Object payload = message.getPayload();

            if(payload instanceof Heartbeat){
                sendHeartBeat();
            }else if(payload instanceof Ping){
                requestAutopilotVersion();
            }else if(payload instanceof Altitude || payload instanceof Attitude || payload instanceof RcChannels || payload instanceof RcChannelsOverride || payload instanceof LocalPositionNed
                    || payload instanceof BatteryStatus || payload instanceof ServoOutputRaw || payload instanceof ExtendedSysState || payload instanceof HighresImu || payload instanceof EstimatorStatus || payload instanceof Vibration || payload instanceof SysStatus
                    || payload instanceof UtmGlobalPosition
            ){

            }else {
               Log.e("Command Ack", payload.toString());
            }

            if(listener != null) listener.onReceive(payload, type);
        }
    }

    private synchronized UsbSerialPort getUsbPort() {
        return usb_port ;
    }

    private final class SerialPortInputStream extends InputStream {
        private int headIndex = 0 ;
        private int tailIndex = 0 ;
        private static final int BUFSIZ = 10240;
        private final byte[] mRingBuffer = new byte[BUFSIZ];
        private static final int READ_WAIT_MILLIS = 200;

        public SerialPortInputStream() {}

        @Override
        public final int available() throws IOException
        {
            final UsbSerialPort port = getUsbPort() ;
            if (port == null )
                throw new IOException("This port appears to have been shutdown or disconnected.");
            byte[] byteBuffer = new byte[BUFSIZ];
            int len = port.read(byteBuffer, READ_WAIT_MILLIS);
            if (len > 0) {
                if ( headIndex + len >= BUFSIZ ) {
                    int wLen = BUFSIZ - headIndex ;
                    System.arraycopy(byteBuffer, 0, mRingBuffer, tailIndex, wLen);
                    System.arraycopy(byteBuffer, wLen, mRingBuffer, 0, len-wLen);
                    headIndex = headIndex + len - BUFSIZ ;
                }
                else {
                    System.arraycopy(byteBuffer, 0, mRingBuffer, tailIndex, len);
                    headIndex += len ;
                }
            }
            return len ;
        }

        @Override
        public final int read() throws IOException
        {
            // Perform error checking
            int retVal = 0;
            while ( headIndex == tailIndex ) {
                available() ;
            }
            if ( headIndex != tailIndex ) {
                retVal = mRingBuffer[tailIndex] & 0xFF ;
                tailIndex ++ ;
                if ( tailIndex >= BUFSIZ ) {
                    tailIndex = 0 ;
                }
            }

            return retVal ;
        }

        @Override
        public final int read(byte[] b) throws NullPointerException
        {
            int wLen = b.length ;
            int retVal = 0;
            if ( headIndex != tailIndex ) {
                if ( headIndex > tailIndex ) {
                    int rLen = headIndex - tailIndex ;
                    if ( rLen > wLen ) {
                        System.arraycopy(mRingBuffer, tailIndex, b, 0, wLen);
                        tailIndex += wLen ;
                        retVal = wLen ;
                    }
                    else {
                        System.arraycopy(mRingBuffer, tailIndex, b, 0, rLen);
                        tailIndex += rLen ;
                        retVal = rLen ;
                    }
                }
                else {
                    int rLen = (BUFSIZ - tailIndex) +  headIndex + 1 ;
                    if ( rLen > wLen ) {
                        System.arraycopy(mRingBuffer, tailIndex, b, 0, BUFSIZ - wLen);
                        System.arraycopy(mRingBuffer, 0, b, BUFSIZ - wLen, wLen-(BUFSIZ - wLen));
                        tailIndex += wLen ;
                        retVal = wLen ;
                    }
                    else {
                        System.arraycopy(mRingBuffer, tailIndex, b, 0, BUFSIZ - rLen);
                        System.arraycopy(mRingBuffer, 0, b, BUFSIZ - rLen, rLen-(BUFSIZ - rLen));
                        tailIndex += rLen ;
                        retVal = rLen ;
                    }
                }
            }
            if ( tailIndex >= BUFSIZ ) {
                tailIndex = tailIndex - BUFSIZ ;
            }
            return retVal ;
        }

        @Override
        public final int read(byte[] b, int off, int len) throws NullPointerException, IndexOutOfBoundsException
        {
            int wLen = len ;
            int retVal = 0;
            if ( headIndex != tailIndex ) {
                if ( headIndex > tailIndex ) {
                    int rLen = headIndex - tailIndex ;
                    if ( rLen > wLen ) {
                        System.arraycopy(mRingBuffer, tailIndex, b, off, wLen);
                        tailIndex += wLen ;
                        retVal = wLen ;
                    }
                    else {
                        System.arraycopy(mRingBuffer, tailIndex, b, off, rLen);
                        tailIndex += rLen ;
                        retVal = rLen ;
                    }
                }
                else {
                    int rLen = (BUFSIZ - tailIndex) +  headIndex + 1 ;
                    if ( rLen > wLen ) {
                        System.arraycopy(mRingBuffer, tailIndex, b, off, BUFSIZ - wLen);
                        System.arraycopy(mRingBuffer, 0, b, off + BUFSIZ - wLen, wLen-(BUFSIZ - wLen));
                        tailIndex += wLen ;
                        retVal = wLen ;
                    }
                    else {
                        System.arraycopy(mRingBuffer, tailIndex, b, off, BUFSIZ - rLen);
                        System.arraycopy(mRingBuffer, 0, b, off + BUFSIZ - rLen, rLen-(BUFSIZ - rLen));
                        tailIndex += rLen ;
                        retVal = rLen ;
                    }
                }
            }
            if ( tailIndex >= BUFSIZ ) {
                tailIndex = tailIndex - BUFSIZ ;
            }
            return retVal ;
        }

        @Override
        public final long skip(long n) throws IOException
        {
            int skipTail = tailIndex + (int)n ;
            if ( tailIndex < headIndex ) {
                int rLen = headIndex - tailIndex ;
                if ( n > rLen ) n = rLen ;
                tailIndex += n;
            }
            else {
                int rLen = (BUFSIZ - tailIndex) +  headIndex + 1 ;
                if ( n > rLen ) n = rLen ;
                tailIndex += n;
            }
            if ( tailIndex >= BUFSIZ ) {
                tailIndex = tailIndex - BUFSIZ ;
            }
            return n ;
        }
    }

    // OutputStream interface class
    private final class SerialPortOutputStream extends OutputStream {
        private byte[] byteBuffer = new byte[1];

        public SerialPortOutputStream() {
        }

        @Override
        public final void write(int b) throws IOException {
            final UsbSerialPort port = getUsbPort() ;
            if (port == null )
                throw new IOException("This port appears to have been shutdown or disconnected.");
            byteBuffer[0] = (byte) (b & 0xFF);
            int bytesWritten = port.write(byteBuffer, 100);
//            getListener().onNewData(bytesWritten + " write bytes");
            if (bytesWritten <= 0)
                throw new IOException("This port appears to have been shutdown or disconnected.");
        }

        @Override
        public final void write(byte[] b) throws NullPointerException, IOException {
            final UsbSerialPort port = getUsbPort() ;
            port.write(b, 1000);
//            getListener().onNewData(" write(byte[]) bytes");
        }

        @Override
        public final void write(byte[] b, int off, int len) throws NullPointerException, IndexOutOfBoundsException, IOException {
            // Perform error checking
            final UsbSerialPort port = getUsbPort() ;
            if (b == null)
                throw new NullPointerException("A null pointer was passed in for the write buffer.");
            if ((len < 0) || (off < 0) || ((off + len) > b.length))
                throw new IndexOutOfBoundsException("The specified write offset plus length extends past the end of the specified buffer.");
            if (port == null )
                throw new IOException("This port appears to have been shutdown or disconnected.");
            if (len == 0)
                return;

            // Write to the serial port
            byte[] tmp = new byte[len];
            System.arraycopy(b , off , tmp , 1 , len );
            int numWritten = port.write(tmp,1000) ;
//            getListener().onNewData(numWritten + " write(byte[],off,len) bytes");
            if (numWritten <= 0)
                throw new IOException("This port appears to have been shutdown or disconnected.");
        }
    }

    public interface MavEventListener{
        void onReceive(Object payload, int type);
    }
}
