package ci.allogaz;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AlloGazApplication {

    public static final String ZONE = "Africa/Abidjan";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE));
        Locale.setDefault(Locale.FRENCH);
        SpringApplication.run(AlloGazApplication.class, args);
    }
}
