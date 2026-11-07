package org.example;


import org.example.config.ConfigException;
import org.example.config.ConfigLoader;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        ConfigLoader obj = new ConfigLoader();
        try{
            Map<String, Object> map = obj.readYaml(args[0]);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                System.out.println(entry.getKey() + " - " + entry.getValue());
            }
        } catch (ConfigException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

    }
}