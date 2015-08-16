package ch.ntb.inf.deep.runtime.mpc5200.driver.usb;

/**
 * OHCI states for OHCI host controller
 * 
 * <li>{@link #OHCI_RH_HALTED}</li>
 * <li>{@link #OHCI_RH_SUSPENDED}</li>
 * <li>{@link #OHCI_RH_RUNNING}</li>
 * 
 * @author Andreas Kalberer
 *
 */

public enum OhciState {
	OHCI_RH_HALTED,
	OHCI_RH_SUSPENDED,
	OHCI_RH_RUNNING
}
