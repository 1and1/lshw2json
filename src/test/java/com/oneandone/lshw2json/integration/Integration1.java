package com.oneandone.lshw2json.integration;

/**
 * Test for example1.
 * @author Stephan Fuhrmann
 */
public class Integration1 extends AbstractIntegration {

    @Override
    public String getInputName() {
        return "/example1.xml";
    }

    @Override
    public String getExpectedName() {
        return "/example1.xml.json";
    }
}
