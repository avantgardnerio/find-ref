package com.zenplanner.findref;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scanner {

    public static final Pattern regex = Pattern.compile("\\b[\\/\\w\\.\\\\]*\\b");
    public static final Set<String> validExt = new HashSet<>(Arrays.asList(new String[]{"cfm", "cfc"}));
    public static final Set<String> exclude = new HashSet<>(Arrays.asList(new String[]{".git", "mxunit", "web-inf", ".settings"})); // TODO: Un-hard-code

    public Graph collect(File root) {
        Graph graph = new Graph();
        collect(root, root, graph);
        return graph;
    }

    public void link(File root, Graph graph) throws Exception {
        for (String relative : graph.keySet()) {
            File file = new File(root, relative);
            if (file.isDirectory()) {
                continue;
            }
            File folder = file.getParentFile();
            String path = file.getAbsolutePath();
            String text = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
            Matcher m = regex.matcher(text);
            while (m.find()) {
                String grp = m.group();
                if (grp == null || grp.length() == 0) {
                    continue;
                }
                grp = normalizeFolderSeperator(grp);

                // Add files in folder
                if (addNestedFiles(graph, relative, grp)) {
                    continue;
                }

                // Add CF references
                grp = normalize(grp);
                if (addIfPresent(root, folder, graph, relative, grp)) {
                    continue;
                }
                if (addIfPresent(root, root, graph, relative, grp)) {
                    continue;
                }
            }
        }
    }

    private boolean addNestedFiles(Graph graph, String relative, String folderPath) {
        if (!isDir(folderPath)) {
            return false;
        }
        if (!graph.containsKey(folderPath)) {
            return false;
        }
        folderPath += "/";
        boolean found = false;
        for (String filePath : graph.keySet()) { // TODO: Fix big-O issue - Binary search for matching range?
            if(filePath.length() <= folderPath.length()) {
                continue; // File not in folder
            }
            String trunc = filePath.substring(0, Math.min(folderPath.length(), filePath.length()));
            if (!StringUtils.equals(folderPath, trunc)) {
                continue;
            }
            if(isDir(filePath)) {
                continue;
            }
            String rel = filePath.substring(folderPath.length());
            if(rel.contains("/")) {
                continue;
            }
            graph.addRelation(relative, filePath);
            found = true;
        }
        return found;
    }

    private void collect(File root, File parent, Graph graph) {
        for (File child : parent.listFiles()) {
            String relative = makeRelative(root, child);
            String filename = child.getName().toLowerCase();
            if (exclude.contains(filename)) {
                continue;
            }
            if (child.isDirectory()) {
                collect(root, child, graph);
                graph.add(relative);
            } else {
                String ext = FilenameUtils.getExtension(filename);
                if (!validExt.contains(ext)) {
                    continue;
                }
                graph.add(relative);
            }
        }
    }

    private static boolean isDir(String path) {
        if (path.endsWith(".cfc")) {
            return false;
        }
        if (path.endsWith(".cfm")) {
            return false;
        }
        return true;
    }

    private static String normalizeFolderSeperator(String path) {
        return path.replace('\\', '/');
    }

    private static String normalize(String grp) {
        String ext;
        if (grp.endsWith(".cfm")) {
            ext = ".cfm";
            grp = grp.substring(0, grp.length() - 4);
        } else if (grp.endsWith(".cfc")) {
            ext = ".cfc";
            grp = grp.substring(0, grp.length() - 4);
        } else {
            ext = ".cfc";
        }
        normalizeFolderSeperator(grp);
        grp = grp.replace('.', '/');
        grp = grp + ext;
        return grp;
    }

    private static boolean addIfPresent(File root, File folder, Graph graph, String relative, String grp) {
        String localRel = makeRelative(root, new File(folder, grp)).toLowerCase();
        if (graph.containsKey(localRel)) {
            graph.addRelation(relative, localRel);
            return true;
        }
        return false;
    }

    private static String makeRelative(File root, File child) {
        String path = root.toURI().relativize(child.toURI()).getPath().toLowerCase();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
