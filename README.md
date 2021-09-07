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
    implementation group: 'net.jacobpeterson', name: 'iqfeed4j', version: '6.2-1.5.1'
}
```

If you are using Maven as your build tool, add the following dependency to your `pom.xml` file:

```
<dependency>
    <groupId>net.jacobpeterson</groupId>
    <artifactId>iqfeed4j</artifactId>
    <version>6.2-1.5.1</version>
    <scope>compile</scope>
</dependency>
```

Note that you don't have to use the Maven Central artifacts and instead can just install a clone of this project to your local Maven repository as shown in the [Building](#building) section.

# Configuration
Creating an `iqfeed4j.properties` file on the classpath with the following format allows you to easily load properties using the [`IQFeed4j`](src/main/java/net/jacobpeterson/iqfeed4j/IQFeed4j.java) default constructor and the [`IQConnectExecutable`](src/main/java/net/jacobpeterson/iqfeed4j/executable/IQConnectExecutable.java) default constructor:
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

## [`IQFeed4j`](src/main/java/net/jacobpeterson/iqfeed4j/IQFeed4j.java)
[`IQFeed4j`](src/main/java/net/jacobpeterson/iqfeed4j/IQFeed4j.java) is a class that contains feed instances to interface with IQFeed along with an instance of [`IQConnectExecutable`](src/main/java/net/jacobpeterson/iqfeed4j/executable/IQConnectExecutable.java). You will generally only need one instance of it in your application. Directly interact with the various feeds that IQFeed4j provides with `feedName();` and start/stop the feeds with `startFeedName();` and `stopFeedName();`. You must start the feed with `startFeedName();` before using it via `feedName();`.

It contains various constructors that allow you to specify parameters for the feeds (e.g. port, hostname). The no-args constructor will use the properties specified in the `iqfeed4j.properties` file on the classpath as discussed in the [Configuration section](#configuration). 

## [`IQConnectExecutable`](src/main/java/net/jacobpeterson/iqfeed4j/executable/IQConnectExecutable.java)
[`IQConnectExecutable`](src/main/java/net/jacobpeterson/iqfeed4j/executable/IQConnectExecutable.java) provides a convenient way to start/stop the IQConnect.exe program. IQConnect.exe is the program that accepts TCP/IP connections to interface with IQFeed programmatically.

It contains various constructors that allow you to specify parameters for the executable (e.g. command to execute, process parameters). The no-args constructor will use the properties specified in the `iqfeed4j.properties` file on the classpath as discussed in the [Configuration section](#configuration).

## Streaming Feeds

### [`Level1Feed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/streaming/level1/Level1Feed.java)
This feed is used for real-time Level 1 market data.

The following example will change what fields are sent when a `SummaryUpdate` occurs on this feed, prints Apple's most recent split, and prints AAPL trades continuously:
```java
try {
    iqFeed4j.startLevel1Feed();
    
    iqFeed4j.level1().selectUpdateFieldNames(
            SummaryUpdateField.MOST_RECENT_TRADE,
            SummaryUpdateField.MOST_RECENT_TRADE_SIZE,
            SummaryUpdateField.MOST_RECENT_TRADE_MARKET_CENTER,
            SummaryUpdateField.MOST_RECENT_TRADE_DATE,
            SummaryUpdateField.MOST_RECENT_TRADE_TIME);
    
    iqFeed4j.level1().requestWatchTrades("AAPL",
            (fundamentalData) -> System.out.printf("Apple's most recent %.2f split was on %s.\n",
                    fundamentalData.getSplitFactor1(), fundamentalData.getSplitFactor1Date()),
            (summaryUpdate) -> System.out.printf("AAPL trade occurred with %d shares at $%.4f with " +
                            "the market center code %d at %s %s with a latency of %s milliseconds.\n",
                    summaryUpdate.getMostRecentTradeSize(),
                    summaryUpdate.getMostRecentTrade(),
                    summaryUpdate.getMostRecentTradeMarketCenter(),
                    summaryUpdate.getMostRecentTradeDate(),
                    summaryUpdate.getMostRecentTradeTime(),
                    ChronoUnit.MILLIS.between(
                            summaryUpdate.getMostRecentTradeTime(),
                            LocalTime.now(ZoneId.of("America/New_York")))));
} catch (IOException exception) {
    exception.printStackTrace();
}
```

### [`MarketDepthFeed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/streaming/marketdepth/MarketDepthFeed.java)
Not implemented yet. See [TODO](#todo).

### [`DerivativeFeed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/streaming/derivative/DerivativeFeed.java)
This feed allows you to stream real-time interval bar data derived from Level 1 data.

The following example will listen to AAPL 1-minute interval bars during Eastern market hours and print them out:
```java
try {
    iqFeed4j.startDerivativeFeed();

    iqFeed4j.derivative().requestIntervalWatch(
            "AAPL",
            60,
            LocalDateTime.now(),
            null,
            null,
            LocalTime.of(9, 30),
            LocalTime.of(12 + 4, 0),
            IntervalType.SECONDS,
            null,
            (interval) -> System.out.printf("Received AAPL interval: %s\n", interval));
} catch (IOException exception) {
    exception.printStackTrace();
}
```

### [`AdminFeed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/streaming/admin/AdminFeed.java)
This feed allows you to manage the IQFeed connection in various ways and retrieve connection information.

The following example demonstrates a few things you can do with the `AdminFeed`.
```java
try {
    iqFeed4j.startAdminFeed();
    
    // Set the login ID and password (and wait for confirmation response)
    String currentLoginID = iqFeed4j.admin().setLoginID("<login ID>").get();
    String currentPassword = iqFeed4j.admin().setPassword("<password>").get();
    System.out.println(currentLoginID);
    System.out.println(currentPassword);

    // Get the next occurrence of the 'FeedStatistics' message
    System.out.printf("Feed stats: %s\n", iqFeed4j.admin().getNextFeedStatistics().get());

    // Print all 'ClientStatistics' after 5 seconds
    iqFeed4j.admin().setClientStatsOn();
    Thread.sleep(5000);
    iqFeed4j.admin().getClientStatisticsOfClientIDs().entrySet().forEach(System.out::println);
} catch (IOException | ExecutionException | InterruptedException exception) {
    exception.printStackTrace();
}
```

## Lookup Feeds

### [`HistoricalFeed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/lookup/historical/HistoricalFeed.java)
This feed allows you to request historical ticks (trades) and intervals. 

The following example will request ascending AAPL 1-minute interval bars during market hours from 6/10/2021 to 6/11/2021.
```java
try {
    iqFeed4j.startHistoricalFeed();
    
    // Using synchronous data consumption method (aka consume data as it is being read)
    iqFeed4j.historical().requestIntervals(
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
    List<Interval> aaplIntervals = iqFeed4j.historical().requestIntervals(
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
    aaplIntervals.forEach(System.out::println);
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

### [`MarketSummaryFeed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/lookup/marketsummary/MarketSummaryFeed.java)
This feed allows you to retrieve a current summary/snapshot of various market data, both historically and currently.

The following example will print out all the `FiveMinuteSnapshot`s of NASDAQ equities.
```java
try {
    iqFeed4j.startMarketSummaryFeed();
    
    List<FiveMinuteSnapshot> fiveMinuteSnapshots = iqFeed4j.marketSummary().request5MinuteSummary(
            "1", // Equity security type
            "5"); // NASDAQ group ID
    fiveMinuteSnapshots.forEach(System.out::println);
} catch (IOException | ExecutionException | InterruptedException exception) {
    exception.printStackTrace();
}
```

### [`NewsFeed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/lookup/news/NewsFeed.java)
This feed allows you to retrieve historical news data on various tickers.

The following example gets all the `NewsConfiguration`s and acquires all the current `NewsMinorSource`s from it and then fetches `NewsHeadlines` for AAPL and TSLA on 6/10/2021 and prints them out.
```java
try {
    iqFeed4j.startNewsFeed();
    
    NewsConfiguration newsConfiguration = iqFeed4j.news().requestNewsConfiguration();
    List<String> allMinorNewsSources = newsConfiguration.getCategories().stream()
            .flatMap(newsCategory -> newsCategory.getMajorSources().stream())
            .flatMap(newsMajorSource -> newsMajorSource.getMinorSources().stream())
            .map(NewsMinorSource::getID)
            .collect(Collectors.toList());

    NewsHeadlines newsHeadlines = iqFeed4j.news().requestNewsHeadlines(
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

### [`OptionChainsFeed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/lookup/optionchains/OptionChainsFeed.java)
This feed allows you to retrieve Option chain data including: equity Option contacts, Future contracts, Future Option contracts, Future Spreads.

The following example gets 5 In-The-Money and 5 Out-Of-The-Money Equity Put and Call Option contracts for AAPL expiring in June and prints them out.
```java
try {
    iqFeed4j.startOptionChainsFeed();
    
    List<OptionContract> applOptions = iqFeed4j.optionChains().getEquityOptionChainWithITMOTMFilter(
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

### [`SymbolMarketInfoFeed`](src/main/java/net/jacobpeterson/iqfeed4j/feed/lookup/symbolmarketinfo/SymbolMarketInfoFeed.java)
This feed allows you to retrieve current symbol information and current market information.

The following example gets all the current `ListedMarket`s and prints them out. 
```java
try {
    iqFeed4j.startSymbolMarketInfoFeed();

    List<ListedMarket> listedMarkets = iqFeed4j.symbolMarketInfo().requestListedMarkets();
    listedMarkets.forEach(System.out::println);
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
- Use [TA4j](https://github.com/ta4j/ta4j) `Num` interface instead of `Double` for number variables so that users can use either `Double` or `BigDecimal` for performance or precision in price data. 
- Add [TimeSeriesDataStore](https://github.com/Petersoj/TimeSeriesDataStore)
