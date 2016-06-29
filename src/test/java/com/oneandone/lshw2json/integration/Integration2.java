/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oneandone.lshw2json.integration;

/**
 * Test for example2.
 * @author Stephan Fuhrmann
 */
public class Integration2 extends AbstractIntegration {

    @Override
    public String getInputName() {
        return "/example2.xml";
    }

    @Override
    public String getExpectedName() {
        return "/example2.xml.json";
    }
}
