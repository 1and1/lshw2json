/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
