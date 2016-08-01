package uk.ac.ebi.chembl.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;


/**
 * Logback converter to replace the %emailTo placeholder with the address in
 * the ${email.to} configuration property, so that the e-mail address to
 * where WARN and ERROR messages are sent can be taken from the application
 * configuration
 */
public class SmtpToConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        // This property is set in AnnotatorApp::prepare
        return System.getProperty("email.to");
    }
}