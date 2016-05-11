This is our submission for HW9 for CSCI 4430 Programming Languages

Our names are as follows
Paul Revere - RCS: reverp
Joseph Hitchcock - RCS: hitchj


================================================================================
MultithreadedServer implementation
================================================================================
The Multithreaded server starts a thread from an
ExecutorService.newCachedThreadPool() for each transaction. These threads read
the values they need and cache them for local access. The cache is implemented
as a TreeMap. At the end of a transaction the thread will attempt to aquire the
locks and then verify the values it read have not changed. If either of these
steps fail the thread will throw out the work it did and try again after a
random delay. The random delay is there to ensure that the thread can't
immediately aquire the locks it just released, giving other threads time to
operate with them. Once a thread aquires locks and verifies successfully it
will then write its changes to the master accounts list and display to the
console that it was successful.


================================================================================
Test Cases
================================================================================
We provide a series of test cases to simulate different interactions between the
threads. We include the "increment" and the "rotate" tests that we were provided
as they act as good examples. We then added smaller tests that ensure the code
produces the correct results in more complicated transactions. These tests also
excercise the mechanisms for acquiring and releasing locks when there are
conflicts in writing, the tests that do this are the testReadWrite* tests and
the testLarge test. They also check that the server code will cache the values
of the accounts rather than interact with the shared list for each task, these
tests are the testCache* tests.
