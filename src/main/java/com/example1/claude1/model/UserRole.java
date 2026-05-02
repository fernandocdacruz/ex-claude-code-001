package com.example1.claude1.model;

public enum UserRole {
    ADMIN("admin"),
    USER("user");

    private final String rule;

    UserRole(String rule) {
        this.rule = rule;
    }

    public String getRule() {
        return rule;
    }
}
