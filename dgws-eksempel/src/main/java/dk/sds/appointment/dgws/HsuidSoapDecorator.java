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
import org.springframework.beans.factory.annotation.Autowired;
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
import dk.nsi.hsuid._2016._08.hsuid_1_1.HsuidHeader;
import dk.nsi.hsuid._2016._08.hsuid_1_1.SubjectIdentifierType;
import dk.sds.dgws.DgwsContext;

public class HsuidSoapDecorator extends DgwsSoapDecorator {

	@Autowired
	DgwsContext dgwsContext;

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		super.handleMessage(message);
		try {
			// HsuidHeader
			Header hsUidHeader = getHsuid("system owner", "test aftaler", "1.0", "MyOrganisation", "Aftale");
			message.getHeaders().add(hsUidHeader);
		} catch (JAXBException | ParserConfigurationException e) {
			throw new Fault(e);
		}
	}

	private Header getHsuid(String systemOwner, String systemName, String systemVersion,
			String responsibleOrg, String issuer) throws JAXBException, ParserConfigurationException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new CitizenCivilRegistrationNumberAttribute(dgwsContext.getPatientContext().getPatientId()));
		attributes.add(dgwsContext.getUserType());
		attributes.add(new ActingUserCivilRegistrationNumberAttribute(dgwsContext.getUserCivilRegistrationNumber()));
		attributes.add(new OrganisationIdentifierAttribute("25450442", SubjectIdentifierType.NSI_SORCODE.toString()));
		attributes.add(new SystemVendorNameAttribute(systemOwner));
		attributes.add(new SystemNameAttribute(systemName));
		attributes.add(new SystemVersionAttribute(systemVersion));
		attributes.add(new OperationsOrganisationNameAttribute(responsibleOrg));
		if(dgwsContext.useConsentOverride()) {
			attributes.add(new ConsentOverrideAttribute(dgwsContext.getConsentOverride()));
		}
			attributes
				.add(new ResponsibleUserCivilRegistrationNumberAttribute(dgwsContext.getUserCivilRegistrationNumber()));
		attributes.add(new ResponsibleUserAuthorizationCodeAttribute(dgwsContext.getUserAuthorizationCode()));
		HsuidHeader hsuidHeader = HealthcareServiceUserIdentificationHeaderUtil
				.createHealthcareServiceUserIdentificationHeader(issuer, attributes);

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
