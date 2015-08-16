package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

import ch.ntb.inf.deep.runtime.mpc5200.driver.usb.exceptions.UsbException;
import ch.ntb.inf.deep.unsafe.US;

/**
 * represents memory structure of a TransferDescriptor linked to an EndpointDescriptor, possible types are listed here: {@link TdType}
 * 
 * @author Andreas Kalberer
 *
 */

public class TransferDescriptor {

	private int[] transferDescriptor;
	private TdType tdType;
	
	/**
	 * constructor for a TransferDescriptor structure
	 * @param type of TransferDescriptor, see {@link TdType}.
	 */
	public TransferDescriptor(TdType type){
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
			case EMPTY:
				transferDescriptor[2] = 0xF0000000;			// empty TD, only error field set
				break;
		}
	}
	
	/**
	 * constructor for new TransferDescriptorr with user data buffer
	 * @param type  	of TransferDescriptor, see {@link TdType}.
	 * @param buffer	user data buffer for transfer
	 * @param length	length of user data buffer
	 */
	public TransferDescriptor(TdType type, byte[] buffer, int length){
		this(type);
		if(buffer == null){				// no data buffer
			setEmptyUserBufferPointer();
		}
		else{
			setCurrentBufferPointer(buffer, length);
		}
		this.tdType = type;
	}
	
	/**
	 * set user data buffer pointer
	 * @param buffer	user data buffer for transfer
	 * @param length	length of user data buffer
	 */
	public void setCurrentBufferPointer(byte[] buffer, int length){
		transferDescriptor[3] = US.REF(buffer);
		transferDescriptor[5] = (US.REF(buffer) + (length - 1));
	}
	
	/**
	 * set data pointer as zero for TransferDescriptor (empty data)
	 */
	public void setEmptyUserBufferPointer(){
		transferDescriptor[3] = 0x00000000;
		transferDescriptor[5] = 0x00000000;
	}
	
	/**
	 * get type of TransferDescriptor
	 * @return TransferDescriptor type, see {@link TdType}
	 */
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
	
	/**
	 * set pointer to next TransferDescriptor in list
	 * @param address
	 */
	public void setNextTD(int address){
		transferDescriptor[4] = address;
	}
	
	/**
	 * get address of memory structure of TransferDescriptor
	 * @return	address of memory structure
	 */
	public int getTdAddress(){
		return (US.REF(transferDescriptor) + 8);
	}
	
	/**
	 * check if TransferDescriptor was already handled, can be checked on ConditionCode field in TransferDescriptor
	 * @return true: on success with no errors, false: else
	 * @throws UsbException
	 */
	public boolean done() throws UsbException{
		if( (transferDescriptor[2] & 0xF0000000) == 0){
			return true;
		}
		if( (transferDescriptor[2] & 0xF0000000) == 0xF0000000){
			return false;
		}
		// reserved condition codes
		if( ((transferDescriptor[2] & 0xF0000000) == 0xA0000000) || ((transferDescriptor[2] & 0xF0000000) == 0xB0000000)){
			return false;
		}
		return false;
		//TODO improve error handling -> check with done list, could be possible that one read could occur to not finished td
//		throw new UsbException("Error on ControlTransfer");
	}
	
	/**
	 * set buffer rounding for transfer<br> 
	 * If this is set, then the last data packet may be smaller than the defined buffer 
	 * without causing an error condition on the TransferDescriptor.
	 */
	public void setBufferRounding(){
		transferDescriptor[2] |= (1 << 18);
	}
	
	/**
	 * clear buffer rounding for transfer<br> 
	 * If this is cleared, then the last data packet to a TransferDescriptor from an endpoint must 
	 * exactly fill the defined data buffer, else an error condition on the TransferDescriptor is set.
	 */
	public void clearBufferRounding(){
		transferDescriptor[2] &= ~(1 << 18);
	}
}
