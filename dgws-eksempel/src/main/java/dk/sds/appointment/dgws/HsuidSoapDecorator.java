package dk.sds.appointment.dgws;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import dk.nsi.hsuid.ActingUserCivilRegistrationNumberAttribute;
import dk.nsi.hsuid.Attribute;
import dk.nsi.hsuid.CitizenCivilRegistrationNumberAttribute;
import dk.nsi.hsuid.ConsentOverrideAttribute;
import dk.nsi.hsuid.HealthcareServiceUserIdentificationHeaderUtil;
import dk.nsi.hsuid.OperationsOrganisationNameAttribute;
import dk.nsi.hsuid.OrganisationIdentifierAttribute;
import dk.nsi.hsuid.ResponsibleUserAuthorizationCodeAttribute;
import dk.nsi.hsuid.ResponsibleUserCivilRegistrationNumberAttribute;
import dk.nsi.hsuid.SystemNameAttribute;
import dk.nsi.hsuid.SystemVendorNameAttribute;
import dk.nsi.hsuid.SystemVersionAttribute;
import dk.nsi.hsuid.UserTypeAttribute;
import dk.nsi.hsuid._2016._08.hsuid_1_1.HsuidHeader;
import dk.nsi.hsuid._2016._08.hsuid_1_1.SubjectIdentifierType;
import dk.sts.appointment.configuration.UserContext;

public class HsuidSoapDecorator extends DgwsSoapDecorator {

	UserContext userContext;
	
	public HsuidSoapDecorator(UserContext userContext) {
		this.userContext = userContext;
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		super.handleMessage(message);
		try {
			// HsuidHeader
			Header hsUidHeader = getHsuid("nsi:HealthcareProfessional", "0101584160", "system owner", "test aftaler", "1.0", "MyOrganisation", "Aftale");
			message.getHeaders().add(hsUidHeader);
		} catch (JAXBException | ParserConfigurationException e) {
			throw new Fault(e);
		}
	}

	private Header getHsuid(String isCitizen, String userCivilRegistrationNumber, String systemOwner, String systemName, String systemVersion, String responsibleOrg, String issuer) throws JAXBException, ParserConfigurationException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new CitizenCivilRegistrationNumberAttribute(userContext.getPatientId()));
		attributes.add(new UserTypeAttribute(isCitizen));
		attributes.add(new ActingUserCivilRegistrationNumberAttribute(userCivilRegistrationNumber));
		attributes.add(new OrganisationIdentifierAttribute("293591000016003", SubjectIdentifierType.NSI_SORCODE.toString()));
		attributes.add(new SystemVendorNameAttribute(systemOwner));
		attributes.add(new SystemNameAttribute(systemName));
		attributes.add(new SystemVersionAttribute(systemVersion));
		attributes.add(new OperationsOrganisationNameAttribute(responsibleOrg));
		attributes.add(new ConsentOverrideAttribute(true));
		attributes.add(new ResponsibleUserCivilRegistrationNumberAttribute(userCivilRegistrationNumber));
		attributes.add(new ResponsibleUserAuthorizationCodeAttribute("SRTTQ"));
		HsuidHeader hsuidHeader = HealthcareServiceUserIdentificationHeaderUtil.createHealthcareServiceUserIdentificationHeader(issuer, attributes);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc = dbf.newDocumentBuilder().newDocument(); 			
		JAXBContext jaxbContext = JAXBContext.newInstance(HsuidHeader.class);
		jaxbContext.createMarshaller().marshal(hsuidHeader, doc);

		Node hsUidElement = doc.getDocumentElement().getFirstChild();
		QName hsUidQName = new QName(hsUidElement.getNamespaceURI(), hsUidElement.getLocalName());
		Header hsUidHeader = new Header(hsUidQName, doc.getFirstChild());
		return hsUidHeader;
	}
}
