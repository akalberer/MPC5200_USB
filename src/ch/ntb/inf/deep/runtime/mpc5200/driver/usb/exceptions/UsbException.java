package ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions;

import java.io.IOException;

public class UsbException extends IOException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7815687304701563477L;

	public UsbException(){
		super();
	}
	
	public UsbException(String message){
		super(message);
	}
	
	public UsbException(String message, Throwable cause){
		super(message, cause);
	}
	
	public UsbException(Throwable cause){
		super(cause);
	}
	
}
