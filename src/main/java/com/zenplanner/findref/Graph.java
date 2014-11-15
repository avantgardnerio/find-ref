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
