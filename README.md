# ID2203 VT24 Course Project CRDTs

Implement a CvRDT-based Key-Value store with an additional operation for sequentially consistent queries.

## Repository info
This repository contains a template and initial scaffolding for the course project in the course ID2203 VT24. Feel free to fork this project for your own implementation and use, note that it is licensed under Apache 2.0.

## Info
The task is to implement a sequentially consistent operation `Atomic { Read | Write }` on CRDTs. Whereas CRDTs are eventually consistent, and do not support stronger forms of consistency out of the box, this project is about implementing a form of meta-level coordination for CRDTs to support stronger forms of consistency. In particular, the task is to enable executing a set of operations in a way that is sequentially consistent on the shared CRDT state.

## Example
Consider a CvRDT based Key-Value map (LWWMap). The LWWMap is by design eventually consistent. Concurrent operations to the LWWMap may be executed such that strange intermediate states are observed. For example: consider three processes, A, B, C, for which A and B write, and C reads: `A writes {x = a; y = a}`, and `B writes { x = b; y = b }`. C concurrently reads the state and observes `C reads {x = a; y= b}`. This level of consistency may be acceptable for some applications, but some applications (e.g. Shopping Cart, Bank Account) would require stronger consistency levels, i.e. `C reads {x = a; y = a}` or `C reads {x = b; y = b}`. 

## Problem definition and suggested algorithms
You are given a set of *Actors* that each consume a stream of operations. The operations modify the local LWWMap CvRDT state of each actor. Regularly, the actors will exchange their local state with each other in order to (eventually) merge the state. The task is to enable the actors to execute additional operations `Atomic { OP }` under sequential consistency. This is done through implementing additional communication between the actors. You are free to choose the algorithm to implement it. We recommend to implement a lock-based algorithm, or to follow the proposed algorithm in OACP. 

Note that the following algorithm has some issues with potential deadlocks, fix these issues to ensure that your protocol will not deadlock, and that your protocol behaves well even when some nodes go offline (i.e. one node that holds a lock may go offline or crash, your system should be able to recover from this). Note that you should amend the below suggested algorithm to make it correct and efficient, or come up with one on your own.

```
-- Simplified Lock-based Sequential Operations --

Process {
  Local crdt: LWWMap
  Local others: Set[ActorRef]

  Upon Receiving OP:
    crdt.execute(OP)

  Upon Receiving Atomic{ OP }:
    GatherLocks(OP)
    Sync()
    crdt.execute(OP)
    ReleaseLocks(OP)

  Function GatherLocks(OP):
    keys = OP.keys()
    ... // Aquire locks for `keys` from `others`

  Function ReleaseLocks(OP):
    keys = OP.keys()
    ... // Release locks for `keys` to `others`

  Function Sync():
    ... // Get the latest state from `others` and merge into local state
}
```

**References:**

- OACP: https://www.sciencedirect.com/science/article/pii/S2352220820300468.
  - Information on the problem, theory.
- LWWMap: https://doc.akka.io/japi/akka/current/akka/cluster/ddata/LWWMap.html.
  - Akka implementation of LWWMap.
- Sequential consistency: https://jepsen.io/consistency/models/sequential 
  - Information on consistency levels.

## Grading Checklist

Preparatory tasks:
- [ ] You have read the “ID2203 –Distributed Systems, Advanced - Course Project – VT24 P3” document, and read the “Grading” information.

Mandatory project tasks:
- [ ] Your project has implemented sequentially consistent operations on top of CvRDTs.
  - You can use/fork this repository as a template to get started.
  - Your solution works correctly even when nodes fail and recover.
- [ ] Your project uses GitHub for the collaboration.
  - The commits are balanced among group members.
  - Your project is private (for the duration of the course VT24, you can make it public later).
- [ ] Your project README contains the following:
  - A summary of what you have implemented.
  - A summary of why your implementation is correct.
  - Instructions on how to run, test, and evaluate the project.
  - A statement of contributions: who has done what.
- [ ] Your solution is documented.
- [ ] Your solution is appropriately tested.
  - You have tested its correctness.
- [ ] Your solution has an appropriate performance evaluation.
  - Your evaluation compares it against other non-CRDT based alternatives.
- [ ] You have put in an appropriate amount of effort.

Mandatory administrative tasks:
- [ ] You have registered your project.
- [ ] You have formed a group and created a shared repository.
- [ ] You have handed in a project report (PDF).
- [ ] You have given the *Oral Presentation* for your project.

Bonus tasks, for higher grades, 3 of the following:
- [ ] Implement your own version of a Key-Value Map CRDT (instead of using the LWWMap).
- [ ] Execute your evaluation on a distributed system using Kubernetes, evaluate it and gather some initial results.
- [ ] Come up with and implement an optimized protocol for transactional read-only queries.
  - It may be sufficient to gather a causally consistent snapshot of the CRDT states, merging these states, and returning the results on the merged snapshot state. This could be done without locking.
- [ ] Come up with and implement an optimized batched protocol for executing sequentially consistent queries.
- [ ] Implement an example application, such as a shopping cart, that uses your CRDT store.
- [ ] Your project provides great performance benefits, and you have demonstrated this through a performance evaluation which compares it against other solutions.
- [ ] Make your implementation tolerant to failures by persisting the state, and ensuring that state updates do not get lost. Show that it works through injecting failures, and checking that the state is recovered and correct.

## Running the project
```
sbt run
```

## Further Notes
Your project should implement a key-value store interface with "on-demand" sequentially consistent operations. It should provide a key-value store interface, . There should be several parallel/distributed actors that each handle operations for this key-value store (based on eventually consistent CRDTs). Additionally to this, there should be a way to execute a set of operations in a sequentially consistent manner. Not all operations need to be sequentially consistent, only grouped operations that are marked as `Atomic` should be executed in a sequentially consistent manner w.r.t. all other operations.
