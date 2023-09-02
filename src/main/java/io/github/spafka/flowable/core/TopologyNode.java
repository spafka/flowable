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


    public class SkipList<E extends TopologyNode> extends ConcurrentSkipListSet<E> {

        @Override
        public String toString() {
            return this.stream().map(x -> (x.node.getId())).collect(Collectors.joining(",", "[", "]"));
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