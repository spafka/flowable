package io.github.spafka.flowable;

import org.flowable.bpmn.model.FlowElement;

import java.util.ArrayList;
import java.util.List;

public class TopologyNode<T extends FlowElement> {
    public T node;
    public List<TopologyNode<T>> next = new ArrayList<>();
    public List<TopologyNode<T>> pre = new ArrayList<>();

    long ts;

    public TopologyNode(T node) {
        this.node = node;
    }


    public String printTree() {
        return printTreeHelper(this, "");
    }

    private String printTreeHelper(TopologyNode<T> node, String indent) {
        StringBuilder sb = new StringBuilder(indent + node.node.getName());
        for (TopologyNode<T> child : node.next) {
            sb.append("\n" + printTreeHelper(child, indent + "  ")); // 使用两个空格进行缩进
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return printTree();
    }
}
