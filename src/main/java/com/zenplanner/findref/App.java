package com.zenplanner.findref;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {

    public static final Pattern regex = Pattern.compile("\\b[\\/\\w\\.\\\\]*\\b");
    public static final Set<String> validExt = new HashSet<>(Arrays.asList(new String[] {"cfm","cfc"}));
    public static final Set<String> exclude = new HashSet<>(Arrays.asList(new String[] {".git", "mxunit","web-inf"}));

    public static void main(String[] args) throws Exception {
        String path = args[0];
        File root = new File(path);
        Graph graph = new Graph();
        collect(root, root, graph);
        scan(root, graph);
        if(args.length > 1) {
            graph = graph.forward(args[1]);
        }
        graph.write();
    }

    public static void scan(File root, Graph graph) throws Exception {
        for(String relative : graph.keySet()) {
            Set<String> dependencies = graph.getDependencies(relative);
            File file = new File(root, relative);
            File folder = file.getParentFile();
            String path = file.getAbsolutePath();
            String text = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
            Matcher m = regex.matcher(text);
            while(m.find()) {
                // Normalize
                String grp = m.group();
                if(grp == null || grp.length() == 0) {
                    continue;
                }
                grp = normalize(grp);
                String localRel = makeRelative(root, new File(folder, grp)).toLowerCase();
                String rootRel = makeRelative(root, new File(root, grp)).toLowerCase();

                // Map
                if(graph.containsKey(localRel)) {
                    graph.addRelation(relative, localRel);
                    continue;
                }
                if(graph.containsKey(rootRel)) {
                    graph.addRelation(relative, rootRel);
                    continue;
                }
            }
        }
    }

    public static String normalize(String grp) {
        String ext = null;
        if(grp.endsWith(".cfm")) {
            ext = ".cfm";
            grp = grp.substring(0, grp.length()-4);
        }
        if(grp.endsWith(".cfc")) {
            ext = ".cfc";
            grp = grp.substring(0, grp.length()-4);
        }
        if(ext == null) {
            ext = ".cfc";
        }
        grp = grp.replace('\\', '/');
        grp = grp.replace('.', '/');
        grp = grp + ext;
        return grp;
    }

    public static void collect(File root, File parent, Graph graph) throws Exception {
        for(File child : parent.listFiles()) {
            String filename = child.getName().toLowerCase();
            if(exclude.contains(filename)) {
                continue;
            }
            if(child.isDirectory()) {
                collect(root, child, graph);
            } else {
                String ext = FilenameUtils.getExtension(filename);
                if(!validExt.contains(ext)) {
                    continue;
                }
                //String className = FilenameUtils.removeExtension(filename);
                String relative = makeRelative(root, child);
                graph.add(relative.toLowerCase());
            }
        }
    }

    public static String makeRelative(File root, File child) {
        return root.toURI().relativize(child.toURI()).getPath();
    }
}
