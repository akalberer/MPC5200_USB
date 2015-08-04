package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.unsafe.US;

public class UsbRequest {

	TransferDescriptor setupTd;
	TransferDescriptor dataTd;
	TransferDescriptor statusTd;
	
	private static byte[] setupGetDevDescEnum = new byte[]{
		(byte) 0x80,		// bRequestType: get descriptor
		(byte) 0x06,		// bRequest
		(byte) 0x00,		// index 0
		(byte) 0x01,		// type of descriptor: DeviceDescriptor
		(byte) 0x00,		// ID of language, else 0
		(byte) 0x00,
		(byte) 0x08,		// length to read (8 bytes)
		(byte) 0x00			// high byte
	};
	
	private static byte[] setupGetDevDesc = new byte[]{
		(byte) 0x80,		// bRequestType: get descriptor
		(byte) 0x06,		// bRequest
		(byte) 0x00,		// index 0
		(byte) 0x01,		// type of descriptor: DeviceDescriptor
		(byte) 0x00,		// ID of language, else 0
		(byte) 0x00,
		(byte) 0x12,		// length to read (18 bytes)
		(byte) 0x00			// high byte
	};
	
	public UsbRequest(){
		
	}
	
	public void getDeviceDescriptor(byte[] data) throws UsbException{
		if(data.length < 18){
			throw new UsbException("UsbRequest: byte array too short.");
		}
		
		setupTd = new TransferDescriptor(TdType.SETUP, setupGetDevDesc, 8);
		OhciHcd.enqueueTd(setupTd);
		dataTd = new TransferDescriptor(TdType.IN, data, 18);
		OhciHcd.enqueueTd(dataTd);
		statusTd = new TransferDescriptor(TdType.STATUS_OUT, null, 0);
		OhciHcd.enqueueTd(statusTd);
	}
	
	public void getDeviceDescriptorEnumeration(byte[] data) throws UsbException{
		if(data.length < 8){
			throw new UsbException("UsbRequest: byte array too short.");
		}
		
		setupTd = new TransferDescriptor(TdType.SETUP, setupGetDevDescEnum, 8);
		OhciHcd.enqueueTd(setupTd);
		dataTd = new TransferDescriptor(TdType.IN, data, 8);
		OhciHcd.enqueueTd(dataTd);
		statusTd = new TransferDescriptor(TdType.STATUS_OUT, null, 0);
		OhciHcd.enqueueTd(statusTd);
	}
	
	public boolean controlDone(){
		if(statusTd.done()){
			return true;
		}
		return false;
	}
}
