package dk.sds.dds;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import dk.sds.appointment.configuration.DgwsConfiguration;
import dk.sts.appointment.configuration.ApplicationConfiguration;

@Import({ApplicationConfiguration.class, DgwsConfiguration.class})
@EnableAutoConfiguration
@Configuration
@PropertySource("test.properties")
public class TestConfiguration {

	
}
