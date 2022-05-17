package org.darklol9.transformers.string;

import org.darklol9.transformers.AbstractTransformer;
import org.darklol9.util.AsmUtil;
import org.darklol9.util.InstructionCapture;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StringTransformer extends AbstractTransformer {

    public StringTransformer(HashSet<ClassNode> classes) {
        super(classes);
    }

    @Override
    public void visit() {

        //([JLjava/lang/String;J)Ljava/lang/String;

        AtomicInteger counter = new AtomicInteger();

        long fieldKey = Long.MIN_VALUE;
        ClassNode decryptorClass = null;

        for (ClassNode classNode : classes) {
            for (MethodNode method : classNode.methods) {
                for (AbstractInsnNode ain : method.instructions) {
                    if (ain instanceof MethodInsnNode) {
                        MethodInsnNode node = (MethodInsnNode) ain;
                        if (node.desc.equals("([JLjava/lang/String;J)Ljava/lang/String;")) {

                            if (fieldKey == Long.MIN_VALUE) {
                                decryptorClass = findClass(node.owner);
                                if (decryptorClass == null) throw new RuntimeException("Could not find decryptor class");
                                fieldKey = decryptorClass.fields.stream().map(field -> field.value)
                                        .filter(Objects::nonNull)
                                        .filter(value -> value instanceof Long)
                                        .map(value -> (Long) value)
                                        .findFirst().orElseThrow(() -> new RuntimeException("Could not find field key"));
                            }

                            AbstractInsnNode prev = ain.getPrevious();
                            while (true) {
                                if (prev instanceof IntInsnNode)
                                    if (prev.getOpcode() == NEWARRAY && ((IntInsnNode) prev).operand == T_LONG)
                                        break;
                                if (prev == null)
                                    break;
                                prev = prev.getPrevious();
                            }

                            InstructionCapture capture = new InstructionCapture(prev, new LinkedList<>());
                            long[] values = AsmUtil.grabLongArray(capture);
                            String key = AsmUtil.grabString(capture);
                            long hashedKey = AsmUtil.getLongValue(capture.increment());
                            AbstractInsnNode call = capture.get();
                            if (call instanceof MethodInsnNode) {
                                MethodInsnNode methodCall = (MethodInsnNode) call;
                                if (methodCall.desc.equals("([JLjava/lang/String;J)Ljava/lang/String;")) {
                                    assert values != null;
                                    String decoded = valueOf(values, key, hashedKey, classNode.name.replace("/", "."), fieldKey);
                                    for (AbstractInsnNode caught : capture.getCaught()) {
                                        method.instructions.remove(caught);
                                    }
                                    method.instructions.set(node, new LdcInsnNode(decoded));
                                    counter.incrementAndGet();
                                }
                            }
                        }
                    }
                }
            }
        }

        if (counter.get() > 0) {
            classes.remove(decryptorClass);
            log("Decrypted %d string(s)", counter.get());
        }

    }

    private static String decode(final String s, final String s2) {
        final char[] charArray = s2.toCharArray();
        final byte[] decode = Base64.getDecoder().decode(s);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < decode.length; ++i) {
            final int n = (byte)(decode.length % 256) ^ (-decode[i] + (byte)charArray[i % charArray.length] - (byte)charArray[(i + decode.length / 2) % charArray.length] + (byte)charArray[(31 + i * i) % charArray.length]) % 256;
            sb.append((char)(byte)((n < 0) ? (n + 256) : n));
        }
        return sb.toString();
    }

    private static String toString(final long n) {
        return new String(new char[] { (char)(n & 0xFFFFL), (char)(n >> 16 & 0xFFFFL), (char)(n >> 32 & 0xFFFFL), (char)(n >> 48 & 0xFFFFL) });
    }

    public static String valueOf(final long[] array, final String s, final long n, String callerClass, long fieldKey) {
        final StringBuilder sb = new StringBuilder();
        long n2 = callerClass.hashCode();
        for (int length = array.length, i = 0; i < length; ++i) {
            final long n3 = ~n2 + array[i];
            n2 = (n3 ^ n3 >> 24);
        }
        final long n4 = n ^ (n2 ^ n2 >>> 32);
        for (int length2 = array.length, j = 0; j < length2; ++j) {
            sb.append(decode(toString(array[j] + fieldKey ^ n4), s));
        }
        return sb.toString();
    }
}
