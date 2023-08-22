package io.github.spafka.flowable;

import java.util.ArrayList;
import java.util.List;

class TopologyNode {
    private String id;
    private String name;
    private List<TopologyNode> nextNodes;
    private List<TopologyNode> preNodes;

    public TopologyNode(String id, String name) {
        this.id = id;
        this.name = name;
        this.nextNodes = new ArrayList<>();
        this.preNodes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<TopologyNode> getNextNodes() {
        return nextNodes;
    }

    public List<TopologyNode> getPreNodes() {
        return preNodes;
    }

    public void addNextNode(TopologyNode node) {
        nextNodes.add(node);
    }

    public void addPreNode(TopologyNode node) {
        preNodes.add(node);
    }

    @Override
    public String toString() {
        return name;
    }
}

public class TopologyGraphExample {
    public static void main(String[] args) {
        // 创建拓扑图节点
        TopologyNode start = new TopologyNode("startEvent", "Start Event");
        TopologyNode userTask1 = new TopologyNode("userTask1", "User Task 1");
        TopologyNode userTask2 = new TopologyNode("userTask2", "User Task 2");
        TopologyNode end = new TopologyNode("endEvent", "End Event");

        // 构建节点之间的连接关系
        start.addNextNode(userTask1);
        userTask1.addPreNode(start);
        userTask1.addNextNode(userTask2);
        userTask2.addPreNode(userTask1);
        userTask2.addNextNode(end);
        end.addPreNode(userTask2);

        // 输出拓扑图节点及其连接关系
        printTopologyGraph(start);
    }

    private static void printTopologyGraph(TopologyNode node) {
        if (node == null) {
            return;
        }

        System.out.println("Node: " + node.getName());
        System.out.println("Next Nodes:");
        for (TopologyNode next : node.getNextNodes()) {
            System.out.println("\t" + next.getName());
        }

        System.out.println("Pre Nodes:");
        for (TopologyNode pre : node.getPreNodes()) {
            System.out.println("\t" + pre.getName());
        }

        System.out.println();

        // 递归打印下一个节点
        for (TopologyNode next : node.getNextNodes()) {
            printTopologyGraph(next);
        }
    }
}