package dk.sds.dds;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
import dk.sts.appointment.Application;
import dk.sts.appointment.dto.DocumentMetadata;
import dk.sts.appointment.services.AppointmentXdsRequestService;
import dk.sts.appointment.services.XdsException;

public class AbstractTest {


	public String createAftale(AppointmentXdsRequestService axrs, String uuid, String name, String cpr, String orgSorCode, String orgName) throws ParseException, SAXException, IOException, ParserConfigurationException, TransformerException, XdsException {
		return createAftale(axrs, uuid, name, cpr, null, orgSorCode, orgName);
	}
	public String createAftale(AppointmentXdsRequestService axrs, String uuid, String name, String cpr, Code contentTypeCode, String orgSorCode, String orgName) throws ParseException, SAXException, IOException, ParserConfigurationException, TransformerException, XdsException {
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
				.setSOR(orgSorCode)
				.setName(orgName)
				.build();
		appointment.setCustodian(custodianOrganization);

		OrganizationIdentity authorOrganization = new OrganizationIdentity.OrganizationBuilder()
				.setSOR(orgSorCode)
				.setName(orgName)
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
				.setSOR(orgSorCode)
				.setName(orgName)
				.build();

		appointment.setAppointmentTitle("Aftale");
		appointment.setAppointmentId(uuid);
		appointment.setAppointmentStatus(Status.ACTIVE);
		appointment.setAppointmentText("text");
		appointment.setIndicationDisplayName("Ekkokardiografi (Ultralydsundersøgelse af hjertet)");
		appointment.setAppointmentLocation(appointmentLocation);

		Participant appointmentAuthor = new Participant.ParticipantBuilder()
				.setSOR(orgSorCode)
				.setTime(authorTime)
				.setPersonIdentity(andersAndersen)
				.setOrganizationIdentity(new OrganizationIdentity.OrganizationBuilder().setName(orgName).build())
				.build();

		appointment.setAppointmentAuthor(appointmentAuthor);

		Participant appointmentPerformer = new Participant.ParticipantBuilder()
				.setSOR(orgSorCode)
				.setTime(authorTime)
				.setPersonIdentity(laegeAndersAndersen)
				.setOrganizationIdentity(
						new OrganizationIdentity.OrganizationBuilder().setName(orgName).build())
				.build();

		appointment.setAppointmentPerformer(appointmentPerformer);

		appointment.setConfidentialityCode("N");

		DocumentMetadata metaData = axrs.createDocumentMetadata(appointment);
		if  (contentTypeCode != null) {
			metaData.setContentTypeCode(contentTypeCode);
		}
		String xml = codec.encode(appointment);
		
		String documentId = axrs.createAndRegisterDocument(uuid, xml, metaData);
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


	public static Patient definePersonIdentity(String name, String cpr) {

		Patient nancy = new Patient.PatientBuilder(name)
				.setSSN(cpr)
				.build();

		return nancy;
	}
	
	protected String generateUUID() {
		java.util.UUID uuid = java.util.UUID.randomUUID();
		return Math.abs(uuid.getLeastSignificantBits()) + "." + Math.abs(uuid.getMostSignificantBits())+"."+Calendar.getInstance().getTimeInMillis();
	}

}
