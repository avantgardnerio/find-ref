package com.zenplanner.findref;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class App {

    public static void main(String[] args) throws Exception {
        String path = args[0];
        String file = args.length > 1 ? args[1] : null;
        String dir = args.length > 2 ? args[2] : "forward";
        int maxDepth = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        File root = new File(path);

        Scanner scanner = new Scanner();
        Graph graph = scanner.collect(root);
        scanner.link(root, graph);
        if (file != null && StringUtils.equals(dir, "forward")) {
            graph = graph.forward(file, maxDepth);
        }
        if (file != null && StringUtils.equals(dir, "backward")) {
            graph = graph.backward(file, maxDepth);
        }
        File outFile = new File("out.gv");
        graph.write(outFile);
        Process p = Runtime.getRuntime().exec("gvedit " + outFile.getAbsolutePath());
        p.waitFor();
    }

}
