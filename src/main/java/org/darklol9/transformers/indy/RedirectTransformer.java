package org.darklol9.transformers.indy;

import org.darklol9.transformers.AbstractTransformer;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RedirectTransformer extends AbstractTransformer {

    public RedirectTransformer(HashSet<ClassNode> classes) {
        super(classes);
    }

    @Override
    public void visit() {

        AtomicInteger counter = new AtomicInteger(0);

        for (ClassNode classNode : classes) {
            for (MethodNode method : classNode.methods) {
                for (AbstractInsnNode ain : method.instructions) {
                    if (ain instanceof MethodInsnNode) {
                        MethodInsnNode node = (MethodInsnNode) ain;

                        ClassNode targetClass = findClass(node.owner);
                        if (targetClass == null) continue;
                        MethodNode target = findMethod(targetClass, node);
                        if (target == null) continue;

                        if (Modifier.isStatic(target.access)) {

                            AbstractInsnNode first = target.instructions.getFirst();
                            MethodInsnNode potential = null;
                            boolean call = false;
                            while (first != target.instructions.getLast()) {

                                if (first instanceof LabelNode) {
                                    first = first.getNext();
                                    continue;
                                }

                                if (first instanceof MethodInsnNode) {
                                    call = true;
                                    potential = (MethodInsnNode) first.clone(null);
                                    first = first.getNext();
                                    continue;
                                }

                                if (!(first instanceof VarInsnNode)) {
                                    break;
                                }

                                VarInsnNode var = (VarInsnNode) first;
                                if (!(var.getOpcode() >= ILOAD && var.getOpcode() <= ALOAD)) {
                                    break;
                                }

                                first = first.getNext();

                            }

                            if (first == target.instructions.getLast() && call && potential != null) {
                                method.instructions.set(ain, potential);
                                counter.incrementAndGet();
                            }
                        }
                    }
                }
            }
        }

        log("Redirected " + counter.get() + " method call(s)");
    }
}
