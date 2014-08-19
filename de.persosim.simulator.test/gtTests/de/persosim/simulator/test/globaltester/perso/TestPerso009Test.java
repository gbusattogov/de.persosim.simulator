package de.persosim.simulator.test.globaltester.perso;

import java.util.ArrayList;
import java.util.Collection;

import de.persosim.simulator.perso.DefaultPersoTestPkiTemplate09;
import de.persosim.simulator.perso.Personalization;
import de.persosim.simulator.test.globaltester.GtConstants;
import de.persosim.simulator.test.globaltester.GtSuiteDescriptor;
import de.persosim.simulator.test.globaltester.JobDescriptor;

public class TestPerso009Test extends GtDefaultPersoTest {

	@Override
	public Personalization getPersonalization() {
		if(persoCache == null) {
			persoCache = new DefaultPersoTestPkiTemplate09();
		}
		
		return persoCache;
	}
	
	@Override
	public Collection<JobDescriptor> getAllApplicableGtTests() {
		Collection<JobDescriptor> retVal = 
		new ArrayList<JobDescriptor>();
		
		retVal.add(new GtSuiteDescriptor(GtConstants.PROJECT_EPA_EAC2_BSI, "EAC2_ISO7816_H_04a"));

		return retVal;
	}
	
}
