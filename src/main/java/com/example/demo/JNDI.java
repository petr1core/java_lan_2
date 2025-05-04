package com.example.demo;

import javax.naming.Referenceable;

public class JNDI {
    public static void main(String[] args) {
        try {
            Class<?> clazz = Class.forName("javax.naming.Referenceable");
            System.out.println("JNDI Referenceable found: " + clazz);
        } catch (ClassNotFoundException e) {
            System.err.println("JNDI Referenceable not found: " + e.getMessage());
        }
    }
}

