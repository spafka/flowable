package io.github.spafka.flowable.core;

import org.flowable.bpmn.model.FlowElement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class TopologyNode<T extends FlowElement> implements Comparable<TopologyNode<T>> {
    public T node;
    public SkipList<TopologyNode<T>> next = new SkipList<>();
    public SkipList<TopologyNode<T>> pre = new SkipList<>();

    public TopologyNode(T node) {
        this.node = node;
    }

    @Override
    public int compareTo(TopologyNode<T> o) {
        return this.node.getId().compareTo(o.node.getId());
    }


    class SkipList<T extends TopologyNode> extends ConcurrentSkipListSet {

        @Override
        public String toString() {

            Object collect = this.stream().map(x -> (String) ((TopologyNode) x).node.getId()).collect(Collectors.toList());
            return String.join(",", (List)collect) ;
        }
    }

    public void addSource(TopologyNode<T> source) {
        pre.add(source);
    }

    public void addNext(TopologyNode<T> source) {
        next.add(source);
    }

    @Override
    public String toString() {
        return node.getId();
    }
}