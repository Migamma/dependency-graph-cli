package org.example;


import org.example.config.ConfigException;
import org.example.config.ConfigLoader;
import org.example.config.Config;


public class Main {
    public static void main(String[] args) {
        ConfigLoader loader = new ConfigLoader();
        try{
            Config parameters;
            if (args.length > 0){
                parameters = loader.load(args[0]);
            } else{
                parameters = loader.load("config.yaml");
            }
            System.out.println("packageName: " + parameters.packageName());
            System.out.println("repository: " + parameters.repository());
            System.out.println("outputFile: " + parameters.outputFile());
            System.out.println("filter: " + parameters.filter());
            System.out.println("testMode: " + parameters.testMode());
            System.out.println("asciiTree: " + parameters.asciiTree());
            System.out.println("maxDepth: " + parameters.maxDepth());
        } catch (ConfigException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

    }
}