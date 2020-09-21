# Deprecation notice

In version 2020.2 of IDEA, native Maven Wrapper support has been included, therefore this plugin is considered deprecated.

Include Maven-Wrapper (https://github.com/takari/maven-wrapper) support in
IDEA.

# Usage

Simply install, no user configurable parts (yet), maven wrapper is automatically detected
and downloaded.

# Caveats

- If more than one (Top Level) Module is part of the project, only the main
module is considered (the one where the iml file / .idea folder resides)
- completely removing the wrapper should work, but is untested (resets to bundled version)




