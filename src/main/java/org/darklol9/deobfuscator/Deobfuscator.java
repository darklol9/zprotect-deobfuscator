package org.darklol9.deobfuscator;

import lombok.Builder;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.darklol9.transformers.AbstractTransformer;
import org.darklol9.transformers.indy.RedirectTransformer;
import org.darklol9.transformers.string.StringTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import sun.util.calendar.BaseCalendar;

import java.io.*;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Builder
public class Deobfuscator {

    private File input;
    private File output;

    @SneakyThrows
    public void deobfuscate() {

        long start = System.currentTimeMillis();

        log("Running deobfuscation on %s", input.getName());

        JarFile jf = new JarFile(input);

        HashSet<ClassNode> classNodes = new HashSet<>();
        Map<String, byte[]> resources = new HashMap<>();

        LinkedList<AbstractTransformer> transformers = new LinkedList<>();
        transformers.add(new RedirectTransformer(classNodes));
        transformers.add(new StringTransformer(classNodes));

        // Get all files inside jar
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            if (entry.getName().endsWith(".class") || entry.getName().endsWith(".class/")) {
                ClassNode classNode = new ClassNode();
                ClassReader reader = new ClassReader(jf.getInputStream(entry));
                reader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classNodes.add(classNode);
            } else {
                resources.put(entry.getName(), IOUtils.toByteArray(jf.getInputStream(entry)));
            }
        }

        for (AbstractTransformer transformer : transformers) {
            transformer.visit();
        }

        try (FileOutputStream fos = new FileOutputStream(output)) {
            try (JarOutputStream jos = new JarOutputStream(fos)) {

                for (ClassNode classNode : classNodes) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                    classNode.accept(writer);
                    jos.putNextEntry(new JarEntry(classNode.name + ".class"));
                    jos.write(writer.toByteArray());
                }

                resources.forEach((name, data) -> {
                    try {
                        jos.putNextEntry(new JarEntry(name));
                        jos.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        long difference = output.length() - input.length();

        boolean compressed = difference < 0;
        Date epoch = new Date(0);
        Date elapsed = Date.from(Instant.ofEpochMilli(System.currentTimeMillis() - start));

        StringBuilder time = new StringBuilder();

        int dh = elapsed.getHours() - epoch.getHours();
        int dm = elapsed.getMinutes() - epoch.getMinutes();
        int ds = elapsed.getSeconds() - epoch.getSeconds();

        if (dh > 0)
            time.append(dh).append("h ");
        if (dm > 0)
            time.append(dm).append("m ");
        if (ds > 0)
            time.append(ds).append("s ");
        Method normalize = Date.class.getDeclaredMethod("normalize");
        normalize.setAccessible(true);
        BaseCalendar.Date date = (BaseCalendar.Date) normalize.invoke(elapsed);
        time.append(date.getMillis()).append("ms");

        System.out.printf("Size: %.2fKB -> %.2fKB (%s%.2f%%)\n",
                input.length()/1024D, output.length()/1024D, compressed ? "-" : "+", (100D * Math.abs((double) difference) / (double) input.length()));
        System.out.printf("Elapsed: %s\n", time);
    }

    private void log(String s, Object... args) {
        System.out.printf(s + "\n", args);
    }
}
