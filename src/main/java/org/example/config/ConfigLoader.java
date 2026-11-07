package org.example.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


public class ConfigLoader {
    private Object valueViaKey(Map<String, Object> data, String key){
        if (!(data.containsKey(key))){
            throw new ConfigException("Missing parameter: " + key);
        }

        return data.get(key);
    }

    private Map<String, Object> readYaml(String path){
        Yaml yaml = new Yaml();
        try(InputStream inputStream = Files.newInputStream(Path.of(path))){
            Object data = yaml.load(inputStream);
            if (!(data instanceof Map)){
                throw new ConfigException("Config must contain key-value pairs");
            }
            return (Map<String, Object>) data;
        } catch (IOException e){
            throw new ConfigException("Cannot read file: " + path );
        } catch (YAMLException e) {
            throw new ConfigException("Invalid YAML in file: " + path);
        }
    }

    private String getString(Map<String, Object> data, String key){

        Object value = valueViaKey(data, key);

        if (!(value instanceof String)){
            throw new ConfigException("Parameter " + key + " must be a string");
        } else{
            return (String) value;
        }
    }

    private boolean getBoolean(Map<String, Object> data, String key){
        Object value = valueViaKey(data, key);

        if (!(value instanceof Boolean)){
            throw new ConfigException("Parameter " + key + " must be a boolean");
        } else{
            return (boolean) value;
        }
    }

    private int getInteger(Map<String, Object> data, String key){
        Object value = valueViaKey(data, key);

        if (!(value instanceof Integer)){
            throw new ConfigException("Parameter " + key + " must be an integer");
        } else{
            return (int) value;
        }
    }

    public Config load(String path){
        Map<String, Object> data = readYaml(path);
        String packageName = getString(data, "packageName");
        String repository = getString(data, "repository");
        String outputFile = getString(data, "outputFile");
        String filter = getString(data, "filter");
        boolean testMode = getBoolean(data, "testMode");
        boolean asciiTree = getBoolean(data, "asciiTree");
        int maxDepth = getInteger(data, "maxDepth");
        return new Config(packageName, repository, outputFile, filter, testMode, asciiTree, maxDepth);
    }
}
