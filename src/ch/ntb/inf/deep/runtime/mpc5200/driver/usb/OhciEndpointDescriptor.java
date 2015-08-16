package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.unsafe.US;

/**
 * <b>OhciEndpointDescriptor</b><br>
 * Represents the memory structure needed for an EndpointDescriptor, that will be processed by the Host Controller during operation
 * 
 * @author Andreas Kalberer
 */

public class OhciEndpointDescriptor {
	
	private int[] endpointDesc;
	private EndpointType epType;
	
	private static int usbDevAddress = 0;		// address of USB-Device after enumeration
	
	public OhciEndpointDescriptor(EndpointType epType) throws UsbException{
		this.endpointDesc = new int[6];
		if( (epType == EndpointType.INTERRUPT) || (epType == EndpointType.ISOCHRONOUS) ){
			throw new UsbException("EndpointType not supported");
		}
		this.epType = epType;
	}
	
	public OhciEndpointDescriptor(EndpointType epType, int maxPacketSize) throws UsbException{
		this(epType);
		
		for(int i = 0; i < endpointDesc.length; i++){		// just for paranoia
			endpointDesc[i] = 0;
		}
		
		setMaxPacketSize(maxPacketSize);
	}
	
	/**
	 * set maximum packet size of endpoint
	 * @param maxPacketSize
	 * @throws UsbException
	 */
	public void setMaxPacketSize(int maxPacketSize) throws UsbException{
		if(maxPacketSize > 0x7FF){
			throw new UsbException("maxPacketSize too big.");
		}
		endpointDesc[2] &= ~(0x07FF0000);				// clear current maxPacketSize
		endpointDesc[2] |= (maxPacketSize << 16);
	}
	
	public int getMaxPacketSize(){
		return ((endpointDesc[2] & 0x07FF0000) >> 16);
	}
	
	/**
	 * set skip bit of endpoint descriptor
	 * @param set true: skip endpoint, false: clear skip bit
	 */
	public void setSkipBit(boolean set){
		if(set){
			endpointDesc[2] |= (1 << 14);
		}
		else{
			endpointDesc[2] &= ~(1 << 14);
		}
	}
	
	/**
	 * set skip bit of endpoint descriptor
	 */
	public void setSkipBit(){
		setSkipBit(true);
	}
	
	/**
	 * clear skip bit of endpoint descriptor
	 */
	public void clearSkipBit(){
		setSkipBit(false);
	}
	
	/**
	 * set USB device address
	 * @param address		desired and configured address with SET_ADDRESS Request
	 * @throws UsbException	on failure
	 */
	public void setUsbDevAddress(int address) throws UsbException{
		if(address > 127 || address <= 0){
			throw new UsbException("wrong usb address.");
		}
		
		endpointDesc[2] &= ~0x0000007F;		// clear current usb device address
		endpointDesc[2] |= address;			// set new address
		usbDevAddress = address;
	}
	
	/**
	 * get USB device address
	 * @return device address this endpoint is configured to
	 */
	public int getUsbDevAddress(){
		return usbDevAddress;
	}
	
	/**
	 * set endpoint that should be used
	 * @param endpoint			desired endpoint
	 * @throws UsbException		on failure
	 */
	public void setEndpoint(int endpoint) throws UsbException{
		if(endpoint < 0 || endpoint > 0xF){
			throw new UsbException("invalid endpoint.");
		}
		endpointDesc[2] &= ~0x00000780;		// clear current endpoint number
		endpointDesc[2] |= (endpoint << 7);	// set new endpoint number
	}
	
	/**
	 * get the endpoint number this EndpointDescriptor uses
	 * @return endpoint number
	 */
	public int getEndpoint(){
		return ((endpointDesc[2] & 0x00000780) >> 7);
	}
	
	/**
	 * set tail pointer of TransferDescriptor list linked to this endpoint
	 * @param address address of tail TransferDescriptor
	 */
	public void setTdTailPointer(int address){
		endpointDesc[3] = address;
	}
	
	/**
	 * get tail pointer of TransferDescriptor list linked to this endpoint
	 * @return pointer to tail TransferDescriptor
	 */
	public int getTdTailPointer(){
		return endpointDesc[3];
	}
	
	/**
	 * set head pointer of TransferDescriptor list linked to this endpoint
	 * @param address address of head TransferDescriptor
	 */
	public void setTdHeadPointer(int address){
		endpointDesc[4] = (address & 0xFFFFFFF0);
	}
	
	/**
	 * get head pointer of TransferDescriptor list linked to this endpoint
	 * @return pointer to head TransferDescriptor
	 */
	public int getTdHeadPointer(){
		return (endpointDesc[4] & 0xFFFFFFF0);
	}
	
	/**
	 * set next EndpointDescriptor
	 * @param address address of next EndpointDescriptor that should be linked to this EndpointDescriptor
	 */
	public void setNextEndpointDescriptor(int address){
		endpointDesc[5] = address;
	}
	
	/**
	 * get next EndpointDescriptor
	 * @return address of next EndpointDescriptor linked to this EndpointDescriptor
	 */
	public int getNextEndpointDescriptor(){
		return endpointDesc[5];
	}
	
	/**
	 * get address of memory structure that needs to be passed to HostController or used for linking to this EndpointDescriptor
	 * @return address of EndpointDescriptor structure in memory
	 */
	public int getEndpointDescriptorAddress(){
		return (US.REF(endpointDesc) + 8);
	}
	
	/**
	 * get type of endpoint
	 * @return {@link EndpointType}
	 */
	public EndpointType getEndpointType(){
		return epType;
	}
}
