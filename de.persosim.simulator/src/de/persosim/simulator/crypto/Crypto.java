package de.persosim.simulator.crypto;

import java.security.Provider;

import de.persosim.simulator.Activator;

/**
 * This class is intended to be used as source for the CryptoProvider in order
 * to allow using the code base as single source solution when porting to
 * Android. 
 * 
 * @author slutters
 * 
 */
public class Crypto {
	
	public static Provider getCryptoProvider() {
		return Activator.objectImplementingInterface.getCryptoProviderObject();
	}
	
}
