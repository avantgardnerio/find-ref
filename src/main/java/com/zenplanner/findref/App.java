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
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class App {

    public static void main(String[] args) throws Exception {
        // TODO: Replace with a standard solution: http://commons.apache.org/proper/commons-cli/usage.html
        String path = args[0];
        String rootName = args.length > 1 ? args[1] : null;
        Direction dir = args.length > 2 ? Direction.valueOf(args[2]) : Direction.OUT;

        // Read
        Configuration conf = new BaseConfiguration();
        conf.setProperty("storage.backend", "inmemory");
        TitanGraph oldGraph = TitanFactory.open(conf);
        try(FileInputStream in = new FileInputStream(new File(path))) {
            GraphMLReader.inputGraph(oldGraph, in);
        }
        TitanGraph newGraph = TitanFactory.open(conf);

        // Filter
        Vertex root = getVert(oldGraph, rootName);
        walk(newGraph, root, dir);

        // Write
        try(FileOutputStream out = new FileOutputStream(new File("out.graphml"))) {
            GraphMLWriter.outputGraph(newGraph, out);
        }
    }

    private static void walk(TitanGraph out, Vertex root, Direction dir) {
        Map<Vertex, Vertex> visited = new HashMap<>();
        walk(visited, out, root, dir);
    }

    private static Vertex walk(Map<Vertex, Vertex> visited, TitanGraph newGraph, Vertex oldParent, Direction dir) {
        // Don't cycle
        if(visited.containsKey(oldParent)) {
            return visited.get(oldParent);
        }
        Vertex newParent = newGraph.addVertex(oldParent);
        copyVertex(oldParent, newParent);
        visited.put(oldParent, newParent);

        // Scan children
        for(Edge oldEdge : oldParent.getEdges(dir, "child")) {
            Vertex oldChild = oldEdge.getVertex(dir.opposite());
            Vertex newChild = walk(visited, newGraph, oldChild, dir);
            newGraph.addEdge(null, newParent, newChild, "child");
        }

        return newParent;
    }

    public static void copyVertex(Vertex oldVert, Vertex newVert) {
        for(String key : oldVert.getPropertyKeys()) {
            Object val = oldVert.getProperty(key);
            newVert.setProperty(key, val);
        }
    }

    private static Vertex getVert(TitanGraph graph, String rootName) {
        Vertex root = null;
        for(Vertex vert : graph.getVertices("path", rootName)) {
            if(root != null) {
                throw new RuntimeException("Multiple vertices found!");
            }
            root = vert;
        }
        if(root == null) {
            throw new RuntimeException("No vertices found!");
        }
        return root;
    }

}
