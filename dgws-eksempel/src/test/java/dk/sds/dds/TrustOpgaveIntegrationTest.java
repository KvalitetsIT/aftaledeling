package dk.sds.dds;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import dk.s4.hl7.cda.codes.Loinc;
import dk.sds.dgws.DgwsContext;
import dk.sosi.seal.model.CareProvider;
import dk.sosi.seal.model.UserInfo;
import dk.sosi.seal.model.constants.SubjectIdentifierTypeValues;
import dk.sts.appointment.configuration.PatientContext;
import dk.sts.appointment.services.AppointmentXdsRequestService;
import dk.sts.appointment.services.XdsException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestConfiguration.class }, loader = AnnotationConfigContextLoader.class)
public class TrustOpgaveIntegrationTest {

	private static final String BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD = "Test1234";
	private static final String BRUGER_UDEN_AUTH_KEYSTORE_ALIAS = "grethe pedersen";

	private static final String BRUGER_MED_AUTH_KEYSTORE_PASSWORD = "Test1234";
	private static final String BRUGER_MED_AUTH_KEYSTORE_ALIAS = "casper rasmussen";

	@Autowired
	DgwsContext dgwsContext;

	@Value("classpath:/uden.jks")
	private Resource brugerUdenAuthKeystore;
	private UserInfo brugerUdenAuthUserInfoKendtRolle;
	private UserInfo brugerUdenAuthUserInfoUkendtRolle;
	private CareProvider brugerUdenAuthCareProvider;
	
	@Value("classpath:/med.jks")
	private Resource brugerMedAuthKeystore;
	private UserInfo brugerMedAuthUserInfo;
	private CareProvider brugerMedAuthCareProvider;

	@Autowired
	AppointmentXdsRequestService appointmentXdsRequestService;

	@Before
	public void setup() {
		
		brugerUdenAuthUserInfoKendtRolle = new UserInfo("1812292476", "Grethe", "Pedersen", "grethe@kuk.dk", "Test", "god-rolle", null);
		brugerUdenAuthUserInfoUkendtRolle = new UserInfo("1812292476", "Grethe", "Pedersen", "grethe@kuk.dk", "Test", "unknown-role", null);
		brugerUdenAuthCareProvider = new CareProvider(SubjectIdentifierTypeValues.CVR_NUMBER, "25450442", "LAKESIDE A/S");
		
		brugerMedAuthUserInfo = new UserInfo("0804569723", "Casper", "Rasmussen", null, "Test", "læge", "CBNH1");
		brugerMedAuthCareProvider = new CareProvider(SubjectIdentifierTypeValues.CVR_NUMBER, "25450442", "LAKESIDE A/S");
		
		dgwsContext.clearDgwsUserContext();
		
		PatientContext patientContext = new PatientContext("0303032504");
		dgwsContext.setPatientContext(patientContext);

	}
	
	@Test
	public void testFremsoegDokumenterMedAutoriseretBrugerMoces() throws IOException, XdsException {
	
		// Given
		dgwsContext.setDgwsUserContext(brugerMedAuthKeystore, BRUGER_MED_AUTH_KEYSTORE_PASSWORD, BRUGER_MED_AUTH_KEYSTORE_ALIAS, brugerMedAuthUserInfo, brugerMedAuthCareProvider, true);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getDocumentsForPatient(dgwsContext.getPatientContext().getPatientId());
		
		// Then
		Assert.assertTrue(documentEntries.size() > 0);
	}

	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedUkendtRolle() throws IOException, XdsException {
	
		// Given
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoUkendtRolle, brugerUdenAuthCareProvider, false);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());
		
		// Then
		Assert.assertEquals(0, documentEntries.size());
	}

	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedKendtRolleOgLovligDokumentType() throws IOException, XdsException {
	
		// Given
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoKendtRolle, brugerUdenAuthCareProvider, false);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());
		
		// Then
		Assert.assertEquals(0, documentEntries.size());
	}

	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedKendtRolleMedMedUlovligDokumenttype() throws IOException, XdsException {
	
		// Given
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoKendtRolle, brugerUdenAuthCareProvider, false);
		List<Code> ulovligDokumentType = new LinkedList<>();
		ulovligDokumentType.add(new Code(Loinc.PHMR_CODE, new LocalizedString(Loinc.PMHR_DISPLAYNAME), Loinc.OID));

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getDocumentsForPatient(dgwsContext.getPatientContext().getPatientId(), ulovligDokumentType, null, null);
		
		// Then
		Assert.assertEquals(0, documentEntries.size());
	}

}
