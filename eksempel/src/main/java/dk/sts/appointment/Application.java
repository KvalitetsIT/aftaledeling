package dk.sts.appointment;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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

import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import dk.s4.hl7.cda.convert.APDXmlCodec;
import dk.s4.hl7.cda.model.apd.AppointmentDocument;
import dk.sts.appointment.configuration.ApplicationConfiguration;
import dk.sts.appointment.configuration.UserContext;
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
	
	@Autowired
	UserContext userContext;

	public static void main(String[] args) throws Exception {
		LOGGER.debug("Starting application");
		SpringApplicationBuilder sab = new SpringApplicationBuilder(Application.class);
		sab.web(false);
		sab.run(args);
	}	

	public void run(String... args) throws Exception {

		// Search documents for patient
		List<DocumentEntry> currentAppointments = appointmentXdsRequestService.getAllAppointmentsForPatient(userContext.getPatientId());
		System.out.println("The patient with id="+userContext.getPatientId()+" has "+currentAppointments.size()+" registered in the XDS registry.");

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
		DocumentMetadata metaData = appointmentXdsRequestService.createDocumentMetadata(appointmentDocument);
		
		String externalIdForNewDocument = generateUUID();
		
		//use the builder to output XML for the modified appointment
		String alteredXML = codec.encode(appointmentDocument);
		
		String documentId = appointmentXdsRequestService.createAndRegisterDocument(externalIdForNewDocument, alteredXML , metaData);
		System.out.println("We registered a new appointment with documentId="+documentId);

		// Search document for patient (we assume there is one more now)
		List<DocumentEntry> currentAppointmentsAfterNewAppointment = appointmentXdsRequestService.getAllAppointmentsForPatient(userContext.getPatientId());
		boolean documentIsInList1 = isDocumentIdInList(documentId, currentAppointmentsAfterNewAppointment);
		System.out.println("The patient with id="+userContext.getPatientId()+" now has "+currentAppointmentsAfterNewAppointment.size()+" registered in the XDS registry after create. DocumentId:"+documentId+" "+(documentIsInList1 ? "could": "COULDN'T BUT SHOULD")+" be found.");

		Date restrictSearchEnd = DATEFORMAT.parse("20170531123000+0100");
		List<DocumentEntry> searchWithDateRestriction = appointmentXdsRequestService.getAllAppointmentsForPatient(userContext.getPatientId(), null, restrictSearchEnd);
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
		
		DocumentMetadata updatedAppointmentCdaMetadata = appointmentXdsRequestService.createDocumentMetadata(appointmentDocument);
		String externalIdForUpdatedDocument = generateUUID();
		String newDocumentId = appointmentXdsRequestService.createAndRegisterDocumentAsReplacement(externalIdForUpdatedDocument, alteredXML, updatedAppointmentCdaMetadata, toBeUpdated.getEntryUuid());
		
		List<DocumentEntry> currentAppointmentsAfterUpdatedAppointment = appointmentXdsRequestService.getAllAppointmentsForPatient(userContext.getPatientId());
		boolean couldFindOld = isDocumentIdInList(documentId, currentAppointmentsAfterUpdatedAppointment);
		boolean couldFindNew = isDocumentIdInList(newDocumentId, currentAppointmentsAfterUpdatedAppointment);
		System.out.println("The patient with id="+userContext.getPatientId()+" now has "+currentAppointmentsAfterUpdatedAppointment.size()+" registered in the XDS registry after update. The old DocumentId:"+documentId+" "+(couldFindOld ? "COULD BUT SHOULDN'T" : "fortunately could not")+" be found in search. The new DocumentId:"+newDocumentId+" "+(couldFindNew ? "could" : "COULDN'T BUT SHOULD")+" be found.");
		
		// Now we want to deprecate a document...but first we have to get the document entry from the registry
		DocumentEntry toBeDeprecated = appointmentXdsRequestService.getAppointmentDocumentEntry(newDocumentId);		
		// ... then deprecate
		appointmentXdsRequestService.deprecateDocument(toBeDeprecated);

		//Fremsøg aftaler for patienten
		List<DocumentEntry> currentAppointmentsAfterDeprecation = appointmentXdsRequestService.getAllAppointmentsForPatient(userContext.getPatientId());
		System.out.println("The patient with id="+userContext.getPatientId()+" now has "+currentAppointmentsAfterDeprecation.size()+" registered in the XDS registry after deprecate.");

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
