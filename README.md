# Enjin Coin Minecraft Plugin

ECMP is an implementation of the Enjin Coin Java SDK.
The primary goal of this project is to serve as an example for plugin developers on how to integrate Enjin Coin technology into Minecraft servers.
We encourage plugin developers to expand and build on this framework to create fun and engaging experiences for the Minecraft community.

## Useful Resources

Released versions of ECMP can be downloaded [here](https://github.com/enjin/enjin-coin-minecraft-framework/releases).

## Building

ECMP uses Gradle to manage dependencies and building.

#### Requirements
* Java 8 JDK or newer
* Git or Git enabled tools

#### Building

```shell script
git clone https://github.com/enjin/enjin-coin-minecraft-framework.git
cd enjin-coin-minecraft-framework
./gradlew build
```

You will find build artifacts in the `build/libs` directory of the sub-modules.
The production jar ends with the `-all` classifier.
In addition to the production jar, source and javadoc jars will also be created.

## Contributing

#### Pull Requests

If you make any changes or improvements to the plugin which you believe would be beneficial to others, consider making a pull request to merge your changes back into the upstream project, particularly if your changes are bug fixes!

#### Project Layout

The project consists of the following modules.

* server - implementation for the spigot server platform.

## License

ECMP is licensed under the Apache License 2.0. Please see [`LICENSE.txt`](https://github.com/enjin/enjin-coin-minecraft-framework/blob/master/LICENSE) for more info.