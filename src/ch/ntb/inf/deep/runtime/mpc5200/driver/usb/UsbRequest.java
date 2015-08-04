package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;

public class UsbRequest {

	public static void getDeviceDescriptor(byte[] data) throws UsbException{
		if(data.length < 18){
			throw new UsbException("UsbRequest: byte array too short.");
		}
		
		byte[] setupGetDevDescriptor = new byte[8];
		
		setupGetDevDescriptor[0] = (byte) 0x80;		// bRequestType: get descriptor
		setupGetDevDescriptor[1] = (byte) 0x06;		// bRequest
		setupGetDevDescriptor[2] = (byte) 0x00;		// index 0
		setupGetDevDescriptor[3] = (byte) 0x01;		// type of descriptor: DeviceDescriptor
		setupGetDevDescriptor[4] = (byte) 0x00;		// ID of language, else 0
		setupGetDevDescriptor[5] = (byte) 0x00;
		setupGetDevDescriptor[6] = (byte) 0x12;		// length to read (18 bytes)
		setupGetDevDescriptor[7] = (byte) 0x00;		// high byte
		
		TransferDescriptor setupTest = new TransferDescriptor(TdType.SETUP, setupGetDevDescriptor, 8);
		OhciHcd.enqueueTd(setupTest);
		TransferDescriptor dataTest = new TransferDescriptor(TdType.IN, data, 18);
		OhciHcd.enqueueTd(dataTest);
		TransferDescriptor statusTest = new TransferDescriptor(TdType.STATUS_OUT, null, 0);
		OhciHcd.enqueueTd(statusTest);
	}
}
