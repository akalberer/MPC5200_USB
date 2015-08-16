package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

/**
 * represents the type of the TransferDescriptor<br>
 * possible values are:
 * <li> {@link #SETUP}</li>
 * <li> {@link #OUT}</li>
 * <li> {@link #IN}</li>
 * <li> {@link #STATUS_IN}</li>
 * <li> {@link #STATUS_OUT}</li>
 * <li> {@link #EMPTY}</li>
 * 
 * @author Andreas Kalberer
 *
 */

public enum TdType {
	SETUP,
	OUT,
	IN,
	STATUS_IN,
	STATUS_OUT,
	EMPTY
}
