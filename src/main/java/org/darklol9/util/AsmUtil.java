package org.darklol9.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.concurrent.atomic.AtomicReference;

public class AsmUtil implements Opcodes {

    public static boolean isInteger(AbstractInsnNode ain) {
        if (ain == null) return false;
        if ((ain.getOpcode() >= ICONST_M1
                && ain.getOpcode() <= ICONST_5)
                || ain.getOpcode() == SIPUSH
                || ain.getOpcode() == BIPUSH)
            return true;
        if (ain instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) ain;
            return ldc.cst instanceof Integer;
        }
        return false;
    }

    public static int getIntValue(AbstractInsnNode node) {
        if (node.getOpcode() >= ICONST_M1
                && node.getOpcode() <= ICONST_5)
            return node.getOpcode() - 3;
        if (node.getOpcode() == SIPUSH
                || node.getOpcode() == BIPUSH)
            return ((IntInsnNode) node).operand;
        if (node instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) node;
            if (ldc.cst instanceof Integer)
                return (int) ldc.cst;
        }
        return 0;
    }

    public static boolean isLong(AbstractInsnNode ain) {
        if (ain == null) return false;
        if (ain.getOpcode() == LCONST_0
                || ain.getOpcode() == LCONST_1)
            return true;
        if (ain instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) ain;
            return ldc.cst instanceof Long;
        }
        return false;
    }

    public static long getLongValue(AbstractInsnNode node) {
        if (node.getOpcode() >= LCONST_0
                && node.getOpcode() <= LCONST_1)
            return node.getOpcode() - 9;
        if (node instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) node;
            if (ldc.cst instanceof Long)
                return (long) ldc.cst;
        }
        return 0;
    }

    public static long[] grabLongArray(InstructionCapture capture) {

        AbstractInsnNode node = capture.get();

        long[] array;

        if (isInteger(node.getPrevious())) {

            capture.add(node.getPrevious());

            array = new long[getIntValue(node.getPrevious())];

            int found = 0;

            capture.increment();

            node = capture.get();

            while (true) {
                if (node.getOpcode() == DUP &&
                    isInteger(node.getNext()) &&
                    isLong(node.getNext().getNext()) &&
                    node.getNext().getNext().getNext().getOpcode() == LASTORE) {
                    array[getIntValue(node.getNext())] = getLongValue(node.getNext().getNext());

                    capture.capture(node, node.getNext().getNext().getNext().getNext());

                    found++;
                } else {
                    break;
                }
                node = node.getNext().getNext().getNext().getNext();
            }

            if (found != array.length) {
                throw new RuntimeException("Array length mismatch, " + found + " != " + array.length);
            }

            capture.update(node);

            return array;
        }

        return null;
    }

    public static String grabString(InstructionCapture capture) {

        AbstractInsnNode ain = capture.get();

        if (ain.getOpcode() == LDC) {
            LdcInsnNode ldc = (LdcInsnNode) ain;
            if (ldc.cst instanceof String) {
                capture.increment();
                return (String) ldc.cst;
            }
        }

        return null;
    }
}
