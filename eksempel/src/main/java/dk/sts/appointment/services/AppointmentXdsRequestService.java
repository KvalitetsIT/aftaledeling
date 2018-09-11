package dk.sts.appointment.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.EbXMLFactory;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.EbXMLFactory30;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.EbXMLQueryResponse30;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.ProvideAndRegisterDocumentSetRequestType;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetRequestType;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType.DocumentResponse;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Severity;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.lcm.SubmitObjectsRequest;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryRequest;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rs.RegistryError;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rs.RegistryErrorList;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rs.RegistryResponseType;
import org.openehealth.ipf.commons.ihe.xds.core.transform.responses.QueryResponseTransformer;
import org.openehealth.ipf.commons.ihe.xds.iti18.Iti18PortType;
import org.openehealth.ipf.commons.ihe.xds.iti41.Iti41PortType;
import org.openehealth.ipf.commons.ihe.xds.iti43.Iti43PortType;
import org.openehealth.ipf.commons.ihe.xds.iti57.Iti57PortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import dk.s4.hl7.cda.codes.Loinc;
import dk.s4.hl7.cda.codes.MedCom;
import dk.s4.hl7.cda.codes.NSI;
import dk.s4.hl7.cda.model.apd.AppointmentDocument;
import dk.s4.hl7.cda.model.phmr.PHMRDocument;
import dk.sts.appointment.AppointmentConstants;
import dk.sts.appointment.dto.DocumentMetadata;

public class AppointmentXdsRequestService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentXdsRequestService.class);

	private static final EbXMLFactory ebXMLFactory = new EbXMLFactory30();

	@Autowired
	AppointmentXdsRequestBuilderService appointmentXdsRequestBuilderService;

	@Autowired
	Iti57PortType iti57PortType;

	@Autowired
	Iti43PortType iti43PortType;

	@Autowired
	Iti18PortType iti18PortType;

	@Autowired
	Iti41PortType iti41PortType;

	public List<DocumentEntry> getAllAppointmentsForPatient(String citizenId) throws XdsException {
		return getAppointmentsForPatient(citizenId, null, null);
	}
	public List<DocumentEntry> getAllAppointmentsForPatient(String citizenId, Date start, Date end) throws XdsException {
		return getAppointmentsForPatient(citizenId, start, end);
	}

	public List<DocumentEntry> getAppointmentsForPatient(String citizenId, Date start, Date end) throws XdsException {
		List<Code> typeCodes = new ArrayList<Code>();
		typeCodes.add(AppointmentConstants.APPOINTMENT_CODE);
		return getDocumentsForPatient(citizenId, typeCodes, start, end);
	}

	public List<DocumentEntry> getDocumentsForPatient(String citizenId) throws XdsException {
		return getDocumentsForPatient(citizenId, null, null, null);
	}
	
	public List<DocumentEntry> getDocumentsForPatient(String citizenId, List<Code> typeCodes, Date start, Date end) throws XdsException {
		AdhocQueryRequest adhocQueryRequest = appointmentXdsRequestBuilderService.buildAdhocQueryRequest(citizenId, typeCodes, start, end);
		AdhocQueryResponse adhocQueryResponse = iti18PortType.documentRegistryRegistryStoredQuery(adhocQueryRequest);
		
		RegistryErrorList registryErrorList = adhocQueryResponse.getRegistryErrorList();
		
		if (hasErrors(registryErrorList)) {
			throw new XdsException(registryErrorList);
		} else {
			QueryResponseTransformer queryResponseTransformer = new QueryResponseTransformer(getEbXmlFactory());
			EbXMLQueryResponse30 ebXmlresponse = new EbXMLQueryResponse30(adhocQueryResponse);
			QueryResponse queryResponse = queryResponseTransformer.fromEbXML(ebXmlresponse);
			List<DocumentEntry> docEntries = queryResponse.getDocumentEntries();
			return docEntries;
		}
	}
	
	private boolean hasErrors(RegistryErrorList registryErrorList) {
		if(registryErrorList != null) {
			List<RegistryError> registryErrors = registryErrorList.getRegistryError();
			for (RegistryError registryError : registryErrors) {
				Severity severity  = Severity.valueOfOpcode30(registryError.getSeverity());
				if(Severity.ERROR.equals(severity)) {
					return true;
				}
			}		
		}
		return false;
		
	}

	public DocumentEntry getAppointmentDocumentEntry(String documentId) throws XdsException {

		AdhocQueryRequest adhocQueryRequest = appointmentXdsRequestBuilderService.buildAdhocQueryRequest(documentId, QueryReturnType.LEAF_CLASS);
		AdhocQueryResponse adhocQueryResponse = iti18PortType.documentRegistryRegistryStoredQuery(adhocQueryRequest);

		if (!Status.SUCCESS.getOpcode30().equals(adhocQueryResponse.getStatus()) && adhocQueryResponse.getRegistryErrorList() != null && !adhocQueryResponse.getRegistryErrorList().getRegistryError().isEmpty()) {
			throw new XdsException(adhocQueryResponse.getRegistryErrorList());
		} else {
			QueryResponseTransformer queryResponseTransformer = new QueryResponseTransformer(getEbXmlFactory());
			EbXMLQueryResponse30 ebXmlresponse = new EbXMLQueryResponse30(adhocQueryResponse);
			QueryResponse queryResponse = queryResponseTransformer.fromEbXML(ebXmlresponse);
			List<DocumentEntry> docEntries = queryResponse.getDocumentEntries();
			DocumentEntry documentEntry = docEntries.get(0);
			return documentEntry;
		}
	}

	public String fetchDocument(String documentId) throws IOException, XdsException {
		return fetchDocument(documentId, null, null);
	}	

	public Map<String, String> fetchDocuments(List<String> documentIds, String homeCommunityId, String repositoryId) throws XdsException {

		Map<String, String> documents = new HashMap<>();
		
		RetrieveDocumentSetRequestType rdsrt = null;
		if (repositoryId != null && homeCommunityId != null) {
			rdsrt = appointmentXdsRequestBuilderService.buildRetrieveDocumentSetRequestType(documentIds, homeCommunityId, repositoryId);
		} else {
			rdsrt = appointmentXdsRequestBuilderService.buildRetrieveDocumentSetRequestType(documentIds);
		}

		RetrieveDocumentSetResponseType repositoryResponse= iti43PortType.documentRepositoryRetrieveDocumentSet(rdsrt);
		if (repositoryResponse.getRegistryResponse().getRegistryErrorList() == null || repositoryResponse.getRegistryResponse().getRegistryErrorList().getRegistryError() == null || repositoryResponse.getRegistryResponse().getRegistryErrorList().getRegistryError().isEmpty()) {
			// if no documents an error is produced, get(0) should work.
			
			Iterator<DocumentResponse> documentIterator = repositoryResponse.getDocumentResponse().iterator();
			while (documentIterator.hasNext()) {
				DocumentResponse documentResponse = documentIterator.next();
				try {
					String documentString = new BufferedReader(new InputStreamReader(documentResponse.getDocument().getInputStream())).lines().collect(Collectors.joining());
					documents.put(documentResponse.getDocumentUniqueId(), documentString);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

		} else {
			XdsException e = new XdsException();
			for (RegistryError registryError :repositoryResponse.getRegistryResponse().getRegistryErrorList().getRegistryError()) {
				e.addError(registryError.getCodeContext());
			}
			throw e;
		}
		return documents;
	}
	
	public String fetchDocument(String documentId, String homeCommunityId, String repositoryId) throws IOException, XdsException {
		List<String> documentIds = new LinkedList<String>();
		documentIds.add(documentId);
		Map<String, String> documentResult = fetchDocuments(documentIds, homeCommunityId, repositoryId);
		return documentResult.get(documentId);
	}




	public String createAndRegisterDocumentAsReplacement(String externalIdForUpdatedDocument, String updatedAppointmentXmlDocument, DocumentMetadata updatedAppointmentCdaMetadata, String externalIdForDocumentToReplace) throws XdsException {
		ProvideAndRegisterDocumentSetRequestType provideAndRegisterDocumentSetRequest = appointmentXdsRequestBuilderService.buildProvideAndRegisterDocumentSetRequestWithReplacement(externalIdForUpdatedDocument, updatedAppointmentXmlDocument, updatedAppointmentCdaMetadata, externalIdForDocumentToReplace);
		RegistryResponseType registryResponse = iti41PortType.documentRepositoryProvideAndRegisterDocumentSetB(provideAndRegisterDocumentSetRequest);
		if (registryResponse.getRegistryErrorList() == null || registryResponse.getRegistryErrorList().getRegistryError() == null || registryResponse.getRegistryErrorList().getRegistryError().isEmpty()) {
			return externalIdForUpdatedDocument;
		} else {
			XdsException e = new XdsException();
			for (RegistryError registryError :registryResponse.getRegistryErrorList().getRegistryError()) {
				e.addError(registryError.getCodeContext());
			}
			throw e;
		}
	}


	public String createAndRegisterDocument(String externalId, String document, DocumentMetadata documentMetadata) throws XdsException {
		ProvideAndRegisterDocumentSetRequestType provideAndRegisterDocumentSetRequest = appointmentXdsRequestBuilderService.buildProvideAndRegisterDocumentSetRequest(externalId, document, documentMetadata);
		RegistryResponseType registryResponse = iti41PortType.documentRepositoryProvideAndRegisterDocumentSetB(provideAndRegisterDocumentSetRequest);
		if (registryResponse.getRegistryErrorList() == null || registryResponse.getRegistryErrorList().getRegistryError() == null || registryResponse.getRegistryErrorList().getRegistryError().isEmpty()) {
			return externalId;
		} else {
			XdsException e = new XdsException();
			for (RegistryError registryError :registryResponse.getRegistryErrorList().getRegistryError()) {
				e.addError(registryError.getCodeContext());
			}
			throw e;
		}
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
		appointmentCdaMetadata.setMimeType("text/xml");
		appointmentCdaMetadata.setLanguageCode("da/dk");
		appointmentCdaMetadata.setConfidentialityCode(new Code(apd.getConfidentialityCode(), new LocalizedString(apd.getConfidentialityCode()), "2.16.840.1.113883.5.25"));
		return appointmentCdaMetadata;
	}
	
	public DocumentMetadata createDocumentMetadataPhmr(PHMRDocument phmr) throws ParseException {
		DocumentMetadata appointmentCdaMetadata = new DocumentMetadata();
		appointmentCdaMetadata.setTitle(phmr.getTitle());
		appointmentCdaMetadata.setPatientId(new Code(phmr.getPatient().getId().getExtension(), new LocalizedString(phmr.getPatient().getId().getAuthorityName()), phmr.getPatient().getId().getRoot()));
		appointmentCdaMetadata.setReportTime(phmr.getAuthor().getTime());
		appointmentCdaMetadata.setOrganisation(new Code(phmr.getAuthor().getId().getExtension(), new LocalizedString(phmr.getAuthor().getOrganizationIdentity().getOrgName()), NSI.SOR_OID));
		appointmentCdaMetadata.setClassCode(new Code("001", new LocalizedString("Klinisk rapport"), "1.2.208.184.100.9"));
		appointmentCdaMetadata.setFormatCode(new Code("urn:ad:dk:medcom:appointment", new LocalizedString("DK PHMR schema"), "1.2.208.184.100.10"));
		appointmentCdaMetadata.setHealthcareFacilityTypeCode(new Code("22232009", new LocalizedString("hospital") ,"2.16.840.1.113883.6.96"));
		appointmentCdaMetadata.setPracticeSettingCode(new Code("408443003", new LocalizedString("almen medicin"),"2.16.840.1.113883.6.96"));
		appointmentCdaMetadata.setSubmissionTime(new Date());
		Code PHMR_CODE = new Code(Loinc.PHMR_CODE, new LocalizedString(Loinc.PMHR_DISPLAYNAME), Loinc.OID);

		appointmentCdaMetadata.setContentTypeCode(PHMR_CODE);
		appointmentCdaMetadata.setTypeCode(PHMR_CODE);
		appointmentCdaMetadata.setServiceStartTime(phmr.getServiceStartTime());
		appointmentCdaMetadata.setServiceStopTime(phmr.getServiceStopTime());
		return appointmentCdaMetadata;
	}
	

	protected EbXMLFactory getEbXmlFactory() {
		return ebXMLFactory;
	}


	public void deprecateDocument(DocumentEntry toBeDeprecated) throws XdsException {
		SubmitObjectsRequest body = appointmentXdsRequestBuilderService.buildDeprecateSubmitObjectsRequest(toBeDeprecated);		
		RegistryResponseType registryResponse = iti57PortType.documentRegistryUpdateDocumentSet(body);

		if (registryResponse.getRegistryErrorList() == null || registryResponse.getRegistryErrorList().getRegistryError() == null || registryResponse.getRegistryErrorList().getRegistryError().isEmpty()) {
			//OK !
		} else {
			XdsException e = new XdsException();
			for (RegistryError registryError :registryResponse.getRegistryErrorList().getRegistryError()) {
				e.addError(registryError.getCodeContext());
			}
			throw e;
		}
	}

	public void addOutInterceptors(AbstractPhaseInterceptor<Message> interceptor) {		
		addOutInterceptor(iti18PortType, interceptor);
		addOutInterceptor(iti41PortType, interceptor);		
		addOutInterceptor(iti43PortType, interceptor);
		addOutInterceptor(iti57PortType, interceptor);
	}

	private void addOutInterceptor(Object o, AbstractPhaseInterceptor<Message> interceptor) {
		Client proxy = ClientProxy.getClient(o);
		proxy.getOutInterceptors().add(interceptor);
	}

	public void addInInterceptors(AbstractPhaseInterceptor<Message> interceptor) {		
		addInInterceptor(iti18PortType, interceptor);
		addInInterceptor(iti41PortType, interceptor);		
		addInInterceptor(iti43PortType, interceptor);
		addInInterceptor(iti57PortType, interceptor);
	}

	private void addInInterceptor(Object o, AbstractPhaseInterceptor<Message> interceptor) {
		Client proxy = ClientProxy.getClient(o);
		proxy.getInInterceptors().add(interceptor);
	}
}
