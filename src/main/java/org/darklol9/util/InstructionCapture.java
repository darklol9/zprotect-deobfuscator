package org.darklol9.util;

import lombok.AllArgsConstructor;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.LinkedList;

@AllArgsConstructor
public class InstructionCapture {

    private AbstractInsnNode current;
    private LinkedList<AbstractInsnNode> caught;

    public LinkedList<AbstractInsnNode> getCaught() {
        return caught;
    }

    public void update(AbstractInsnNode current) {
        this.current = current;
    }

    public void add(AbstractInsnNode current) {
        this.caught.add(current);
    }

    public AbstractInsnNode increment() {
        AbstractInsnNode old = this.current;
        add(old);
        update(old.getNext());
        return old;
    }

    public AbstractInsnNode get() {
        return this.current;
    }

    public void capture(AbstractInsnNode node, AbstractInsnNode next) {
        while (node != next) {
            add(node);
            node = node.getNext();
        }
    }
}
