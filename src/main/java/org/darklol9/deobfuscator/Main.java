package org.darklol9.deobfuscator;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: java -jar deobfuscator.jar <input> <output>");
            System.exit(1);
        }

        File input = new File(args[0]);
        File output = new File(args[1]);
        Deobfuscator deobf = Deobfuscator.builder().input(input).output(output).build();
        deobf.deobfuscate();
    }

}
