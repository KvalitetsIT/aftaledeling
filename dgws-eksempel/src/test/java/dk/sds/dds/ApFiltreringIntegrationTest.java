package dk.sds.dds;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.EbXMLQueryResponse30;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rs.RegistryError;
import org.openehealth.ipf.commons.ihe.xds.core.transform.responses.QueryResponseTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.xml.sax.SAXException;

import dk.nsi.hsuid.OrganisationIdentifierAttribute;
import dk.sds.dgws.DgwsContext;
import dk.sts.appointment.configuration.PatientContext;
import dk.sts.appointment.services.AppointmentXdsRequestService;
import dk.sts.appointment.services.XdsException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { Test01Configuration.class }, loader = AnnotationConfigContextLoader.class)
public class ApFiltreringIntegrationTest extends AbstractTest {

	private static final String AP_AFD_1_SORCODE = "45691000016001";
	private static final String AP_AFD_2_SORCODE = "69861000016000";
	private static final String NONAP_SORCODE = "515361000016007";
	
	private static final String PATIENT_1_AP_CONSENT_AFD_1 = "2811008055";
	private static final String PATIENT_2_AP_CONSENT_AFD_2 = "0112774513";
	
	@Autowired
	DgwsContext dgwsContext;

	OrganisationIdentifierAttribute organisationIdentifierAttribute;

	@Autowired
	AppointmentXdsRequestService appointmentXdsRequestService;

	@Before
	public void setup() throws ParseException, SAXException, IOException, ParserConfigurationException, TransformerException, XdsException {


		// Vi laver følgende aftale for
		
		//PATIENT_1: 1 aftale på AP_AFD_1_SORCODE, 1 på AFD_2_SORCODE, og 1 på NONAP_SORCODE
	//	5791583155566471715.5107996068517361886.1537359408120 String aftale1_1 = createAftale(appointmentXdsRequestService, generateUUID(), "Test Person", PATIENT_1_AP_CONSENT_AFD_1, AP_AFD_1_SORCODE, "AP Afdeling 1");
	//	8861746093003310561.4817566306424763317.1537359577640 String aftale1_2 = createAftale(appointmentXdsRequestService, generateUUID(), "Test Person", PATIENT_1_AP_CONSENT_AFD_1, AP_AFD_2_SORCODE, "AP Afdeling 2");
	//	7865757444829144439.290447770598657108.1537359632723 String aftale1_3 = createAftale(appointmentXdsRequestService, generateUUID(), "Test Person", PATIENT_1_AP_CONSENT_AFD_1, NONAP_SORCODE, "Odense afdelingen");
		
	//	System.out.println("aftale1_1:"+aftale1_1+" aftale1_2: "+aftale1_2+" aftale1_3:"+aftale1_3);
		

	}
	
	@Test
	public void testAtIpFiltreringPillerDokumentUdForAfdelingUdenApSamtykkeNårVærdispringIkkeBenyttes() {
		// Given
		PatientContext defaultPatientContext = new PatientContext(PATIENT_1_AP_CONSENT_AFD_1);
		dgwsContext.setPatientContext(defaultPatientContext);
		dgwsContext.setConsentOverride(false);

		// When
		AdhocQueryResponse adhocQueryResponse = appointmentXdsRequestService.getDocumentsForPatientAdhocQueryResponse(PATIENT_1_AP_CONSENT_AFD_1, null, null, null);
		
		// Then
		Assert.assertTrue("Forventede en 'fejl' pga frafiltrering", adhocQueryResponse.getRegistryErrorList() != null && adhocQueryResponse.getRegistryErrorList().getRegistryError().size() == 1);
		RegistryError registryError = adhocQueryResponse.getRegistryErrorList().getRegistryError().get(0);	
		
		QueryResponseTransformer queryResponseTransformer = new QueryResponseTransformer(appointmentXdsRequestService.getEbXmlFactory());
		EbXMLQueryResponse30 ebXmlresponse = new EbXMLQueryResponse30(adhocQueryResponse);
		QueryResponse queryResponse = queryResponseTransformer.fromEbXML(ebXmlresponse);
		
		Assert.assertTrue("Forventede dokumentet fra AP_AFD1", queryResponseContainsDocumentEntry(queryResponse, "5791583155566471715.5107996068517361886.1537359408120"));
		Assert.assertTrue("Forventede dokumentet fra NONAP_SORCODE", queryResponseContainsDocumentEntry(queryResponse, "7865757444829144439.290447770598657108.1537359632723"));
		Assert.assertFalse("Forventede ikke dokumentet fra AP_AFD2", queryResponseContainsDocumentEntry(queryResponse, "8861746093003310561.4817566306424763317.1537359577640"));
	}

	@Test
	public void testAtIpFiltreringIkkePillerDokumentUdForAfdelingUdenApSamtykkeNårVærdispringBenyttes() throws XdsException {
		// Given
		PatientContext defaultPatientContext = new PatientContext(PATIENT_1_AP_CONSENT_AFD_1);
		dgwsContext.setPatientContext(defaultPatientContext);
		dgwsContext.setConsentOverride(true);
		// When
		AdhocQueryResponse adhocQueryResponse = appointmentXdsRequestService.getDocumentsForPatientAdhocQueryResponse(PATIENT_1_AP_CONSENT_AFD_1, null, null, null);
		
		// Then
		Assert.assertTrue("Forventede ingen fejl", adhocQueryResponse.getRegistryErrorList() == null);
		
		QueryResponseTransformer queryResponseTransformer = new QueryResponseTransformer(appointmentXdsRequestService.getEbXmlFactory());
		EbXMLQueryResponse30 ebXmlresponse = new EbXMLQueryResponse30(adhocQueryResponse);
		QueryResponse queryResponse = queryResponseTransformer.fromEbXML(ebXmlresponse);
		
		Assert.assertTrue("Forventede dokumentet fra AP_AFD1", queryResponseContainsDocumentEntry(queryResponse, "5791583155566471715.5107996068517361886.1537359408120"));
		Assert.assertTrue("Forventede dokumentet fra NONAP_SORCODE", queryResponseContainsDocumentEntry(queryResponse, "7865757444829144439.290447770598657108.1537359632723"));
		Assert.assertTrue("Forventede dokumentet fra AP_AFD2", queryResponseContainsDocumentEntry(queryResponse, "8861746093003310561.4817566306424763317.1537359577640"));
	}

	public boolean queryResponseContainsDocumentEntry(QueryResponse queryResponse, String documentId) {
		for (DocumentEntry de : queryResponse.getDocumentEntries()) {
			if (de.getUniqueId().equals(documentId)) {
				return true;
			}
		}
		return false;
	}
}
