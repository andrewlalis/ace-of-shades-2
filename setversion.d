#!/usr/bin/rdmd
/** 
 * This module takes the main parent POM's version, and applies it to all child
 * modules.
 * 
 * While you can run this with `./setversion.d`, it's faster if you compile
 * with `dmd setversion.d` and then just run `./setversion`.
 */
module setversion;

import std.stdio;
import std.file : write, readText;

void main() {
    string newVersion = getMainVersion();
    writefln!"Setting all modules to version %s"(newVersion);
    string[] files = ["client/pom.xml", "core/pom.xml", "server/pom.xml"];
    foreach (pomFile; files) {
        string xml = replaceVersion(readText(pomFile), newVersion);
        write(pomFile, xml);
        writefln!"Updated %s to version %s"(pomFile, newVersion);
    }
}

string getMainVersion() {
    import std.file : readText;
    import std.regex;
    auto versionRegex = ctRegex!(`<version>(\S+)<\/version>`);
    auto c = matchFirst(readText("pom.xml"), versionRegex);
    return c[1];
}

string replaceVersion(string xml, string newVersion) {
    import std.regex;
    import std.string : strip, indexOf;
    auto versionRegex = ctRegex!(`<parent>[\s\S]*<version>(\S+)<\/version>[\s\S]*<\/parent>`);
    auto c = matchFirst(xml, versionRegex);
    if (!c.empty) {
        string currentVersion = c[1];
        auto hitIndex = c.hit.indexOf(currentVersion);
        string prefix = xml[0 .. c.pre.length + hitIndex];
        string suffix = xml[c.pre.length + hitIndex + currentVersion.length .. $];
        return prefix ~ newVersion ~ suffix;
    }
    return xml;
}
