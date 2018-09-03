package dk.sds.appointment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

import dk.s4.hl7.cda.codes.MedCom;
import dk.s4.hl7.cda.convert.APDXmlCodec;
import dk.s4.hl7.cda.model.AddressData;
import dk.s4.hl7.cda.model.AddressData.Use;
import dk.s4.hl7.cda.model.OrganizationIdentity;
import dk.s4.hl7.cda.model.Participant;
import dk.s4.hl7.cda.model.Participant.ParticipantBuilder;
import dk.s4.hl7.cda.model.Patient;
import dk.s4.hl7.cda.model.Patient.PatientBuilder;
import dk.s4.hl7.cda.model.PersonIdentity;
import dk.s4.hl7.cda.model.apd.AppointmentDocument;
import dk.s4.hl7.cda.model.apd.AppointmentDocument.Status;
import dk.sds.appointment.configuration.DgwsConfiguration;
import dk.sts.appointment.configuration.ApplicationConfiguration;
import dk.sts.appointment.configuration.PatientContext;
import dk.sts.appointment.dto.DocumentMetadata;
import dk.sts.appointment.services.AppointmentXdsRequestService;

@Import({ApplicationConfiguration.class, DgwsConfiguration.class})
@EnableAutoConfiguration
public class AftaleGenerator implements CommandLineRunner {

	private static APDXmlCodec codec = new APDXmlCodec();

	@Autowired
	AppointmentXdsRequestService appointmentXdsRequestService;

	@Autowired
	PatientContext oatientContext;

	@Override
	public void run(String... args) throws Exception {

		BufferedReader b = null;

		if (args.length > 0) {
			String csvFile = args[0];
			File f = new File(csvFile);
			b = new BufferedReader(new FileReader(f));
		} else {
			System.out.println("Usage: AftaleGenerator <csv_input_file>");
			System.exit(0);
		}


		SimpleDateFormat dateParser = new SimpleDateFormat("yyyyMMddHHmmssZ");
		String readLine = "";
		while ((readLine = b.readLine()) != null) {
			String[] segments = readLine.split("¤");

			if (segments.length != 12) {
				System.out.println("Forkert linje");
				System.exit(0);
			}
			String uuid = segments[0];
			String patientId = segments[1];
			String fromDateStr = segments[2];
			Date fromDate = dateParser.parse(fromDateStr);
			String toDateStr = segments[3];
			Date toDate = dateParser.parse(toDateStr);
			String orgSor = segments[4];
			String orgAddress = segments[5];
			String orgPostalCode = segments[6];
			String orgCity = segments[7];
			String orgPhone = segments[8];
			String orgName = segments[9];
			String personText = segments[10];
			String indication = segments[11];

			if (orgPostalCode.length() != 4) {
				System.out.println("Forkert postnummer: "+orgPostalCode);
				System.exit(0);
			}

			oatientContext.setPatientId(patientId);
			PersonIdentity patientIdentity = new PersonIdentity.PersonBuilder(personText).build();

			AppointmentDocument appointmentDocument = createAppointmentDocument(uuid, patientId, orgSor, orgName, orgAddress, orgPostalCode, orgCity, orgPhone, patientIdentity, fromDate, toDate, indication);
			DocumentMetadata documentMetadata = appointmentXdsRequestService.createDocumentMetadata(appointmentDocument);
			String xml = codec.encode(appointmentDocument);
			String documentId = appointmentXdsRequestService.createAndRegisterDocument(uuid, xml, documentMetadata);
			System.out.println("We registered a new appointment with documentId="+documentId);
		}
	}


	public static void main(String[] args) throws Exception {
		SpringApplicationBuilder sab = new SpringApplicationBuilder(AftaleGenerator.class);
		sab.web(false);
		sab.run(args);
	}	

	public AppointmentDocument createAppointmentDocument(String documentId, String patientId, String orgSor, String orgName, String orgAddress, String orgPostalCode, String orgCity, String orgTelephone, PersonIdentity personIdentity, Date from, Date to, String indicationDisplayName) {
		Date now = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");

		AppointmentDocument appointment = new AppointmentDocument(MedCom.createId(documentId));

		Patient p = new PatientBuilder().setSSN(patientId).build();
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
