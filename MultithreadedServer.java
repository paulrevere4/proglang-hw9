package hw09;

import java.awt.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO: Task is currently an ordinary class.
// You will need to modify it to make it a task,
// so it can be given to an Executor thread pool.
//
class Task implements Runnable {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts;
    private String transaction;

    private Map<String, Integer> cache;
    private Map<String, Integer> cacheOG;
    private Set<String> writes;
    private Set<String> reads;
    private Set<String> aquiredLocks;

    // TODO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans;
        cache = new TreeMap<String, Integer>();
        cacheOG = new TreeMap<String, Integer>();
        writes = new TreeSet<String>();
        reads = new TreeSet<String>();
        aquiredLocks = new HashSet<String>();
    }

    private int getAccountVal(int accountNum) {
        String name = String.valueOf( (char)(accountNum+'A') );
        if ( !cache.containsKey(name)) {
            int val = accounts[accountNum].peek();
            reads.add(name);
            cache.put(name, val);
            cacheOG.put(name, val);
            return val;
        } else {
            return cache.get(name);
        }
    }

    // TODO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    private int parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z) {
            throw new InvalidTransactionError();
        }
//        Account a = accounts[accountNum];
        int accountVal = getAccountVal(accountNum);
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*') {
                throw new InvalidTransactionError();
            }
            accountNum = (getAccountVal(accountNum) % numLetters);
            accountVal = getAccountVal(accountNum);
        }
        return accountVal;
    }

    private int parseAccountOrNum(String name) {
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            rtn = parseAccount(name);
        }
        return rtn;
    }

    private String getLhsName(String name) {
        if (name.length() != 1) {
            throw new InvalidTransactionError();
        }
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z) {
            throw new InvalidTransactionError();
        }
        return String.valueOf(name.charAt(0));
    }

    public void carryOutCommands() {
        String[] commands = transaction.split(";");

        for (int i = 0; i < commands.length; i++) {
            String[] words = commands[i].trim().split("\\s");
//            System.out.println(Arrays.toString(words));
            if (words.length < 3) {
                throw new InvalidTransactionError();
            }

//            Account lhs = parseAccount(words[0]);
            String lhs = getLhsName(words[0]);

            if (!words[1].equals("=")) {
                throw new InvalidTransactionError();
            }
            int rhs = parseAccountOrNum(words[2]);
            for (int j = 3; j < words.length; j+=2) {
                if (words[j].equals("+")) {
                    rhs += parseAccountOrNum(words[j+1]);
                }
                else if (words[j].equals("-")) {
                    rhs -= parseAccountOrNum(words[j+1]);
                }
                else {
                    throw new InvalidTransactionError();
                }
            }
            cache.put(lhs, rhs);
            writes.add(lhs);
//            System.out.println(cache.toString());
//            try {
//                lhs.open(true);
//            } catch (TransactionAbortException e) {
//                // won't happen in sequential version
//            }
//            lhs.update(rhs);
//            lhs.close();
        }
    }

    private int getAccountNum(String name) {
        return (int) (name.charAt(0)) - (int) 'A';
    }

    private void aquireLocks() throws TransactionAbortException {
        for (String name : reads) {
//            if (!aquiredLocks.contains(name)) {
                int accountNum = getAccountNum(name);
                accounts[accountNum].open(false);
                aquiredLocks.add(name);
//            }
        }
        for (String name : writes) {
//            if (!aquiredLocks.contains(name)) {
                int accountNum = getAccountNum(name);
                accounts[accountNum].open(true);
                aquiredLocks.add(name);
//            }
        }
    }

    private void clear() {
        cache.clear();
        cacheOG.clear();
        writes.clear();
        reads.clear();
        aquiredLocks.clear();
    }

    private void verifyReads() throws TransactionAbortException {
        for (String name : cacheOG.keySet()) {
            int accountNum = getAccountNum(name);
            accounts[accountNum].verify(cacheOG.get(name));
        }
    }

    private void releaseLocks() {
        for (String name : aquiredLocks) {
            int accountNum = getAccountNum(name);
            accounts[accountNum].close();
        }
    }

    private void update() {
        for (String name : writes) {
            int accountNum = getAccountNum(name);
            int newVal = cache.get(name);
            accounts[accountNum].update(newVal);
//            accounts[accountNum].close();
        }

    }

    public void run() {
        // tokenize transaction
        while (true) {
            carryOutCommands();
            try {
                aquireLocks();
                verifyReads();

            } catch (TransactionAbortException ex) {
                releaseLocks();
                clear();
//                try {
//                    Thread.sleep(1000);  // ms
//                } catch(InterruptedException e) {
//                    e.printStackTrace();
//                }
                continue;
            }
            update();
            releaseLocks();
            break;
        }
        System.out.println("commit: " + transaction);
    }
}

public class MultithreadedServer {

    // requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
    // modifies: accounts
    // effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException {

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));

        // TODO: you will need to create an Executor and then modify the
        // following loop to feed tasks to the executor instead of running them
        // directly.

//        Executor ex = Executors.newFixedThreadPool(3);
        ExecutorService exec = Executors.newCachedThreadPool();

        while ((line = input.readLine()) != null) {
            Task t = new Task(accounts, line);
            exec.execute(t);
//            t.run();
        }
        
        exec.shutdown();
       try {
           exec.awaitTermination(60, TimeUnit.SECONDS);
       } catch (InterruptedException e) {
           e.printStackTrace();
       }

        input.close();

    }
}
