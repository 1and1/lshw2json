/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oneandone.lshw2json.integration;

import com.oneandone.lshw2json.Main;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Abstract test that reads one XML file and compares the result 
 * with a given JSON file.
 * The only methods that need to be implemented are {@link #getExpectedName()}
 * and {@link #getInputName()}.
 * @author Stephan Fuhrmann
 */
public abstract class AbstractIntegration {
    /** The input XML file. */
    private File input;
    /** The expected JSON file. */
    private File expected;
    
    /** Copies one files contents from the classes resources to a temporary file.
     */
    private File copyResourceToTemp(String resourceFile) throws IOException {
        File tmpName;
        URL url = getClass().getResource(resourceFile);
        try (InputStream is = url.openStream()) {
            tmpName = File.createTempFile("lshw2json", "tmp");
            Files.copy(is, tmpName.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return tmpName;
    }
    
    public abstract String getInputName();
    public abstract String getExpectedName();
    
    @Before
    public void setup() throws IOException {
        input = copyResourceToTemp(getInputName());
        expected = copyResourceToTemp(getExpectedName());
    }
    
    @Test
    public void singleFileRun() throws IOException {
        Main.main(new String[] {input.getAbsolutePath()});
        File generated = new File(input.getParentFile(), input.getName()+".json");
        assertTrue(generated.exists());
        
        List<String> expectedLines = Files.readAllLines(expected.toPath());
        List<String> actualLines = Files.readAllLines(generated.toPath());
        
        assertEquals(expectedLines, actualLines);
    }
}
