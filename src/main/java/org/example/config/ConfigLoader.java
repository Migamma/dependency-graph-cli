package org.example.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


public class ConfigLoader {

    public Map<String, Object> readYaml(String path){
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
}
