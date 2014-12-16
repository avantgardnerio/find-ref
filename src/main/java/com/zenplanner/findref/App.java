package com.zenplanner.findref;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {

        // Command line options
        Options options = new Options();
        options.addOption(
                OptionBuilder.withArgName("file").hasArg()
                        .withDescription("graphml file to walk")
                        .create('F')
        );
        options.addOption(
                OptionBuilder.withArgName("root").hasArg()
                        .withDescription("root node from which to walk graph")
                        .create('R')
        );
        options.addOption(
                OptionBuilder.withArgName("direction").hasArg()
                        .withDescription("direction to walk [IN | OUT]")
                        .create('D')
        );
        options.addOption(
                OptionBuilder.withArgName("include").hasArg()
                        .withDescription("only include results that match this value")
                        .create("include")
        );
        options.addOption(
                OptionBuilder.withArgName("exclude").hasArg()
                        .withDescription("exclude results that match this value")
                        .create("exclude")
        );
        options.addOption(
                OptionBuilder.withLongOpt("depth").hasArg()
                        .withDescription("maximum depth to walk")
                        .create("depth")
        );
        options.addOption(
                OptionBuilder.withArgName("help")
                        .withDescription("print help message")
                        .create('?')
        );
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);
        if(cmd.hasOption('?')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "find-ref", options );
            return;
        }

        // Parse arguments
        String path = cmd.getOptionValue('F');
        String rootName = cmd.getOptionValue('R');
        Direction dir = Direction.valueOf(cmd.getOptionValue('D', "OUT"));
        int maxDepth = cmd.hasOption("depth") ? Integer.parseInt(cmd.getOptionValue("depth")) : Integer.MAX_VALUE;
        String include = cmd.getOptionValue("include");
        String exclude = cmd.getOptionValue("exclude");

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
        walk(newGraph, root, dir, maxDepth, include, exclude);

        // Write
        try (FileOutputStream out = new FileOutputStream(new File("out.graphml"))) {
            GraphMLWriter.outputGraph(newGraph, out);
        }
    }

    private static void walk(TitanGraph out, Vertex root, Direction dir, int maxDepth, String include, String exclude) {
        Map<Vertex, Vertex> visited = new HashMap<>();
        walk(visited, out, root, dir, maxDepth, 1, include, exclude);
    }

    private static Vertex walk(Map<Vertex, Vertex> visited, TitanGraph newGraph, Vertex oldParent, Direction dir, int maxDepth, int depth, String include, String exclude) {
        // Don't cycle
        if (visited.containsKey(oldParent)) {
            return visited.get(oldParent);
        }
        Vertex newParent = newGraph.addVertex(oldParent);
        copyVertex(oldParent, newParent);
        visited.put(oldParent, newParent);

        // Scan children
        if (depth < maxDepth) {
            for (Edge oldEdge : oldParent.getEdges(dir, "child")) {
                Vertex oldChild = oldEdge.getVertex(dir.opposite());
                String oldPath = oldChild.getProperty("path");
                if(include != null && !oldPath.contains(include)) {
                    continue;
                }
                if(exclude != null && oldPath.contains(exclude)) {
                    continue;
                }
                Vertex newChild = walk(visited, newGraph, oldChild, dir, maxDepth, depth + 1, include, exclude);
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
