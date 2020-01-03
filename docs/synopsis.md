# Synopsis

## Installation Requirements

SBT/Scala setup for Mac (apologies to everyone else). Firstly you will need [homebrew](https://brew.sh/):

```bash
$ /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```

- Install **JDK** (Java)
- Install **sbt** (Scala)

```bash
$ brew install homebrew/cask/java
$ brew cask info java
```

```bash
$ brew install sbt
$ brew info sbt
```

## Scala

**Test**

```bash
$ sbt test
```

**Run**

There is one driver/main object to run this application:

```bash
$ sbt run
```

**Run debug**

```bash
$ sbt '; set javaOptions += "-Dlog.level=debug"; run'
```

Some other debug options have been added to watch the driver app in action - without these the log output would be overwhelming, and so these options basically slow things down (whether it is a good idea to mix in debug code with actual code is certainly debatable, but for demo purposes it was added).

```bash
$ sbt '; set javaOptions ++= Seq("-Dlog.level=debug", "-Dplayers.per.tick=2", "-Dgames.expire.ratio.per.tick=3"); run'
```

## Design Choices



Design decision - The below (original code) which would issue Waiting events upon a GameCompleted,
now occurs in Controller's "receive".
The spec mentions a Player can be either in a state of "Playing" or "Waiting" which I guess is related to the following commented code.
The approach taken simply seems a tad more intuitive, as there is a "waiting" queue and a "game" queue with an intermediary "triage",
which seemed to leave the idea of "states" redundant.