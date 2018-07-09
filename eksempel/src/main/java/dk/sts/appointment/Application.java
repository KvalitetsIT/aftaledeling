package dk.sts.appointment;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import dk.s4.hl7.cda.codes.MedCom;
import dk.s4.hl7.cda.codes.NSI;
import dk.s4.hl7.cda.convert.APDXmlCodec;
import dk.s4.hl7.cda.model.apd.AppointmentDocument;
import dk.sts.appointment.configuration.ApplicationConfiguration;
import dk.sts.appointment.dto.DocumentMetadata;
import dk.sts.appointment.services.AppointmentXdsRequestService;


@Import({ApplicationConfiguration.class})
@EnableAutoConfiguration
public class Application implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
	
	private static SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyyMMddHHmmssZ");
	
	private static APDXmlCodec codec = new APDXmlCodec();
	
	@Autowired
	AppointmentXdsRequestService appointmentXdsRequestService;
	
	public static String PATIENT_ID = "2512489996";
	

	public static void main(String[] args) throws Exception {
		LOGGER.debug("Starting application");
		SpringApplicationBuilder sab = new SpringApplicationBuilder(Application.class);
		sab.web(false);
		sab.run(args);
	}	

	public void run(String... args) throws Exception {

		// Search documents for patient
		List<DocumentEntry> currentAppointments = appointmentXdsRequestService.getAllAppointmentsForPatient(PATIENT_ID);
		System.out.println("The patient with id="+PATIENT_ID+" has "+currentAppointments.size()+" registered in the XDS registry.");

		// Get appointment document from the first entry (if exits)
		if (currentAppointments.size() > 0 ) {
			DocumentEntry entry = currentAppointments.get(0);
			String uniqueUid = entry.getUniqueId();
			String repositoryId = entry.getRepositoryUniqueId();
			String homeCommunityId = entry.getHomeCommunityId();
			String document = appointmentXdsRequestService.fetchDocument(uniqueUid, repositoryId, homeCommunityId);
			System.out.println("The document: "+document);
		}
		
		
		// Register appointment (data extracted from the example file)
		String originalXmlDocument = getXmlFileContent("/DK-APD_Example_1.xml");
		
		// read the xml into an appointmentDocument with the parser
		AppointmentDocument appointmentDocument = codec.decode(originalXmlDocument);
		
		Date startAppointment = DATEFORMAT.parse("20170531110000+0100");
		Date endAppointment = DATEFORMAT.parse("20170531120000+0100");
		
		// change the time of the appointmentDocument 
		appointmentDocument.setDocumentationTimeInterval(startAppointment, endAppointment);
		DocumentMetadata metaData = createDocumentMetadata(appointmentDocument);
		
		String externalIdForNewDocument = generateUUID();
		
		//use the builder to output XML for the modified appointment
		String alteredXML = codec.encode(appointmentDocument);
		
		String documentId = appointmentXdsRequestService.createAndRegisterDocument(externalIdForNewDocument, alteredXML , metaData);
		System.out.println("We registered a new appointment with documentId="+documentId);

		// Search document for patient (we assume there is one more now)
		List<DocumentEntry> currentAppointmentsAfterNewAppointment = appointmentXdsRequestService.getAllAppointmentsForPatient(PATIENT_ID);
		boolean documentIsInList1 = isDocumentIdInList(documentId, currentAppointmentsAfterNewAppointment);
		System.out.println("The patient with id="+PATIENT_ID+" now has "+currentAppointmentsAfterNewAppointment.size()+" registered in the XDS registry after create. DocumentId:"+documentId+" "+(documentIsInList1 ? "could": "COULDN'T BUT SHOULD")+" be found.");

		Date restrictSearchEnd = DATEFORMAT.parse("20170531123000+0100");
		List<DocumentEntry> searchWithDateRestriction = appointmentXdsRequestService.getAllAppointmentsForPatient(PATIENT_ID, null, restrictSearchEnd);
		boolean isInListWhenRestrictingOnDate = isDocumentIdInList(documentId, searchWithDateRestriction);
		System.out.println("When searching with date restriction the documentId:"+documentId+" "+(isInListWhenRestrictingOnDate ? "could not": "CAN BUT SHOULDN'T")+" be found.");
		
		// Get appointment document from id
		String document = appointmentXdsRequestService.fetchDocument(externalIdForNewDocument);
		
		// Update the document with a new one
		DocumentEntry toBeUpdated = appointmentXdsRequestService.getAppointmentDocumentEntry(documentId);		
		
		// Use the parser to parse the response from the Xds into an appointmentDocument
		appointmentDocument = codec.decode(document);
				
		Date startUpdated = DATEFORMAT.parse("20170531120000+0100");
		Date endUpdated = DATEFORMAT.parse("20170531130000+0100");
		
		//We perform updates to the appointmentDocument - new time and new indication
		appointmentDocument.setDocumentationTimeInterval(startUpdated,endUpdated);
		appointmentDocument.setIndicationDisplayName("Undersøgelse af fod");
		
		//use the builder to output XML for the modified appointment
		alteredXML = codec.encode(appointmentDocument);
		
		DocumentMetadata updatedAppointmentCdaMetadata = createDocumentMetadata(appointmentDocument);
		String externalIdForUpdatedDocument = generateUUID();
		String newDocumentId = appointmentXdsRequestService.createAndRegisterDocumentAsReplacement(externalIdForUpdatedDocument, alteredXML, updatedAppointmentCdaMetadata, toBeUpdated.getEntryUuid());
		
		List<DocumentEntry> currentAppointmentsAfterUpdatedAppointment = appointmentXdsRequestService.getAllAppointmentsForPatient(PATIENT_ID);
		boolean couldFindOld = isDocumentIdInList(documentId, currentAppointmentsAfterUpdatedAppointment);
		boolean couldFindNew = isDocumentIdInList(newDocumentId, currentAppointmentsAfterUpdatedAppointment);
		System.out.println("The patient with id="+PATIENT_ID+" now has "+currentAppointmentsAfterUpdatedAppointment.size()+" registered in the XDS registry after update. The old DocumentId:"+documentId+" "+(couldFindOld ? "COULD BUT SHOULDN'T" : "fortunately could not")+" be found in search. The new DocumentId:"+newDocumentId+" "+(couldFindNew ? "could" : "COULDN'T BUT SHOULD")+" be found.");
		
		// Now we want to deprecate a document...but first we have to get the document entry from the registry
		DocumentEntry toBeDeprecated = appointmentXdsRequestService.getAppointmentDocumentEntry(newDocumentId);		
		// ... then deprecate
		appointmentXdsRequestService.deprecateDocument(toBeDeprecated);

		//Fremsøg aftaler for patienten
		List<DocumentEntry> currentAppointmentsAfterDeprecation = appointmentXdsRequestService.getAllAppointmentsForPatient(PATIENT_ID);
		System.out.println("The patient with id="+PATIENT_ID+" now has "+currentAppointmentsAfterDeprecation.size()+" registered in the XDS registry after deprecate.");

	}
		
	public DocumentMetadata createDocumentMetadata(AppointmentDocument apd) throws ParseException {
		DocumentMetadata appointmentCdaMetadata = new DocumentMetadata();
		appointmentCdaMetadata.setTitle(apd.getTitle());
		appointmentCdaMetadata.setPatientId(new Code(apd.getPatient().getId().getExtension(), new LocalizedString(apd.getPatient().getId().getAuthorityName()), apd.getPatient().getId().getRoot()));
		appointmentCdaMetadata.setReportTime(apd.getAuthor().getTime());
		appointmentCdaMetadata.setOrganisation(new Code(apd.getAuthor().getId().getExtension(), new LocalizedString(apd.getAuthor().getOrganizationIdentity().getOrgName()), NSI.SOR_OID));
		appointmentCdaMetadata.setClassCode(new Code("001", new LocalizedString("Klinisk rapport"), "1.2.208.184.100.9"));
		appointmentCdaMetadata.setFormatCode(new Code("urn:ad:dk:medcom:appointment", new LocalizedString("DK CDA APD"), MedCom.DK_APD_ROOT_OID));
		appointmentCdaMetadata.setHealthcareFacilityTypeCode(new Code("22232009", new LocalizedString("hospital") ,"2.16.840.1.113883.6.96"));
		appointmentCdaMetadata.setPracticeSettingCode(new Code("408443003", new LocalizedString("almen medicin"),"2.16.840.1.113883.6.96"));
		appointmentCdaMetadata.setSubmissionTime(new Date());
		appointmentCdaMetadata.setContentTypeCode(AppointmentConstants.APPOINTMENT_CODE);
		appointmentCdaMetadata.setTypeCode(AppointmentConstants.APPOINTMENT_CODE);
		appointmentCdaMetadata.setServiceStartTime(apd.getServiceStartTime());
		appointmentCdaMetadata.setServiceStopTime(apd.getServiceStopTime());
		return appointmentCdaMetadata;
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
	
	private String generateUUID() {
		java.util.UUID uuid = java.util.UUID.randomUUID();
		return Math.abs(uuid.getLeastSignificantBits()) + "." + Math.abs(uuid.getMostSignificantBits())+"."+Calendar.getInstance().getTimeInMillis();
	}

	private boolean isDocumentIdInList(String id, List<DocumentEntry> documentEntries) {
		for (DocumentEntry documentEntry : documentEntries) {
			if (documentEntry.getUniqueId().equals(id)) {
				return true;
			}
		}
		return false;
	}
}
