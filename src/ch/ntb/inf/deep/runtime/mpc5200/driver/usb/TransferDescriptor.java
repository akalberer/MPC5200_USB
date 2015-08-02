package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.unsafe.US;

public class TransferDescriptor {

	private int[] transferDescriptor;
	private TdType tdType;
	
	public TransferDescriptor(){
		this.transferDescriptor = new int[6];
	}
	
	public TransferDescriptor(TdType type, byte[] buffer, int length){
		this.transferDescriptor = new int[6];
		switch(type){
			case SETUP:
				transferDescriptor[2] = 0xF2E00000;			// setup always DATA0
				break;
			case OUT:
				transferDescriptor[2] = 0xF0E80000;			// set DATA0/1 at enqueue to list
				break;
			case IN:
				transferDescriptor[2] = 0xF0F00000;			// set DATA0/1 at enqueue to list
				break;
			case STATUS_IN:
				transferDescriptor[2] = 0xF3F00000;			// status always DATA1
				break;
			case STATUS_OUT:
				transferDescriptor[2] = 0xF3E80000;			// status always DATA1
				break;
				
		}
		if((buffer == null) && (length == 0)){				// empty data buffer
			setEmptyUserBufferPointer();
		}
		else{
			setCurrentBufferPointer(buffer, length);
		}
		this.tdType = type;
	}
	
	public void setCurrentBufferPointer(byte[] buffer, int length){
		transferDescriptor[3] = US.REF(buffer);
		transferDescriptor[5] = (US.REF(buffer) + (length - 1));
	}
	
	public void setEmptyUserBufferPointer(){
		transferDescriptor[3] = 0x00000000;
		transferDescriptor[5] = 0x00000000;
	}
	
	public TdType getType(){
		return tdType;
	}

	/**
	 * set Data Toggle
	 * @param toggle false: DATA0; true: DATA1
	 */
	public void setDataToggle(boolean toggle) {
		if(!toggle){
			transferDescriptor[2] |= 0x02000000;
		}
		else{
			transferDescriptor[2] |= 0x03000000;
		}
	}
	
	public void setNextTD(int address){
		transferDescriptor[4] = address;
	}
	
	public int getTdAddress(){
		return (US.REF(transferDescriptor) + 8);
	}
}
