package org.example.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


public class ConfigLoader {

    public Map<String, Object> readYaml(String path){
        Yaml yaml = new Yaml();
        try(InputStream inputStream = Files.newInputStream(Path.of(path))){
            return yaml.load(inputStream);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
