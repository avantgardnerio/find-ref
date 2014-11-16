package com.zenplanner.findref;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Graph {
    private Map<String,Set<String>> forward = new TreeMap<>();
    private Map<String,Set<String>> backward = new TreeMap<>();

    public Graph forward(String start) {
        Graph other = new Graph();
        Set<String> visited = new HashSet<>();
        forward(other, start, visited, 1);
        return other;
    }

    public Map<String,Integer> findDepths() {
        Map<String,Integer> map = new TreeMap<>();
        for(String key : keySet()) {
            Graph other = new Graph();
            Set<String> visited = new HashSet<>();
            int depth = forward(other, key, visited, 1);
            map.put(key, depth);
        }
        return map;
    }

    private int forward(Graph other, String current, Set<String> visited, int depth) {
        int maxDepth = depth;

        // Don't revisit
        if(visited.contains(current)) {
            return maxDepth;
        }
        visited.add(current);

        Set<String> dependencies = this.getDependencies(current);
        if(dependencies == null) {
            return maxDepth;
        }
        for(String dependency : dependencies) {
            other.addRelation(current, dependency);
        }
        for(String dependency : dependencies) {
            maxDepth = Math.max(maxDepth, forward(other, dependency, visited, depth + 1));
        }
        return maxDepth;
    }

    public void add(String name) {
        if(!forward.containsKey(name)) {
            forward.put(name, new HashSet<>());
        }
        if(!backward.containsKey(name)) {
            backward.put(name, new HashSet<>());
        }
    }

    public boolean containsKey(String name) {
        return forward.containsKey(name);
    }

    public Set<String> keySet() {
        return forward.keySet();
    }

    public void addRelation(String dependor, String dependant) {
        add(dependor);
        add(dependant);
        forward.get(dependor).add(dependant);
        backward.get(dependant).add(dependor);
    }

    public Set<String> getDependencies(String node) {
        return forward.get(node);
    }

    public Set<String> getDependors(String node) {
        return backward.get(node);
    }

    public void write() throws Exception {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File("out.gv")))) {
            writer.write("digraph abstract {\n");
            for(String parent : this.keySet()) {
                Set<String> children = this.getDependencies(parent);
                for(String child : children) {
                    writer.write(gvSafe(parent) + " -> " + gvSafe(child) + ";\n");
                }
            }
            writer.write("}\n");
        }
    }

    public static String gvSafe(String parent) {
        parent = parent.replace('/', '_');
        parent = parent.replace('.', '_');
        parent = parent.replace('-', '_');
        return parent;
    }

}
