package com.zenplanner.findref;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        // TODO: Replace with a standard solution: http://commons.apache.org/proper/commons-cli/usage.html
        String path = args[0];
        String rootName = args.length > 1 ? args[1] : null;
        Direction dir = args.length > 2 ? Direction.valueOf(args[2]) : Direction.OUT;
        int maxDepth = args.length > 3 ? Integer.parseInt(args[3]) : Integer.MAX_VALUE;

        // Read
        Configuration conf = new BaseConfiguration();
        conf.setProperty("storage.backend", "inmemory");
        TitanGraph oldGraph = TitanFactory.open(conf);
        try (FileInputStream in = new FileInputStream(new File(path))) {
            GraphMLReader.inputGraph(oldGraph, in);
        }
        TitanGraph newGraph = TitanFactory.open(conf);

        // Filter
        Vertex root = getVert(oldGraph, rootName);
        walk(newGraph, root, dir, maxDepth);

        // Write
        try (FileOutputStream out = new FileOutputStream(new File("out.graphml"))) {
            GraphMLWriter.outputGraph(newGraph, out);
        }
    }

    private static void walk(TitanGraph out, Vertex root, Direction dir, int maxDepth) {
        Map<Vertex, Vertex> visited = new HashMap<>();
        walk(visited, out, root, dir, maxDepth, 1);
    }

    private static Vertex walk(Map<Vertex, Vertex> visited, TitanGraph newGraph, Vertex oldParent, Direction dir, int maxDepth, int depth) {
        // Don't cycle
        if (visited.containsKey(oldParent)) {
            return visited.get(oldParent);
        }
        Vertex newParent = newGraph.addVertex(oldParent);
        copyVertex(oldParent, newParent);
        visited.put(oldParent, newParent);

        // Scan children
        if(depth < maxDepth) {
            for (Edge oldEdge : oldParent.getEdges(dir, "child")) {
                Vertex oldChild = oldEdge.getVertex(dir.opposite());
                Vertex newChild = walk(visited, newGraph, oldChild, dir, maxDepth, depth+1);
                newGraph.addEdge(null, newParent, newChild, "child");
            }
        }

        return newParent;
    }

    public static void copyVertex(Vertex oldVert, Vertex newVert) {
        for (String key : oldVert.getPropertyKeys()) {
            Object val = oldVert.getProperty(key);
            newVert.setProperty(key, val);
        }
    }

    private static Vertex getVert(TitanGraph graph, String rootName) {
        Vertex root = null;
        for (Vertex vert : graph.getVertices("path", rootName)) {
            if (root != null) {
                throw new RuntimeException("Multiple vertices found!");
            }
            root = vert;
        }
        if (root == null) {
            throw new RuntimeException("No vertices found!");
        }
        return root;
    }

}
