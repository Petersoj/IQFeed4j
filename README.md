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
- Fundamental data
- Real-time streaming news

This library is community developed. If you have any questions, please ask them on [Github Discussions](https://github.com/Petersoj/IQFeed4j/discussions). If you have discovered an issue, open a new issue [here](https://github.com/Petersoj/IQFeed4j/issues).

Give this repository a star ⭐ if it helped you build an awesome trading algorithm in Java!

# Gradle and Maven Integration
If you are using Gradle as your build tool, add the following dependency to your `build.gradle` file:

```
dependencies {
    implementation group: 'net.jacobpeterson', name: 'iqfeed4j', version: '6.2-1.1'
}
```

If you are using Maven as your build tool, add the following dependency to your `pom.xml` file:

```
<dependency>
    <groupId>net.jacobpeterson</groupId>
    <artifactId>iqfeed4j</artifactId>
    <version>6.2-1.1</version>
    <scope>compile</scope>
</dependency>
```

Note that you don't have to use the Maven Central artifacts and instead can just install a clone of this project to your local Maven repository as shown in the [Building](#building) section.

# Configuration
Creating an `iqfeed4j.properties` file on the classpath with the following format allows you to easily load properties using the `IQFeed4j()` default constructor:
```
# For IQConnectExecutable
iqconnect_command=<this is the command to start the IQConnect.exe program (e.g. wine /home/user/.IQFeed/iqconnect.exe)>
product_id=<supply your registered productID here>
application_version=<this is the version of YOUR application>
login=<IQFeed Datafeed account login ID>
password=<IQFeed Datafeed account password>
autoconnect=<specifies that IQConnect should automatically try to connect to the server when launched. 'true' or 'false'>
save_login_info=<specifies that IQConnect should save the user's login ID and password between runs of IQConnect. 'true' or 'false'>

# For Feeds
feed_name=<name of the feeds that IQFeed4j makes to IQConnect>
feed_hostname=<the hostname of IQConnect>
level_1_feed_port=<port number>
market_depth_feed_port=<port number>
derivative_feed_port=<port number>
admin_feed_port=<port number>
lookup_feed_port=<port number>
```
The default values for `iqfeed4j.properties` can be found [here](https://github.com/Petersoj/IQFeed4j/blob/6.2/src/main/resources/iqfeed4j.default.properties).

# Logging
For logging, this library uses [SLF4j](http://www.slf4j.org/) which serves as an interface for various logging frameworks. This enables you to use whatever logging framework you would like. However, if you do not add a logging framework as a dependency in your project, the console will output a message stating that SLF4j is defaulting to a no-operation (NOP) logger implementation. To enable logging, add a logging framework of your choice as a dependency to your project such as [Log4j 2](http://logging.apache.org/log4j/2.x/index.html), [SLF4j-simple](http://www.slf4j.org/manual.html), or [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/).

# Usage

Note that the examples here are not exhaustive. Refer to the [IQFeed4j Javadoc](https://javadoc.io/doc/net.jacobpeterson/iqfeed4j) for all classes and method signatures.

## IQFeed4j
`IQFeed4j` is a class that contains feed instances to interface with IQFeed along with an instance of `IQConnectExecutable`. You will generally only need one instance of it in your application. Directly interact with the various feeds that IQFeed4j provides via: `getFeedName();` or `startFeedName();` and `stopFeedName();`.

It contains various constructors that allow you to specify various parameters for the feeds (e.g. port, hostname). The no-args constructor will use the properties specified in the `iqfeed4j.properties` file on the classpath as discussed in the [Configuration section](#configuration). 

## IQConnectExecutable
`IQConnectExecutable` provides a convenient way to start/stop the IQConnect.exe program. This is the program that accepts TCP/IP connections to interface with IQFeed programmatically.

It contains various constructors that allow you to specify various parameters for the executable (e.g. command to execute, process parameters). The no-args constructor will use the properties specified in the `iqfeed4j.properties` file on the classpath as discussed in the [Configuration section](#configuration).

## Streaming Feeds

### `Level1Feed`
This feed is used for real-time Level 1 market data.

The following example will change what fields are sent when a `SummaryUpdate` occurs on this feed. 
```java
try {
    iqFeed4j.getLevel1Feed().selectUpdateFieldNames(
            SummaryUpdateField.LAST,
            SummaryUpdateField.LAST_SIZE,
            SummaryUpdateField.LAST_MARKET_CENTER,
            SummaryUpdateField.LAST_DATE,
            SummaryUpdateField.LAST_TIME);
} catch (IOException exception) {
    exception.printStackTrace();
}
```

The following example will listen to AAPL trades and print Apple's most recent split:
```java
try {
    iqFeed4j.getLevel1Feed().requestWatchTrades("AAPL",
            new FeedMessageAdapter<FundamentalData>() {
                @Override
                public void onMessageReceived(FundamentalData fundamentalData) {
                    System.out.printf("Apple's most recent %.2f split was on %s.\n",
                            fundamentalData.getSplitFactor1(), fundamentalData.getSplitFactor1Date());
                }
            }, new FeedMessageAdapter<SummaryUpdate>() {
                @Override
                public void onMessageReceived(SummaryUpdate summaryUpdate) {
                    System.out.printf("AAPL trade occurred with %d shares at $%.4f with the market center code" +
                                    " %d at %s %s with a latency of %s milliseconds.\n",
                            summaryUpdate.getLastSize(),
                            summaryUpdate.getLast(),
                            summaryUpdate.getLastMarketCenter(),
                            summaryUpdate.getLastDate(),
                            summaryUpdate.getLastTime(),
                            ChronoUnit.MILLIS.between(summaryUpdate.getLastTime(), LocalTime.now(ZoneId.of("America/New_York"))));
                }
            });
} catch (IOException exception) {
    exception.printStackTrace();
}
```

### `MarketDepthFeed`
Not implemented yet. See [TODO](#todo).

### `DerivativeFeed`
This feed allows you to stream real-time interval bar data derived from Level 1 data.

The following example will listen to AAPL 1-minute interval bars during Eastern market hours and print them out:
```java
try {
    iqFeed4j.getDerivativeFeed().requestIntervalWatch(
            "AAPL",
            60,
            LocalDateTime.now(),
            null,
            null,
            LocalTime.of(9, 30),
            LocalTime.of(12 + 4, 0),
            IntervalType.SECONDS,
            null,
            new FeedMessageAdapter<Interval>(){
                @Override
                public void onMessageReceived(Interval interval) {
                    System.out.printf("Received AAPL interval %s\n", interval);
                }
            });
} catch (IOException exception) {
    exception.printStackTrace();
}
```

### `AdminFeed`
This feed allows you to manage the IQFeed connection in various ways and retrieve connection information.

The following example demonstrates a few things you can do with the `AdminFeed`.
```java
try {
    // Set the login ID and password (and wait for confirmation response)
    String currentLoginID = iqFeed4j.getAdminFeed().setLoginID("<login ID>").get();
    String currentPassword = iqFeed4j.getAdminFeed().setPassword("<password>").get();
    System.out.println(currentLoginID);
    System.out.println(currentPassword);

    // Get the next occurrence of the 'FeedStatistics' message
    System.out.printf("Feed stats: %s\n", iqFeed4j.getAdminFeed().getNextFeedStatistics().get());

    // Print all 'ClientStatistics' after 5 seconds
    iqFeed4j.getAdminFeed().setClientStatsOn();
    Thread.sleep(5000);
    iqFeed4j.getAdminFeed().getClientStatisticsOfClientIDs().entrySet().forEach(System.out::println);
} catch (IOException | ExecutionException | InterruptedException exception) {
    exception.printStackTrace();
}
```

## Lookup Feeds

### `HistoricalFeed`
This feed allows you to request historical ticks (trades) and intervals. 

The following example will request ascending AAPL 1-minute interval bars during market hours from 6/10/2021 to 6/11/2021.
```java
try {
    // Using synchronous data consumption method (aka consume data as it is being read)
    iqFeed4j.getHistoricalFeed().requestIntervals(
            "AAPL",
            60,
            LocalDateTime.of(2021, 6, 10, 9, 30),
            LocalDateTime.of(2021, 6, 11, 12 + 4, 0),
            null,
            LocalTime.of(9, 30),
            LocalTime.of(12 + 4, 0),
            DataDirection.OLDEST_TO_NEWEST,
            IntervalType.SECONDS,
            new MultiMessageAdapter<Interval>() {
                @Override
                public void onMessageReceived(Interval minuteInterval) {
                    System.out.printf("Minute Interval: %s\n", minuteInterval);
                }
            });

    // Using data accumulation method (aka all data is read into memory to be consumed asynchronously)
    Iterator<Interval> aaplIntervals = iqFeed4j.getHistoricalFeed().requestIntervals(
            "AAPL",
            60,
            LocalDateTime.of(2021, 6, 10, 9, 30),
            LocalDateTime.of(2021, 6, 11, 12 + 4, 0),
            null,
            LocalTime.of(9, 30),
            LocalTime.of(12 + 4, 0),
            DataDirection.OLDEST_TO_NEWEST,
            IntervalType.SECONDS);
    System.out.println("AAPL minute intervals:");
    aaplIntervals.forEachRemaining(System.out::println);
} catch (IOException | InterruptedException exception) {
    exception.printStackTrace();
} catch (ExecutionException executionException) {
    if (executionException.getCause() instanceof NoDataException) {
        System.err.println("No data was found for the requested Interval!");
    } else {
        executionException.printStackTrace();
    }
}
```

### `MarketSummaryFeed`
This feed allows you to retrieve a current summary/snapshot of various market data, both historically and currently.

The following example will print out all the `FiveMinuteSnapshot`s of NASDAQ equities.
```java
try {
    Iterator<FiveMinuteSnapshot> fiveMinuteSnapshots = iqFeed4j.getMarketSummaryFeed().request5MinuteSummary(
            "1", // Equity security type
            "5"); // NASDAQ group ID
    fiveMinuteSnapshots.forEachRemaining(System.out::println);
} catch (IOException | ExecutionException | InterruptedException exception) {
    exception.printStackTrace();
}
```

### `NewsFeed`
This feed allows you to retrieve historical news data on various tickers.

The following example gets all the `NewsConfiguration`s and acquires all the current `NewsMinorSource`s from it and then fetches `NewsHeadlines` for AAPL and TSLA on 6/10/2021 and prints them out.
```java
try {
    NewsConfiguration newsConfiguration = iqFeed4j.getNewsFeed().requestNewsConfiguration();
    List<String> allMinorNewsSources = newsConfiguration.getCategories().stream()
            .flatMap(newsCategory -> newsCategory.getMajorSources().stream())
            .flatMap(newsMajorSource -> newsMajorSource.getMinorSources().stream())
            .map(NewsMinorSource::getID)
            .collect(Collectors.toList());

    NewsHeadlines newsHeadlines = iqFeed4j.getNewsFeed().requestNewsHeadlines(
            allMinorNewsSources,
            Arrays.asList("AAPL", "TSLA"),
            null,
            Collections.singletonList(LocalDate.of(6, 10, 2021)),
            null);

    newsHeadlines.getHeadlines().forEach(System.out::println);
} catch (IOException | ExecutionException | InterruptedException exception) {
    exception.printStackTrace();
}
```

### `OptionChainsFeed`
This feed allows you to retrieve Option chain data including: equity Option contacts, Future contracts, Future Option contracts, Future Spreads.

The following example gets 5 In-The-Money and 5 Out-Of-The-Money Equity Put and Call Option contracts for AAPL expiring in June and prints them out.
```java
try {
    List<OptionContract> applOptions = iqFeed4j.getOptionChainsFeed().getEquityOptionChainWithITMOTMFilter(
            "AAPL",
            PutsCallsOption.PUTS_AND_CALLS,
            Arrays.asList(EquityOptionMonth.JUNE_PUT, EquityOptionMonth.JUNE_CALL),
            null,
            5,
            5,
            NonStandardOptionTypes.INCLUDE).get();
    applOptions.forEach(System.out::println);
} catch (IOException | InterruptedException | ExecutionException exception) {
    exception.printStackTrace();
}
```

### `SymbolMarketInfoFeed`
This feed allows you to retrieve current symbol information and current market information.

The following example gets all the current `ListedMarket`s and prints them out. 
```java
try {
    Iterator<ListedMarket> listedMarkets = iqFeed4j.getSymbolMarketInfoFeed().requestListedMarkets();
    listedMarkets.forEachRemaining(System.out::println);
} catch (IOException | InterruptedException | ExecutionException exception) {
    exception.printStackTrace();
}
```


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
