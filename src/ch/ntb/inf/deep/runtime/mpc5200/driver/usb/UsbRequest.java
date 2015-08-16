package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.EndpointType;

/**
 * used to create an USB-Request, sets up the user data for standard requests and creates the needed TransferDescriptors
 * 
 * @author Andreas Kalberer
 *
 */

public class UsbRequest {

	TransferDescriptor setupTd;
	TransferDescriptor dataTd;
	TransferDescriptor statusTd;
	
	/**
	 * user data for get device descriptor standard-request, used during enumeration,
	 * read only the first 8 bytes with maximum packet size 8 byte (every USB device must support that)
	 */
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
	
	/**
	 * user data for complete get device descriptor standard-request
	 */
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
	
	/**
	 * user data for SET_ADDRESS standard-request
	 */
	private byte[] setupSetAddress = new byte[]{
		(byte) 0x00,		// bRequestType: SET_ADDRESS
		(byte) 0x05,		// bRequest
		(byte) 0x00,		// new device Address low byte
		(byte) 0x00,		// new device Address empty
		(byte) 0x00,		// no index
		(byte) 0x00,
		(byte) 0x00,		// length = 0
		(byte) 0x00
	};
	
	/**
	 * user data for SET_CONFIGURATION standard-request
	 */
	private byte[] setupSetConfiguration = new byte[]{
		(byte) 0x00,		// bRequestType
		(byte) 0x09,		// bRequest
		(byte) 0x00,		// ID of desired config
		(byte) 0x00,		// high byte of desired config -> reserved -> leave blank
		(byte) 0x00,		// index 0
		(byte) 0x00,
		(byte) 0x00,		// length = 0
		(byte) 0x00
	};
	
	/**
	 * user data for SET_INTERFACE standard-request
	 */
	private byte[] setupSetInterface = new byte[]{
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
	
	/**
	 * get device descriptor standard-request
	 * @param data	device descriptor data returned by request
	 * @throws UsbException on failure
	 */
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
	
	/**
	 * get device descriptor standard-request used during enumeration (endpoint has maximum packet size of 8 bytes)
	 * @param data	device descriptor data returned by request
	 * @throws UsbException on failure
	 */
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
	
	/**
	 * set device address standard-request
	 * @param address	desired device address
	 * @throws UsbException on failure
	 */
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
	
	/**
	 * set configuration standard-request
	 * @param configValue	desired configuration value to set on the device
	 * @throws UsbException on failure
	 */
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
	
	/**
	 * set interface standard-request
	 * @param ifaceNum		desired interface number
	 * @param altSetting	alternate setting id
	 * @throws UsbException	on failure
	 */
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
	
	/**
	 * bulk transfer request
	 * @param endpoint		desired endpoint to communicate with the device
	 * @param dir			transfer direction (see {@link TransferDirection})
	 * @param data			user data to send on OUT-, or read buffer on IN-transfer
	 * @param dataLength	data length or buffer size
	 * @throws UsbException	on failure
	 */
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
	
	/**
	 * check if control transfer is finished
	 * @return true: if done, false: else
	 * @throws UsbException on failure
	 */
	public boolean controlDone() throws UsbException{
		if(statusTd.done()){
			return true;
		}
		return false;
	}
}
