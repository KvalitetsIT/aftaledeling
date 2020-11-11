package dk.kvalitetsit.cda.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import dk.s4.hl7.cda.codes.MedCom;
import dk.s4.hl7.cda.convert.APDXmlCodec;
import dk.s4.hl7.cda.model.AddressData;
import dk.s4.hl7.cda.model.OrganizationIdentity;
import dk.s4.hl7.cda.model.Participant;
import dk.s4.hl7.cda.model.Patient;
import dk.s4.hl7.cda.model.PersonIdentity;
import dk.s4.hl7.cda.model.AddressData.Use;
import dk.s4.hl7.cda.model.Participant.ParticipantBuilder;
import dk.s4.hl7.cda.model.Patient.PatientBuilder;
import dk.s4.hl7.cda.model.apd.AppointmentDocument;
import dk.s4.hl7.cda.model.apd.AppointmentDocument.Status;

public class CdaBuilderTest {

	
	
	@Test
	public void testBuildAppointment() throws ParseException {

		// Given 
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		Date from = sdf.parse("10-10-2019 10:00");
		Date to = sdf.parse("10-10-2019 12:00");

		PersonIdentity personid = new PersonIdentity.PersonBuilder("Læge Sven Bertelsen").build();
		AppointmentDocument apd = createAppointmentDocument("1.2.3.4.5", "1110109996", "1234", "Test Sor", "Glarmestergade 23", "8000", "Aarhus C", "22222222", personid, from, to, "Indication test");
		
		APDXmlCodec apdCodec = new APDXmlCodec();
		String apdXml = apdCodec.encode(apd);
		
		AppointmentDocument parsedBackToModel = apdCodec.decode(apdXml);
		
		System.out.println(parsedBackToModel.getAppointmentId());
		
	}
	

	public AppointmentDocument createAppointmentDocument(String documentId, String patientId, String orgSor, String orgName, String orgAddress, String orgPostalCode, String orgCity, String orgTelephone, PersonIdentity personIdentity, Date from, Date to, String indicationDisplayName) {
		Date now = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");

		AppointmentDocument appointment = new AppointmentDocument(MedCom.createId(documentId));

		Patient p = new PatientBuilder().
				setSSN(patientId).
				addFamilyName("Gludsen").
				build();
		appointment.setPatient(p);

		OrganizationIdentity organization = new OrganizationIdentity.OrganizationBuilder()
				.setName(orgName)
				.setAddress(new AddressData.AddressBuilder()
						.addAddressLine(orgAddress)
						.setCity(orgCity)
						.setPostalCode(orgPostalCode)
						.setUse(Use.WorkPlace)
						.build())
				.setSOR(orgSor)
				.addTelecom(Use.WorkPlace, "tel", orgTelephone)
				.build();

		Date authorTime = now;
		Participant part = new ParticipantBuilder()
				.setAddress(organization.getAddress())
				.setSOR(organization.getIdValue())
				.setTelecomList(organization.getTelecomList())
				.setTime(authorTime)
				.setPersonIdentity(personIdentity)
				.setOrganizationIdentity(organization)
				.build();

		appointment.setAuthor(part);
		appointment.setAppointmentPerformer(part);

		appointment.setCustodian(organization);
		appointment.setIndicationDisplayName(indicationDisplayName);
		appointment.setLanguageCode("da-DK");
		appointment.setTitle("Aftale for "+patientId);
		appointment.setEffectiveTime(from);
		appointment.setAppointmentStatus(Status.ACTIVE);
		appointment.setDocumentationTimeInterval(from, to);
		appointment.setAppointmentTitle("Aftale");
		appointment.setAppointmentText("<paragraph>Aftale:</paragraph>" + 
				"<table width=\"100%\">" + "<tbody>" + "<tr>"
				+ "<th>Status</th>" 
				+ "<th>Aftale dato</th>" 
				+ "<th>Vedrørende</th>" 
				+ "<th>Mødested</th>" 
				+ "</tr>" + "<tr>"
				+ "<td>active</td>" 
				+ "<td>"+sdf.format(from)+" - "+sdf.format(to)+"</td>"
				+ "<td>"+indicationDisplayName+"</td>" 
				+ "<td>"+orgAddress+", "+orgPostalCode+" "+orgCity+"</td>"
				+ "</tr>" + "</tbody>" + "</table>");
		appointment.setAppointmentLocation(organization);
		return appointment;
	}

}
