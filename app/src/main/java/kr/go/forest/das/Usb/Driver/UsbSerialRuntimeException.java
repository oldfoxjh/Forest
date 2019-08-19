package kr.go.forest.das.Usb.Driver;

@SuppressWarnings("serial")
public class UsbSerialRuntimeException extends RuntimeException {

    public UsbSerialRuntimeException() {
        super();
    }

    public UsbSerialRuntimeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public UsbSerialRuntimeException(String detailMessage) {
        super(detailMessage);
    }

    public UsbSerialRuntimeException(Throwable throwable) {
        super(throwable);
    }

}
