package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.unsafe.US;

public class OhciEndpointDescriptor {
	
	private int[] endpointDesc;
	
	public OhciEndpointDescriptor(){
		this.endpointDesc = new int[6];
	}
	
	public OhciEndpointDescriptor(int maxPacketSize) throws UsbException{
		this();
		setMaxPacketSize(maxPacketSize);
		for(int i = 0; i < endpointDesc.length; i++){		// just for paranoia
			endpointDesc[i] = 0;
		}
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
	
	public void setSkipBit(){
		setSkipBit(true);
	}
	
	public void clearSkipBit(){
		setSkipBit(false);
	}
	
	public void setUsbDevAddress(int address) throws UsbException{
		if(address > 127 || address <= 0){
			throw new UsbException("wrong usb address.");
		}
		
		endpointDesc[2] &= ~0x0000007F;		// clear current usb device address
		endpointDesc[2] |= address;			// set new address
	}
	
	public void setTdTailPointer(int address){
		endpointDesc[3] = address;
	}
	
	public int getTdTailPointer(){
		return endpointDesc[3];
	}
	
	public void setTdHeadPointer(int address){
		endpointDesc[4] = (address & 0xFFFFFFF0);
	}
	
	public int getTdHeadPointer(){
		return (endpointDesc[4] & 0xFFFFFFF0);
	}
	public void setNextEndpointDescriptor(int address){
		endpointDesc[5] = address;
	}
	
	public int getNextEndpointDescriptor(){
		return endpointDesc[5];
	}
	
	public int getEndpointDescriptorAddress(){
		return (US.REF(endpointDesc) + 8);
	}
}
