package dk.sts.appointment.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.activation.DataHandler;

import org.openehealth.ipf.commons.ihe.xds.core.ebxml.EbXMLAdhocQueryRequest;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.EbXMLFactory;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.EbXMLFactory30;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.EbXMLProvideAndRegisterDocumentSetRequest30;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.ProvideAndRegisterDocumentSetRequestType;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetRequestType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationLabel;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Author;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Document;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntryType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Vocabulary;
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.GetDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.Query;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.lcm.SubmitObjectsRequest;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryRequest;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.AssociationType1;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.ClassificationType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.ExternalIdentifierType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.InternationalStringType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.LocalizedStringType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.ObjectFactory;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.RegistryObjectListType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.RegistryPackageType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.SlotType1;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.ValueListType;
import org.openehealth.ipf.commons.ihe.xds.core.transform.requests.ProvideAndRegisterDocumentSetTransformer;
import org.openehealth.ipf.commons.ihe.xds.core.transform.requests.QueryRegistryTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.sun.istack.ByteArrayDataSource;

import dk.sts.appointment.dto.DocumentMetadata;
import dk.sts.appointment.utilities.PatientIdAuthority;

public class AppointmentXdsRequestBuilderService {

	@Value("${xds.repositoryuniqueid}")
	String repositoryUniqueId;

	@Autowired
	PatientIdAuthority patientIdAuthority;

	public ProvideAndRegisterDocumentSetRequestType buildProvideAndRegisterDocumentSetRequestWithReplacement(String externalIdForUpdatedDocument, String updatedAppointmentXmlDocument, DocumentMetadata updatedAppointmentCdaMetadata, String externalIdForDocumentToReplace) {
		return buildProvideAndRegisterDocumentSetRequest(externalIdForUpdatedDocument, updatedAppointmentXmlDocument, updatedAppointmentCdaMetadata, externalIdForDocumentToReplace);
	}

	public ProvideAndRegisterDocumentSetRequestType buildProvideAndRegisterDocumentSetRequest(String extenalDocumentId, String documentPayload, DocumentMetadata documentMetadata) {
		return buildProvideAndRegisterDocumentSetRequest(extenalDocumentId, documentPayload, documentMetadata, null);
	}

	
	public ProvideAndRegisterDocumentSetRequestType buildProvideAndRegisterDocumentSetRequest(String extenalDocumentId, String documentPayload, DocumentMetadata documentMetadata, String externalIdForDocumentToReplace) {

		DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		ProvideAndRegisterDocumentSet provideAndRegisterDocumentSet = new ProvideAndRegisterDocumentSet();

		// Create DocumentEntry for the CDA codument
		DocumentEntry documentEntry = new DocumentEntry();
		String documentUuid = generateUUID();
		documentEntry.setEntryUuid(documentUuid);

		// Patient Identification
		Identifiable patientIdentifiable = null;
		if (documentMetadata.getPatientId() != null) {
			AssigningAuthority patientIdAssigningAuthority = new AssigningAuthority(documentMetadata.getPatientId().getSchemeName());
			patientIdentifiable = new Identifiable(documentMetadata.getPatientId().getCode(), patientIdAssigningAuthority);
		}
		documentEntry.setPatientId(patientIdentifiable);
		documentEntry.setSourcePatientId(patientIdentifiable);


		// Create author (Organisation)
		AssigningAuthority organisationAssigningAuthority = new AssigningAuthority(documentMetadata.getOrganisation().getSchemeName());
		Author author = new Author();
		if (documentMetadata.getOrganisation() != null && documentMetadata.getOrganisation().getCode() != null) {
			Organization authorOrganisation = new Organization(documentMetadata.getOrganisation().getDisplayName().getValue(), documentMetadata.getOrganisation().getCode(), organisationAssigningAuthority);
			author.getAuthorInstitution().add(authorOrganisation);
		}
		documentEntry.setAuthor(author);

		// Availability Status (enumeration: APPROVED, SUBMITTED, DEPRECATED)
		documentEntry.setAvailabilityStatus(AvailabilityStatus.APPROVED);

		// Dates 
		if (documentMetadata.getReportTime() != null) {
			documentEntry.setCreationTime(dateTimeFormat.format(documentMetadata.getReportTime()));
		}
		if (documentMetadata.getServiceStartTime() != null) {
			documentEntry.setServiceStartTime(dateTimeFormat.format(documentMetadata.getServiceStartTime()));
		}
		if (documentMetadata.getServiceStopTime() != null) {
			documentEntry.setServiceStopTime(dateTimeFormat.format(documentMetadata.getServiceStopTime()));
		}

		
		if (documentMetadata.getClassCode() != null) {
			documentEntry.setClassCode(documentMetadata.getClassCode());
		}
		if (documentMetadata.getConfidentialityCode() != null) {
			documentEntry.getConfidentialityCodes().add(documentMetadata.getConfidentialityCode());
		}

		List<Code> eventCodesEntry = documentEntry.getEventCodeList();
		if (documentMetadata.getEventCodes() != null) {
			for (Code eventCode : documentMetadata.getEventCodes()) {
				eventCodesEntry.add(eventCode);
			}
		}
		if (documentMetadata.getFormatCode() != null) {
			documentEntry.setFormatCode(documentMetadata.getFormatCode());
		}
		if (documentMetadata.getHealthcareFacilityTypeCode() != null) {
			documentEntry.setHealthcareFacilityTypeCode(documentMetadata.getHealthcareFacilityTypeCode());
		}
		if (documentMetadata.getLanguageCode() != null) {
			documentEntry.setLanguageCode(documentMetadata.getLanguageCode());
		}
		if (documentMetadata.getMimeType() != null) {
			documentEntry.setMimeType(documentMetadata.getMimeType());
		}
		documentEntry.setType(DocumentEntryType.STABLE);
		if (documentMetadata.getTitle() != null) {
			documentEntry.setTitle(new LocalizedString(documentMetadata.getTitle()));
		}
		if (documentMetadata.getTypeCode() != null) {
			documentEntry.setTypeCode(documentMetadata.getTypeCode());
		}
		if (documentMetadata.getPracticeSettingCode() != null) {
			documentEntry.setPracticeSettingCode(documentMetadata.getPracticeSettingCode());
		}
		documentEntry.setUniqueId(extenalDocumentId);
		documentEntry.setLogicalUuid(documentUuid);

		Document document = new Document(documentEntry, new DataHandler(new ByteArrayDataSource(documentPayload.getBytes(), documentMetadata.getMimeType())));
		provideAndRegisterDocumentSet.getDocuments().add(document);

		// Create SubmissionSet for the document
		SubmissionSet submissionSet = createSubmissionSet(patientIdentifiable, documentMetadata.getContentTypeCode(), documentMetadata.getReportTime());
		provideAndRegisterDocumentSet.setSubmissionSet(submissionSet);

		// Associate the SubmissionSet with the DocumentEntry
		Association association = new Association();
		association.setAssociationType(AssociationType.HAS_MEMBER);
		association.setEntryUuid(generateUUID());
		association.setSourceUuid(submissionSet.getEntryUuid());
		association.setTargetUuid(documentEntry.getEntryUuid());
		association.setAvailabilityStatus(AvailabilityStatus.APPROVED);
		association.setLabel(AssociationLabel.ORIGINAL);
		provideAndRegisterDocumentSet.getAssociations().add(association);
		if (externalIdForDocumentToReplace != null) {
			Association replacementAssociation = new Association(AssociationType.REPLACE, generateUUID(), documentEntry.getEntryUuid(), externalIdForDocumentToReplace);
			provideAndRegisterDocumentSet.getAssociations().add(replacementAssociation);
		}

		// Transform request
		ProvideAndRegisterDocumentSetTransformer registerDocumentSetTransformer = new ProvideAndRegisterDocumentSetTransformer(getEbXmlFactory());
		EbXMLProvideAndRegisterDocumentSetRequest30 ebxmlRequest = (EbXMLProvideAndRegisterDocumentSetRequest30) registerDocumentSetTransformer.toEbXML(provideAndRegisterDocumentSet);
		ProvideAndRegisterDocumentSetRequestType provideAndRegisterDocumentSetRequestType = ebxmlRequest.getInternal();

		return provideAndRegisterDocumentSetRequestType;
	}

	
	public SubmitObjectsRequest buildDeprecateSubmitObjectsRequest(DocumentEntry documentEntry) {
		
		ObjectFactory factory = new ObjectFactory();
		SubmitObjectsRequest body = new SubmitObjectsRequest();
		RegistryObjectListType registryObjectList = factory.createRegistryObjectListType();
		RegistryPackageType registryPackageType = makeRegistryPackageType(documentEntry);
		registryObjectList.getIdentifiable().add(factory.createRegistryPackage(registryPackageType));
		registryObjectList.getIdentifiable().add(factory.createAssociation(makeAssociation(documentEntry.getEntryUuid(), documentEntry.getAvailabilityStatus().getQueryOpcode(), AvailabilityStatus.DEPRECATED.getQueryOpcode())));

		body.setRegistryObjectList(registryObjectList);

		return body;
	}

	public RetrieveDocumentSetRequestType buildRetrieveDocumentSetRequestType(List<String> documentIds, String homeCommunityId, String repositoryId) {
		RetrieveDocumentSetRequestType retrieveDocumentSetRequestType = new RetrieveDocumentSetRequestType();

		for (Iterator<String> iterator = documentIds.iterator(); iterator.hasNext();) {
			RetrieveDocumentSetRequestType.DocumentRequest documentRequest = new RetrieveDocumentSetRequestType.DocumentRequest();
			documentRequest.setRepositoryUniqueId(repositoryId);
			documentRequest.setHomeCommunityId(homeCommunityId);
			documentRequest.setDocumentUniqueId(iterator.next());
			retrieveDocumentSetRequestType.getDocumentRequest().add(documentRequest);
		}
		return retrieveDocumentSetRequestType;
	}


	public RetrieveDocumentSetRequestType buildRetrieveDocumentSetRequestType(List<String> documentIds){
		return buildRetrieveDocumentSetRequestType(documentIds, null, repositoryUniqueId);
	}

	public AdhocQueryRequest buildAdhocQueryRequest(String citizenId, List<Code> typeCodes, Date start, Date end) {
		return buildAdhocQueryRequest(citizenId, typeCodes, AvailabilityStatus.APPROVED, start, end);
	}

	public AdhocQueryRequest buildAdhocQueryRequest(String citizenId, List<Code> typeCodes, AvailabilityStatus availabilityStatus, Date start, Date end) {
		FindDocumentsQuery fdq = new FindDocumentsQuery();
		DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		AssigningAuthority authority = new AssigningAuthority(patientIdAuthority.getPatientIdAuthority());

		// Patient ID
		Identifiable patientIdentifiable = new Identifiable(citizenId, authority);
		fdq.setPatientId(patientIdentifiable);

		// Availability Status
		List<AvailabilityStatus> availabilityStati = new LinkedList<AvailabilityStatus>();
		availabilityStati.add(availabilityStatus);
		fdq.setStatus(availabilityStati);

		if (typeCodes != null) {
			fdq.setTypeCodes(typeCodes);
		}

		if (start != null) {
			fdq.getServiceStartTime().setFrom(dateTimeFormat.format(start));
		}

		if (end != null) {
			fdq.getServiceStartTime().setTo(dateTimeFormat.format(end));
		}


		QueryRegistry queryRegistry = new QueryRegistry(fdq);
		QueryReturnType qrt = QueryReturnType.LEAF_CLASS;

		if (qrt != null) {
			queryRegistry.setReturnType(qrt);
		}

		QueryRegistryTransformer queryRegistryTransformer = new QueryRegistryTransformer();
		EbXMLAdhocQueryRequest ebxmlAdhocQueryRequest = queryRegistryTransformer.toEbXML(queryRegistry);
		AdhocQueryRequest internal = (AdhocQueryRequest)ebxmlAdhocQueryRequest.getInternal();

		return internal;
	}

	public AdhocQueryRequest buildAdhocQueryRequest(String documentId) {
		return buildAdhocQueryRequest(documentId, QueryReturnType.LEAF_CLASS);
	}
	
	public AdhocQueryRequest buildAdhocQueryRequest(String documentId, QueryReturnType qrt) {
		List<String> uniqueIds = new LinkedList<String>();
		uniqueIds.add(documentId);

		GetDocumentsQuery gdq = new GetDocumentsQuery();
		gdq.setUniqueIds(uniqueIds);
		
		return createAdhocQueryRequest(gdq, qrt);
	}
	

	private AdhocQueryRequest createAdhocQueryRequest(Query query, QueryReturnType qrt) {
		QueryRegistry queryRegistry = new QueryRegistry(query);
		if (qrt != null) {
			queryRegistry.setReturnType(qrt);
		}
		QueryRegistryTransformer queryRegistryTransformer = new QueryRegistryTransformer();
		EbXMLAdhocQueryRequest ebxmlAdhocQueryRequest = queryRegistryTransformer.toEbXML(queryRegistry);
		AdhocQueryRequest internal = (AdhocQueryRequest)ebxmlAdhocQueryRequest.getInternal();

		return internal;
	}


	protected SubmissionSet createSubmissionSet(Identifiable patientIdentifiable, Code contentTypeCode, Date submissionTime) {
		String submissionSetUuid = generateUUID();
		SubmissionSet submissionSet = new SubmissionSet();
		submissionSet.setUniqueId(submissionSetUuid);
		submissionSet.setSourceId(submissionSetUuid);
		submissionSet.setEntryUuid(submissionSetUuid);
		submissionSet.setPatientId(patientIdentifiable);
		submissionSet.setContentTypeCode(contentTypeCode);
		submissionSet.setAvailabilityStatus(AvailabilityStatus.APPROVED);
		
		if (submissionTime != null) {
			DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			String timestamp = dateTimeFormat.format(submissionTime);
			submissionSet.setSubmissionTime(timestamp);
		}
		return submissionSet;
	}

	private static final EbXMLFactory ebXMLFactory = new EbXMLFactory30();

	protected EbXMLFactory getEbXmlFactory() {
		return ebXMLFactory;
	}

	private String generateUUID() {
		java.util.UUID uuid = java.util.UUID.randomUUID();
		return Math.abs(uuid.getLeastSignificantBits()) + "." + Math.abs(uuid.getMostSignificantBits())+"."+Calendar.getInstance().getTimeInMillis();
	}

	private AssociationType1 makeAssociation(String documentEntryUUID, String originalStatus, String newStatus) {
		AssociationType1 assocation = new AssociationType1();

		assocation.setAssociationType(AssociationType.UPDATE_AVAILABILITY_STATUS.getOpcode30());
		assocation.setSourceObject("SubmissionSet");
		assocation.setTargetObject(documentEntryUUID);
		assocation.getSlot().add(makeSlot(Vocabulary.SLOT_NAME_ORIGINAL_STATUS, originalStatus));
		assocation.getSlot().add(makeSlot(Vocabulary.SLOT_NAME_NEW_STATUS, newStatus));

		return assocation;

	}
	
	private RegistryPackageType makeRegistryPackageType(DocumentEntry documentEntry) {
		RegistryPackageType registryPackage = new RegistryPackageType();
		registryPackage.setId("SubmissionSet");
		registryPackage.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:RegistryPackage");
		
		String cpr = documentEntry.getPatientId().getId();
		String entryUuid = documentEntry.getEntryUuid();
		String sourceId = documentEntry.getRepositoryUniqueId();
		
		ClassificationType classificationNode = new ClassificationType();
		classificationNode.setClassifiedObject("SubmissionSet");
		classificationNode.setClassificationNode(Vocabulary.SUBMISSION_SET_CLASS_NODE);
		registryPackage.getClassification().add(classificationNode);
		
		Code docEntryTypeCode = documentEntry.getTypeCode();
		ClassificationType contentTypeClassification = new ClassificationType();
		contentTypeClassification.setClassifiedObject("SubmissionSet");
		contentTypeClassification.setClassificationScheme(Vocabulary.SUBMISSION_SET_CONTENT_TYPE_CODE_CLASS_SCHEME);		
		contentTypeClassification.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:Classification");
		contentTypeClassification.setNodeRepresentation(docEntryTypeCode.getCode());
		contentTypeClassification.setName(makeInternationalStringType(docEntryTypeCode.getDisplayName().getValue()));		
		contentTypeClassification.getSlot().add(makeSlot("codingScheme", docEntryTypeCode.getSchemeName()));
		registryPackage.getClassification().add(contentTypeClassification);
		
		registryPackage.getExternalIdentifier().add(makeExternalIdentifier(Vocabulary.SUBMISSION_SET_UNIQUE_ID_EXTERNAL_ID, entryUuid));
		registryPackage.getExternalIdentifier().add(makeExternalIdentifier(Vocabulary.SUBMISSION_SET_PATIENT_ID_EXTERNAL_ID, patientIdAuthority.formatPatientIdentifier(cpr)));
		registryPackage.getExternalIdentifier().add(makeExternalIdentifier(Vocabulary.SUBMISSION_SET_SOURCE_ID_EXTERNAL_ID, sourceId));
		
		DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		registryPackage.getSlot().add(makeSlot(Vocabulary.SLOT_NAME_SUBMISSION_TIME, dateTimeFormat.format(new Date())));
		
		return registryPackage;
	}
	
	private SlotType1 makeSlot(String name, String value) {
		SlotType1 slot = new SlotType1();
		ValueListType slotList = new ValueListType();		
		slot.setName(name);
		slotList.getValue().add(value);
		slot.setValueList(slotList);		
		return slot;	
	}

	private InternationalStringType makeInternationalStringType(String value) {
		LocalizedStringType lst = new LocalizedStringType();		
		lst.setValue(value);
		return makeInternationalStringType(lst);
	}

	private InternationalStringType makeInternationalStringType(LocalizedStringType value) {
		InternationalStringType ist = new InternationalStringType();
		ist.getLocalizedString().add(value);
		return ist;
	}

	private ExternalIdentifierType makeExternalIdentifier(String identificationScheme, String value) {
		ExternalIdentifierType externalIdentifier = new ExternalIdentifierType();
		externalIdentifier.setObjectType("urn:oasis:names:tc:ebxml-regrep:ObjectType:RegistryObject:ExternalIdentifier");
		externalIdentifier.setIdentificationScheme(identificationScheme);
		externalIdentifier.setValue(value);
		externalIdentifier.setRegistryObject("SubmissionSet");
		return externalIdentifier;
	}
}
