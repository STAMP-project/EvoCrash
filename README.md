# EvoCrash: a crash replication tool for Java

What is EvoCrash?
---------------
In order to aid software developers in debugging, we developed EvoCrash, a search-based approach to automated crash reproduction. EvoCrash receives a Java crash stack trace, and searches for a unit test case that can reproduce the crash.


How does EvoCrash work?
---------------
EvoCrash defines the crash replication problem to a search problem. It uses a guided genetic algorithm to find a test which replicates the stack trace which is passed as input.

Output of EvoCrash
---------------
EvoCrash produces 2 outputs:
* A CSV file which reports the process of search.
* The test which replicates the input stack trace.

Getting Started with Demo
---------------

See [https://github.com/STAMP-project/EvoCrash-demo](https://github.com/STAMP-project/EvoCrash-demo).