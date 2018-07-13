package dk.sts.appointment.configuration;

public class UserContext {

	private String patientId;
	
	public UserContext(String patientId) {
		this.patientId = patientId;
	}

	public String getPatientId() {
		return patientId;
	}

	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}
}
