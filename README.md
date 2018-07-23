# EvoCrash: a crash replication tool for Java

In order to aid software developers in debugging, we developed EvoCrash, a search-based approach to automated crash reproduction. EvoCrash receives a Java crash stack trace, and searches for a unit test case that can reproduce the crash.

## How does EvoCrash work?

EvoCrash defines the crash replication problem to a search problem. It uses a guided genetic algorithm to find a test which replicates the stack trace which is passed as input.

### Output of EvoCrash

EvoCrash produces 2 outputs:
* A CSV file which reports the process of search.
* The test which replicates the input stack trace.

### Getting Started with Demo

See [EvoCrash-demo](https://github.com/STAMP-project/EvoCrash-demo).

## Building EvoCrash

EvoCrash uses [Maven](https://maven.apache.org/).

To build EvoCrash on the command line, install maven and then call

```mvn compile```

To create a binary distribution that includes all dependencies you can
use Maven as well:

```mvn package```


## Funding

EvoCrash is partially funded by research project STAMP (European Commission - H2020)
![STAMP - European Commission - H2020](https://github.com/STAMP-project/docs-forum/blob/master/docs/images/logo_readme_md.png)
