package dk.sts.appointment.services;

import java.util.LinkedList;
import java.util.List;

import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rs.RegistryError;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rs.RegistryErrorList;

public class XdsException extends Exception {

	private static final long serialVersionUID = 1L;

	List<String> errors = new LinkedList<String>();

	public XdsException() {
	}

	public XdsException(RegistryErrorList registryErrorList) {
		this(new LinkedList<String>(), registryErrorList);
	}
	
	private XdsException(List<String> errors, RegistryErrorList registryErrorList) {
		super(toString(errors, registryErrorList));
		setErrors(errors);		
	}
	
	public String addError(String error) {
		this.errors.add(error);
		return error;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
	
	static String toString(List<String> errors, RegistryErrorList list) {
		StringBuilder result = new StringBuilder();
		List<RegistryError> registryErrors = list.getRegistryError();
		String s = "Error received from registry: ";
		result.append(s);
		for (RegistryError error : registryErrors) {
			String err = toString(error);
			result.append(err);
			errors.add(s+err);
		}
		return result.toString();
	}
	
	static String toString(RegistryError err) {
		StringBuilder result = new StringBuilder();
		result.append("[ errorCode:").append(err.getErrorCode());
		result.append(", codectx:").append(err.getCodeContext());
		result.append(", location:").append(err.getLocation());
		result.append(", severity:").append(err.getSeverity());
		result.append(", value:").append(err.getValue());
		result.append("]");
		return result.toString();
	}
}
