package org.darklol9.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;

public abstract class AbstractTransformer implements Opcodes {

    protected HashSet<ClassNode> classes;

    public AbstractTransformer(HashSet<ClassNode> classes) {
        this.classes = classes;
    }

    public abstract void visit();

    public ClassNode findClass(String name) {
        for (ClassNode cn : classes) {
            if (cn.name.equals(name)) {
                return cn;
            }
        }
        return null;
    }

    public MethodNode findMethod(MethodInsnNode node) {
        for (ClassNode cn : classes) {
            return findMethod(cn, node);
        }
        return null;
    }

    public MethodNode findMethod(ClassNode cn, MethodInsnNode node) {
        if (node.owner.equals(cn.name)) {
            for (MethodNode mn : cn.methods) {
                if (mn.name.equals(node.name) && mn.desc.equals(node.desc)) {
                    return mn;
                }
            }
        }
        return null;
    }

    protected void log(String message, Object... args) {
        System.out.printf("[" + this.getClass().getSimpleName() + "] " + message + "\n", args);
    }
}
