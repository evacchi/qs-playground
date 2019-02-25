package org.kie.maven.plugin;

import static java.util.Arrays.asList;

public enum BPMNModelMode {

    YES,
    NO;

    public static boolean shouldGenerateBPMNModel(String s) {
        return asList(YES).contains(valueOf(s.toUpperCase()));
    }
}

