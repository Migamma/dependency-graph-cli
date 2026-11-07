package org.example;


import org.example.config.ConfigLoader;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        ConfigLoader obj = new ConfigLoader();

        Map<String, Object> map = obj.readYaml("config.yaml");

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }
}