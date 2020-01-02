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

```bash
$ sbt run
```

## Design Choices



Design decision - The below (original code) which would issue Waiting events upon a GameCompleted,
now occurs in Controller's "receive".
The spec mentions a Player can be either in a state of "Playing" or "Waiting" which I guess is related to the following commented code.
The approach taken simply seems a tad more intuitive, as there is a "waiting" queue and a "game" queue with an intermediary "triage",
which seemed to leave the idea of "states" redundant.