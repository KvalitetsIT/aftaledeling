package dk.sds.dds;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import dk.nsi.hsuid.OrganisationIdentifierAttribute;
import dk.nsi.hsuid._2016._08.hsuid_1_1.SubjectIdentifierType;
import dk.s4.hl7.cda.codes.Loinc;
import dk.s4.hl7.cda.codes.MedCom;
import dk.s4.hl7.cda.convert.APDXmlCodec;
import dk.s4.hl7.cda.model.AddressData;
import dk.s4.hl7.cda.model.OrganizationIdentity;
import dk.s4.hl7.cda.model.Participant;
import dk.s4.hl7.cda.model.Patient;
import dk.s4.hl7.cda.model.PersonIdentity;
import dk.s4.hl7.cda.model.apd.AppointmentDocument;
import dk.s4.hl7.cda.model.apd.AppointmentDocument.Status;
import dk.s4.hl7.cda.model.util.DateUtil;
import dk.sds.dgws.DgwsContext;
import dk.sosi.seal.model.CareProvider;
import dk.sosi.seal.model.UserInfo;
import dk.sosi.seal.model.constants.SubjectIdentifierTypeValues;
import dk.sts.appointment.Application;
import dk.sts.appointment.configuration.PatientContext;
import dk.sts.appointment.dto.DocumentMetadata;
import dk.sts.appointment.services.AppointmentXdsRequestService;
import dk.sts.appointment.services.XdsException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestConfiguration.class }, loader = AnnotationConfigContextLoader.class)
public class TrustOpgaveIntegrationTest {

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

		// PATIENT_2_DATA_SPECIFIKKE_NEGATIVE_SAMTYKKE: Dataspecifikt negativt samtykke
/*		createAftale("1279171926947241281.8952923385975766673.1533383220289", "Pedersen", PATIENT_2_DATA_SPECIFIKKE_NEGATIVE_SAMTYKKE);
		
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


	@Test
	public void testFremsoegDokumenterMedAutoriseretBrugerMoces() throws IOException, XdsException {

		// Given
		dgwsContext.setDgwsUserContext(brugerMedAuthKeystore, BRUGER_MED_AUTH_KEYSTORE_PASSWORD, BRUGER_MED_AUTH_KEYSTORE_ALIAS, brugerMedAuthUserInfo, brugerMedAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getDocumentsForPatient(dgwsContext.getPatientContext().getPatientId());

		// Then
		Assert.assertTrue(documentEntries.size() > 0);
	}

	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedUkendtRolle() throws IOException, XdsException {

		// Given
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoUkendtRolle, brugerUdenAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());

		// Then
		Assert.assertEquals(0, documentEntries.size());
	}

	@Test
	public void testFremsoegDokumenterMedIkkeAutoriseretBrugerMedKendtRolleOgLovligDokumentType() throws IOException, XdsException {

		// Given
		dgwsContext.setDgwsUserContext(brugerUdenAuthKeystore, BRUGER_UDEN_AUTH_KEYSTORE_PASSWORD, BRUGER_UDEN_AUTH_KEYSTORE_ALIAS, brugerUdenAuthUserInfoKendtRolle, brugerUdenAuthCareProvider, organisationIdentifierAttribute, false);

		// When
		List<DocumentEntry> documentEntries = appointmentXdsRequestService.getAllAppointmentsForPatient(dgwsContext.getPatientContext().getPatientId());

		// Then
		Assert.assertEquals(0, documentEntries.size());
	}

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


	public String createAftale(String uuid, String name, String cpr) throws ParseException, SAXException, IOException, ParserConfigurationException, TransformerException, XdsException {
		APDXmlCodec codec = new APDXmlCodec();
		
		// Define the 'time'
		Date documentCreationTime = DateUtil.makeDanishDateTime(2017, 0, 13, 10, 0, 0);
		Date authorTime = DateUtil.makeDanishDateTime(2017, 1, 16, 10, 0, 0);
		// Create document
		AppointmentDocument appointment = new AppointmentDocument(MedCom.createId(uuid));
		appointment.setLanguageCode("da-DK");
		appointment.setTitle("Aftale for "+cpr);
		appointment.setEffectiveTime(documentCreationTime);
		// Create Patient
		Patient patient = definePersonIdentity(name, cpr);
		appointment.setPatient(patient);
		// Create Custodian organization
		OrganizationIdentity custodianOrganization = new OrganizationIdentity.OrganizationBuilder()
				.setSOR("515361000016007")
				.setName("OUH Klinisk IT (Odense)")
				.build();
		appointment.setCustodian(custodianOrganization);

		OrganizationIdentity authorOrganization = new OrganizationIdentity.OrganizationBuilder()
				.setSOR("242621000016001")
				.setName("OUH Radiologisk Afdeling (Svendborg)")
				.build();

		PersonIdentity jensJensen = new PersonIdentity.PersonBuilder("Jensen").addGivenName("Jens").setPrefix("Læge").build();
		PersonIdentity andersAndersen = new PersonIdentity.PersonBuilder("Andersen").addGivenName("Anders").build();
		PersonIdentity laegeAndersAndersen = new PersonIdentity.PersonBuilder("Andersen").addGivenName("Anders").setPrefix("Læge").build();

		appointment.setAuthor(new Participant.ParticipantBuilder()
				.setSOR(authorOrganization.getIdValue())
				.setTime(authorTime)
				.setPersonIdentity(jensJensen)
				.setOrganizationIdentity(authorOrganization)
				.build());

		// 1.4 Define the service period       
		Date from = DateUtil.makeDanishDateTime(2017, 4, 31, 11, 0, 0);
		Date to = DateUtil.makeDanishDateTime(2017, 4, 31, 12, 0, 0);

		appointment.setDocumentationTimeInterval(from, to);

		OrganizationIdentity appointmentLocation = new OrganizationIdentity.OrganizationBuilder()
				.setSOR("320161000016005")
				.setName("OUH Radiologisk Ambulatorium (Nyborg)")
				.build();

		appointment.setAppointmentTitle("Aftale");
		appointment.setAppointmentId(uuid);
		appointment.setAppointmentStatus(Status.ACTIVE);
		appointment.setAppointmentText("text");
		appointment.setIndicationDisplayName("Ekkokardiografi (Ultralydsundersøgelse af hjertet)");
		appointment.setAppointmentLocation(appointmentLocation);

		Participant appointmentAuthor = new Participant.ParticipantBuilder()
				.setSOR("48681000016007")
				.setTime(authorTime)
				.setPersonIdentity(andersAndersen)
				.setOrganizationIdentity(new OrganizationIdentity.OrganizationBuilder().setName("Lægerne Toldbodvej").build())
				.build();

		appointment.setAppointmentAuthor(appointmentAuthor);

		Participant appointmentPerformer = new Participant.ParticipantBuilder()
				.setSOR("320161000016005")
				.setTime(authorTime)
				.setPersonIdentity(laegeAndersAndersen)
				.setOrganizationIdentity(
						new OrganizationIdentity.OrganizationBuilder().setName("OUH Radiologisk Ambulatorium (Nyborg)").build())
				.build();

		appointment.setAppointmentPerformer(appointmentPerformer);

		appointment.setConfidentialityCode("N");

		DocumentMetadata metaData = appointmentXdsRequestService.createDocumentMetadata(appointment);
		//use the builder to output XML for the modified appointment
		String xml = codec.encode(appointment);
		
		String documentId = appointmentXdsRequestService.createAndRegisterDocument(uuid, xml, metaData);
		return documentId;
	}

	public String getXmlFileContent(String resource) throws SAXException, IOException, ParserConfigurationException, TransformerException {
		InputStream is = Application.class.getResourceAsStream(resource);
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(is);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
		String output = writer.getBuffer().toString();
		String result = output;
		return result;
	}

	public static AddressData defineValdemarsGade53Address() {
		AddressData valdemarsGade53 = new AddressData.AddressBuilder("5700", "Svendborg")
				.addAddressLine("Valdemarsgade 53")
				.setCountry("Danmark")
				.setUse(AddressData.Use.WorkPlace)
				.build();
		return valdemarsGade53;
	}

	public static Patient definePersonIdentity(String name, String cpr) {

		Patient nancy = new Patient.PatientBuilder(name)
				.setSSN(cpr)
				.build();

		return nancy;
	}
	
	private String generateUUID() {
		java.util.UUID uuid = java.util.UUID.randomUUID();
		return Math.abs(uuid.getLeastSignificantBits()) + "." + Math.abs(uuid.getMostSignificantBits())+"."+Calendar.getInstance().getTimeInMillis();
	}

}
