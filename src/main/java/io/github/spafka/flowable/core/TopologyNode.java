package io.github.spafka.flowable.core;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowElement;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class TopologyNode<T extends BaseElement> implements Comparable<TopologyNode<T>> {
    public T node;
    public SkipList<TopologyNode<T>> next = new SkipList<>();
    public SkipList<TopologyNode<T>> pre = new SkipList<>();

    public SkipList<TopologyNode<T>> gateways = new SkipList<>();

   public Set<TopologyNode<T>> forks = new HashSet<>();
   public TopologyNode<T> join;

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

    public void addGate(TopologyNode<T> gate) {
        gateways.add(gate);
    }
    public void addFork(TopologyNode<T> gate) {

        gateways.add(gate);
        forks.add(gate);
    }
    public void addNext(TopologyNode<T> source) {
        next.add(source);
    }

    @Override
    public String toString() {
        return node.getId();
    }
}