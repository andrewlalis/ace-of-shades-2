#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.6.1"
+/

/**
 * Builds OS-dependent client application versions.
 */
module build_clients;

import dsh;
import std.array;
import std.algorithm;
import std.string;
import std.regex;
import std.path;

void main() {
    string[] profiles = [
        "linux-aarch64",
        "linux-amd64",
        "linux-arm",
        "linux-arm32",
        "macos-aarch64",
        "macos-x86_64",
        "windows-aarch64",
        "windows-amd64",
        "windows-x86"
    ];
    string outDir = "client-builds";
    removeIfExists(outDir);
    mkdir(outDir);
    foreach (profile; profiles) {
        print("Building profile: %s", profile);
        string cmd = format!"mvn -B -Plwjgl-natives-%s clean package"(profile);
        new ProcessBuilder()
            .outputTo(buildPath(outDir, "output-" ~ profile ~ ".txt"))
            .run(cmd);
        string jarFile = findFile("client/target", ".+-jar-with-dependencies\\.jar");
        string finalName = replaceFirst(baseName(jarFile), regex("jar-with-dependencies"), profile);
        copy(jarFile, outDir ~ "/" ~ finalName);
    }
}
