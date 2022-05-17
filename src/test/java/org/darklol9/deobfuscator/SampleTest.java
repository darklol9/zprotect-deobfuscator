package org.darklol9.deobfuscator;

import org.junit.jupiter.api.Test;

import java.io.File;

public class SampleTest {

    @Test
    public void test() {
        File input = new File("src/test/resources/sample.jar");
        File output = new File("src/test/resources/output.jar");
        Deobfuscator deobf = Deobfuscator.builder().input(input).output(output).build();
        deobf.deobfuscate();
    }

}
