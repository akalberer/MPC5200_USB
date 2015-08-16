package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

/**
 * describes the type of endpoint
 * 
 * possible values are:
 * <li>{@link #CONTROL}</li>
 * <li>{@link #BULK_IN}</li>
 * <li>{@link #BULK_OUT}</li>
 * <li>{@link #INTERRUPT} (not supported at the moment)</li>
 * <li>{@link #ISOCHRONOUS} (not supported at the moment)</li>
 * 
 * @author Andreas Kalberer
 *
 */

public enum EndpointType {
	CONTROL,
	BULK_IN,
	BULK_OUT,
	INTERRUPT,
	ISOCHRONOUS
}
