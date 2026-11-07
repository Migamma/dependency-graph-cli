package org.example.config;

public record Config(String packageName, String repository, String outputFile, String filter, boolean testMode, boolean asciiTree, int maxDepth) {
}
