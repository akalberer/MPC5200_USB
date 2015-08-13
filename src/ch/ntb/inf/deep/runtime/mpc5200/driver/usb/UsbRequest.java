package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.EndpointType;
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
	
	private static byte[] setupSetAddress = new byte[]{
		(byte) 0x00,		// bRequestType: SET_ADDRESS
		(byte) 0x05,		// bRequest
		(byte) 0x00,		// new device Address low byte
		(byte) 0x00,		// new device Address empty
		(byte) 0x00,		// no index
		(byte) 0x00,
		(byte) 0x00,		// length = 0
		(byte) 0x00
	};
	
	private static byte[] setupSetConfiguration = new byte[]{
		(byte) 0x00,		// bRequestType
		(byte) 0x09,		// bRequest
		(byte) 0x00,		// ID of desired config
		(byte) 0x00,		// high byte of desired config -> reserved -> leave blank
		(byte) 0x00,		// index 0
		(byte) 0x00,
		(byte) 0x00,		// length = 0
		(byte) 0x00
	};
	
	private static byte[] setupSetInterface = new byte[]{
		(byte) 0x01,		// bRequestType
		(byte) 0x0B,		// bRequest
		(byte) 0x00,		// ID of alternate setting
		(byte) 0x00,		
		(byte) 0x00,		// interface-id
		(byte) 0x00,
		(byte) 0x00,		// length
		(byte) 0x00
	};
	
	public UsbRequest(){
		
	}
	
	public void getDeviceDescriptor(byte[] data) throws UsbException{
		if(data.length < 18){
			throw new UsbException("UsbRequest: byte array too short.");
		}
		
		OhciHcd.skipControlEndpoint();
		setupTd = new TransferDescriptor(TdType.SETUP, setupGetDevDesc, 8);
		OhciHcd.enqueueTd(EndpointType.CONTROL, setupTd);
		dataTd = new TransferDescriptor(TdType.IN, data, 18);
		OhciHcd.enqueueTd(EndpointType.CONTROL, dataTd);
		statusTd = new TransferDescriptor(TdType.STATUS_OUT, null, 0);
		OhciHcd.enqueueTd(EndpointType.CONTROL, statusTd);
		OhciHcd.resumeControlEndpoint();
	}
	
	public void getDeviceDescriptorEnumeration(byte[] data) throws UsbException{
		if(data.length < 8){
			throw new UsbException("UsbRequest: byte array too short.");
		}
		
		OhciHcd.skipControlEndpoint();
		setupTd = new TransferDescriptor(TdType.SETUP, setupGetDevDescEnum, 8);
		OhciHcd.enqueueTd(EndpointType.CONTROL, setupTd);
		dataTd = new TransferDescriptor(TdType.IN, data, 8);
		OhciHcd.enqueueTd(EndpointType.CONTROL, dataTd);
		statusTd = new TransferDescriptor(TdType.STATUS_OUT, null, 0);
		OhciHcd.enqueueTd(EndpointType.CONTROL, statusTd);
		OhciHcd.resumeControlEndpoint();
	}
	
	public void setAddress(int address) throws UsbException{
		if(address > 127 || address <= 0){
			throw new UsbException("UsbRequest: address not valid.");
		}
		setupSetAddress[2] = (byte) address;
		
		OhciHcd.skipControlEndpoint();
		setupTd = new TransferDescriptor(TdType.SETUP, setupSetAddress, 8);
		OhciHcd.enqueueTd(EndpointType.CONTROL, setupTd);
		// no data phase
		statusTd = new TransferDescriptor(TdType.STATUS_IN, null, 0);
		OhciHcd.enqueueTd(EndpointType.CONTROL, statusTd);
		OhciHcd.resumeControlEndpoint();
	}
	
	public void setConfiguration(int configValue) throws UsbException{
		if(configValue > 255 || configValue < 0 ){
			throw new UsbException("UsbRequest: invalid config value.");
		}
		
		OhciHcd.skipControlEndpoint();
		setupSetConfiguration[2] = (byte) configValue;
		setupTd = new TransferDescriptor(TdType.SETUP, setupSetConfiguration, 8);
		OhciHcd.enqueueTd(EndpointType.CONTROL, setupTd);
		// no data phase
		statusTd = new TransferDescriptor(TdType.STATUS_IN, null, 0);
		OhciHcd.enqueueTd(EndpointType.CONTROL, statusTd);
		OhciHcd.resumeControlEndpoint();
	}
	
	public void setInterface(int ifaceNum, int altSetting) throws UsbException{
		if(ifaceNum > 255 || ifaceNum < 0 || altSetting > 255 || altSetting < -1){
			throw new UsbException("UsbRequest: invalid paramter");
		}
		if(altSetting == -1){
			altSetting = 0;
		}
		
		OhciHcd.skipControlEndpoint();
		setupSetInterface[2] = (byte) altSetting;
		setupSetInterface[4] = (byte) ifaceNum;
		setupTd = new TransferDescriptor(TdType.SETUP, setupSetInterface, 8);
		OhciHcd.enqueueTd(EndpointType.CONTROL, setupTd);
		// no data phase
		statusTd = new TransferDescriptor(TdType.STATUS_IN, null, 0);
		OhciHcd.enqueueTd(EndpointType.CONTROL, statusTd);
		OhciHcd.resumeControlEndpoint();
	}
	
	public void bulkTransfer(int endpoint, TransferDirection dir, byte[] data, int dataLength) throws UsbException{
		if(dir == TransferDirection.IN){
			OhciHcd.skipBulkEndpointIn();
			dataTd = new TransferDescriptor(TdType.IN, data, dataLength);
			dataTd.setBufferRounding();
			OhciHcd.enqueueTd(EndpointType.BULK_IN, dataTd);
			OhciHcd.resumeBulkEndpointIn();
		}
		else if(dir == TransferDirection.OUT){
			OhciHcd.skipBulkEndpointOut();
			dataTd = new TransferDescriptor(TdType.OUT, data, dataLength);
			OhciHcd.enqueueTd(EndpointType.BULK_OUT, dataTd);
			OhciHcd.resumeBulkEndpointOut();
		}
		else{
			throw new UsbException("invalid parameter in bulk transfer.");
		}
	}
	
	public boolean controlDone() throws UsbException{
		if(statusTd.done()){
			return true;
		}
		return false;
	}
}
