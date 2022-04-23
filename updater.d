#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.2.0"
+/

/** 
 * This updater script can be used to quickly tag the current branch and push
 * that tag to the remote origin.
 */
module updater;

import dsh;

int main(string[] args) {
    if (args.length < 2) {
        stderr.writeln("Missing required tag argument.");
        return 1;
    }

    import std.string;
    import std.regex;

    auto tagRegex = ctRegex!(`^v\d+(?:\.\d+)*$`);
    string tag = args[1].strip;
    if (!matchFirst(tag, tagRegex)) {
        stderr.writefln!"The tag \"%s\" is not valid."(tag);
        return 1;
    }

    writefln!"Tag: %s"(tag);
    int r = run("git tag " ~ tag);
    if (r != 0) {
        stderr.writeln("Couldn't create tag.");
        return r;
    }
    writeln("Created tag.");
    r = run("git push origin " ~ tag);
    if (r != 0) {
        stderr.writeln("Couldn't push tag to remote repository.");
        return r;
    }
    writeln("Pushed tag to remote repository.");
    return 0;
}
