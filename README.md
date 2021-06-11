<p align="center"><a href="https://petersoj.github.io/IQFeed4j/" target="_blank"><img src="https://i.imgur.com/4Gx8Y25.png" alt="Logo"></a></p>
<p align="center">
    <a href="https://search.maven.org/artifact/net.jacobpeterson/iqfeed4j" target="_blank"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/net.jacobpeterson/iqfeed4j"></a>
    <a href="https://javadoc.io/doc/net.jacobpeterson/iqfeed4j" target="_blank"><img src="https://javadoc.io/badge/net.jacobpeterson/iqfeed4j.svg" alt="Javadocs"></a>
    <a href="https://travis-ci.com/github/Petersoj/IQFeed4j" target="_blank"><img src="https://travis-ci.com/Petersoj/IQFeed4j.svg?branch=6.2" alt="Build Status"></a>
    <a href="https://opensource.org/licenses/MIT" target="_blank"><img alt="Github License" src="https://img.shields.io/github/license/petersoj/IQFeed4j"></a>    
</p>

# Overview

IQFeed4j is a Java API for the market-data-vendor DTN IQFeed. IQFeed provides a wide variety of market data including: 
- Real-time tick-by-tick data on US and Canadian equities 
- Real-time equity/index Options and Forex data
- OHLCV historical data
- Fundamental Data
- Real-time streaming news

This library is community developed. If you have any questions, please ask them on [Github Discussions](https://github.com/Petersoj/IQFeed4j/discussions). If you have discovered an issue, open a new issue [here](https://github.com/Petersoj/IQFeed4j/issues).

Give this repository a star ⭐ if it helped you build an awesome trading algorithm in Java!

# Gradle and Maven Integration
If you are using Gradle as your build tool, add the following dependency to your `build.gradle` file:

```
dependencies {
    implementation group: 'net.jacobpeterson', name: 'iqfeed4j', version: '6.2-1.0'
}
```

If you are using Maven as your build tool, add the following dependency to your `pom.xml` file:

```
<dependency>
    <groupId>net.jacobpeterson</groupId>
    <artifactId>iqfeed4j</artifactId>
    <version>6.2-1.0</version>
    <scope>compile</scope>
</dependency>
```

Note that you don't have to use the Maven Central artifacts and instead can just install a clone of this project to your local Maven repository as shown in the [Building](#building) section.

# Configuration
Creating an `iqfeed4j.properties` file on the classpath with the following format allows you to easily load properties using the `IQFeed4j()` default constructor:
```
application_version=1.0
autoconnect=true
save_login_info=true

feed_name=IQFeed4j
feed_hostname=localhost
level_1_feed_port=5009
market_depth_feed_port=9200
derivative_feed_port=9400
admin_feed_port=9300
lookup_feed_port=9100
```
The default values for `iqfeed4j.properties` can be found [here](https://github.com/Petersoj/IQFeed4j/blob/6.2/src/main/resources/iqfeed4j.default.properties).

# Logging
For logging, this library uses [SLF4j](http://www.slf4j.org/) which serves as an interface for various logging frameworks. This enables you to use whatever logging framework you would like. However, if you do not add a logging framework as a dependency in your project, the console will output a message stating that SLF4j is defaulting to a no-operation (NOP) logger implementation. To enable logging, add a logging framework of your choice as a dependency to your project such as [Log4j 2](http://logging.apache.org/log4j/2.x/index.html), [SLF4j-simple](http://www.slf4j.org/manual.html), or [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/).

# Usage
TODO

# Building
To build this project yourself, clone this repository and run:
```
./gradlew build
```

To install built artifacts to your local maven repo, run:
```
./gradlew install
```

Contributions are welcome!

# TODO
- Level 2 Feed
- Unit testing
