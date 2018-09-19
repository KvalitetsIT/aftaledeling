package dk.sds.dds;

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

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
import org.xml.sax.SAXException;

import dk.nsi.hsuid.OrganisationIdentifierAttribute;
import dk.nsi.hsuid._2016._08.hsuid_1_1.SubjectIdentifierType;
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
public class TrustOpgaveIntegrationTest extends AbstractTest {

	private static final String PATIENT_1_NO_CONSENT 												= "0101012504";
	private static final String PATIENT_2_DATA_SPECIFIKKE_NEGATIVE_SAMTYKKE 						= "0202022503";
	private static final String PATIENT_3_GENERELT_NEGATIVT_SAMTYKKE 								= "0303032503";
	private static final String PATIENT_4_NEGATIVT_SAMTYKKE_MOD_SUNDHEDSPERSON_GIVET_VED_CPR 		= "0404042503";
	
	private static final String REGISTRY_ERROR_CONSENT = "codeContext:urn:dk:nsi:Consent Filter Applied";

	private static final String LAKESIDE_CVR_NUMBER = "25450442";
	private static final String LAKESIDE_A_S = "LAKESIDE A/S";

	private static final String BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD = "Test1234";
	private static final String BRUGER_UDEN_AUTH_KEYSTORE_ALIAS = "grethe pedersen";

	private static final String BRUGER_MED_AUTH_KEYSTORE_PASSWORD = "Test1234";
	private static final String BRUGER_MED_AUTH_KEYSTORE_ALIAS = "casper rasmussen";

	@Autowired
	DgwsContext dgwsContext;


	OrganisationIdentifierAttribute organisationIdentifierAttribute;

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
	public void setup() throws ParseException, SAXException, IOException, ParserConfigurationException, TransformerException, XdsException {

		// Opretter testdata: Alle patienter har aftaledokumenter, men der er forskel i samtykkeopsætning
		//
		// PATIENT_1_NO_CONSENT: Ingen samtykkeopsæting
/*
		// PATIENT_2_DATA_SPECIFIKKE_NEGATIVE_SAMTYKKE: Dataspecifikt negativt samtykke
		createAftale("1279171926947241281.8952923385975766673.1533383220289", "Pedersen", PATIENT_2_DATA_SPECIFIKKE_NEGATIVE_SAMTYKKE);
		
		// PATIENT_3_GENERELT_NEGATIVT_SAMTYKKE: Generelt negativt samtykke
		createAftale("2279122926947241281.9152923385975799973.2333383220277", "Frandsen", PATIENT_3_GENERELT_NEGATIVT_SAMTYKKE);
		
		// PATIENT_4_NEGATIVT_SAMTYKKE_MOD_SUNDHEDSPERSON_GIVET_VED_CPR: Negativt samtykke mod specifik sundhedsperson
		createAftale("3379122926947241281.9352923385975799973.7733383220277", "Gunnarsen", PATIENT_4_NEGATIVT_SAMTYKKE_MOD_SUNDHEDSPERSON_GIVET_VED_CPR);
		
*/
		
		

		// testdata-færdig
		organisationIdentifierAttribute = new OrganisationIdentifierAttribute(LAKESIDE_CVR_NUMBER, SubjectIdentifierType.NSI_SORCODE.toString());

		brugerUdenAuthUserInfoKendtRolle = new UserInfo("1812292476", "Grethe", "Pedersen", "grethe@kuk.dk", "Test", "god-rolle", null);
		brugerUdenAuthUserInfoUkendtRolle = new UserInfo("1812292476", "Grethe", "Pedersen", "grethe@kuk.dk", "Test", "unknown-role", null);
		brugerUdenAuthCareProvider = new CareProvider(SubjectIdentifierTypeValues.CVR_NUMBER, LAKESIDE_CVR_NUMBER, LAKESIDE_A_S);

		brugerMedAuthUserInfo = new UserInfo("0804569723", "Casper", "Rasmussen", null, "Test", "læge", "CBNH1");
		brugerMedAuthCareProvider = new CareProvider(SubjectIdentifierTypeValues.CVR_NUMBER, LAKESIDE_CVR_NUMBER, LAKESIDE_A_S);

		dgwsContext.clearDgwsUserContext();

		PatientContext defaultPatientContext = new PatientContext(PATIENT_1_NO_CONSENT);
		dgwsContext.setPatientContext(defaultPatientContext);
	}

	/**
	 * Svarer til testcase 5
	 * 
	 * @throws IOException
	 * @throws XdsException
	 */
	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedLovligRolleOgTilladtDokumenttypeMenMedPatientMedDataSpecifikkeNegativeSamtykker() throws IOException, XdsException {

		// Given
		PatientContext patientMedDataSpecifikkeNegativeSamtykker = new PatientContext(PATIENT_2_DATA_SPECIFIKKE_NEGATIVE_SAMTYKKE);
		dgwsContext.setPatientContext(patientMedDataSpecifikkeNegativeSamtykker);
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoKendtRolle, brugerUdenAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = null;
		try {
			documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());
		} catch (XdsException e) {

		// Then
			List<String> errors = e.getErrors();
			Assert.assertEquals(1, errors.size());
			String error = errors.get(0);
			Assert.assertTrue("Tjekker at fejlen skyldtes samtykke", error.contains(REGISTRY_ERROR_CONSENT));
			return;
		}
		Assert.assertTrue("Regnede med en exception - regnede ikke med et svar", (documentEntries == null || documentEntries.size() == 0));
	}


	/**
	 * Svarer til testcase 7
	 * @throws IOException
	 */
	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedLovligRolleOgTilladtDokumenttypeMenMedPatientMedGenereltNegativtSamtykke() throws IOException  {

		// Given
		PatientContext patientMedNegativeSamtykker = new PatientContext(PATIENT_3_GENERELT_NEGATIVT_SAMTYKKE);
		dgwsContext.setPatientContext(patientMedNegativeSamtykker);

		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoKendtRolle, brugerUdenAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = null;
		try {
			documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());
		} catch (XdsException e) {
        
		// Then
			List<String> errors = e.getErrors();
			Assert.assertEquals(1, errors.size());
			String error = errors.get(0);
			Assert.assertTrue("Tjekker at fejlen skyldtes samtykke", error.contains(REGISTRY_ERROR_CONSENT));
			return;
		}
		Assert.assertTrue("Regnede med en exception - regnede ikke med et svar", (documentEntries == null || documentEntries.size() == 0));
	}

	/**
	 * Svarer til tescase 10
	 * @throws IOException
	 */
	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedLovligRolleOgTilladtDokumenttypeMenMedPatientMedGenereltNegativtSamtykkeModSpecifikSundhedsPerson() throws IOException  {

		// Given
		PatientContext patientMedNegativeSamtykkerModSundhedsPerson = new PatientContext(PATIENT_4_NEGATIVT_SAMTYKKE_MOD_SUNDHEDSPERSON_GIVET_VED_CPR);
		dgwsContext.setPatientContext(patientMedNegativeSamtykkerModSundhedsPerson);

		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoKendtRolle, brugerUdenAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = null;
		try {
			documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());
		} catch (XdsException e) {
        
		// Then
			List<String> errors = e.getErrors();
			Assert.assertEquals(1, errors.size());
			String error = errors.get(0);
			Assert.assertTrue("Tjekker at fejlen skyldtes samtykke", error.contains(REGISTRY_ERROR_CONSENT));
			return;
		}
		Assert.assertTrue("Regnede med en exception - regnede ikke med et svar", (documentEntries == null || documentEntries.size() == 0));
	}


	/**
	 * Testcase 1
	 * @throws IOException
	 * @throws XdsException
	 */
	@Test
	public void testFremsoegDokumenterMedAutoriseretBrugerMoces() throws IOException, XdsException {

		// Given
		dgwsContext.setDgwsUserContext(brugerMedAuthKeystore, BRUGER_MED_AUTH_KEYSTORE_PASSWORD, BRUGER_MED_AUTH_KEYSTORE_ALIAS, brugerMedAuthUserInfo, brugerMedAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getDocumentsForPatient(dgwsContext.getPatientContext().getPatientId());

		// Then
		Assert.assertTrue(documentEntries.size() > 0);
	}

	/**
	 * Testcase 4
	 * @throws IOException
	 * @throws XdsException
	 */
	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedUkendtRolle() throws IOException, XdsException {

		// Given
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoUkendtRolle, brugerUdenAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());

		// Then
		Assert.assertEquals(0, documentEntries.size());
	}

	/**
	 * Testcase 9
	 * @throws IOException
	 * @throws XdsException
	 */
	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedKendtRolleOgLovligDokumentType() throws IOException, XdsException {

		// Given
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoKendtRolle, brugerUdenAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());

		// Then
		Assert.assertEquals(0, documentEntries.size());
	}

	/**
	 * Testcase 6
	 * @throws IOException
	 * @throws XdsException
	 */
	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedKendtRolleMedMedUlovligDokumenttype() throws IOException, XdsException {

		// Given
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoKendtRolle, brugerUdenAuthCareProvider, organisationIdentifierAttribute, false);

		List<Code> ulovligDokumentType = new LinkedList<>();
		ulovligDokumentType.add(new Code(Loinc.PHMR_CODE, new LocalizedString(Loinc.PMHR_DISPLAYNAME), Loinc.OID));

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());

		// Then
		Assert.assertEquals(0, documentEntries.size());
	}

}
