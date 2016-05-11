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
import java.util.Random;
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

    private Account[] accounts; /* shared mutable state */
    private String transaction;

    private Map<String, Integer> cache;
    private Map<String, Integer> cacheOG;
    private Set<String> writes;
    private Set<String> reads;
    private Set<String> aquiredLocks;

    /**
     * Task constructor given access to accounts (shared memory) and a
     * string containing the write(s) in the current transaction.
     * 
     * @param allAccounts   an array of all accounts
     * @param trans         a string containing the transaction
     */
    public Task(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans;
        cache = new TreeMap<String, Integer>();
        cacheOG = new TreeMap<String, Integer>();
        writes = new TreeSet<String>();
        reads = new TreeSet<String>();
        aquiredLocks = new HashSet<String>();
    }

    /**
     * Given an account number, the account name is derived using
     * the alphabetical offset. The first time a new account is seen
     * the amount is cached for later verification, otherwise if it
     * has already been seen it returns the cached value.
     * 
     * @param accountNum    integer representing alphabetical account
     * @return              the value of the specified account
     */
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

    /**
     * Given an account name, access the account, perform the nested
     * asterisk operator as described, and then return the value.
     * 
     * @param name  string representing account
     * @throws      InvalidTransactionError if accountNum not valid index
     * @return      value of the account
     */
    private int parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z) {
            throw new InvalidTransactionError();
        }
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

    /**
     * Given a valid integer, returns that value. Otherwise
     * attempts to access account with the same name.
     * 
     * @param name  string representing an account (or number)
     * @return      value of the account (or number)
     */
    private int parseAccountOrNum(String name) {
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            rtn = parseAccount(name);
        }
        return rtn;
    }

    /**
     * Given account name, return the name of the account being written to.
     * 
     * @param name  string representing account
     * @throws      InvalidTransactionError if multi-letter account provided
     *                                      or invalid letter provided
     * @return      name of account
     */
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

    /**
     * Using cached values, performs serialized operations of
     * a single transaction, which may include multiple
     * writes separated by a semicolon. Tracks all accounts
     * which will need to be read from/written to.
     * 
     * @throws InvalidTransactionError  if input unexpected
     */
    public void carryOutCommands() {
        String[] commands = transaction.split(";");

        for (int i = 0; i < commands.length; i++) {
            String[] words = commands[i].trim().split("\\s");
            if (words.length < 3) {
                throw new InvalidTransactionError();
            }

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
        }
    }

    /**
     * Converts capital letter to integer using alphabetical offset.
     * 
     * @param name  string representing account
     * @return      corresponding number
     */
    private int getAccountNum(String name) {
        return (int) (name.charAt(0)) - (int) 'A';
    }

    /**
     * Using shared memory containing which accounts will need to be
     * read from/written to, opens all of them in alphabetical order 
     * to prevent deadlock, acquiring locks too all accounts.
     * 
     * @throws TransactionAbortException    if opened incorrectly
     */
    private void aquireLocks() throws TransactionAbortException {
        for (String name : reads) {
            int accountNum = getAccountNum(name);
            accounts[accountNum].open(false);
            aquiredLocks.add(name);
        }
        for (String name : writes) {
            int accountNum = getAccountNum(name);
            accounts[accountNum].open(true);
            aquiredLocks.add(name);
        }
    }

    /**
     * Clears all local memory, for use when transaction fails due
     * to changes in accounts, will need to try again.
     */
    private void clear() {
        cache.clear();
        cacheOG.clear();
        writes.clear();
        reads.clear();
        aquiredLocks.clear();
    }

    /**
     * Makes sure that all accounts are the same as what they were
     * when the transaction began.
     * 
     * @throws TransactionAbortException    if verify fails
     */
    private void verifyReads() throws TransactionAbortException {
        for (String name : cacheOG.keySet()) {
            int accountNum = getAccountNum(name);
            accounts[accountNum].verify(cacheOG.get(name));
        }
    }

    /**
     * Releases the lock to all accounts read from/written to.
     */
    private void releaseLocks() {
        for (String name : aquiredLocks) {
            int accountNum = getAccountNum(name);
            accounts[accountNum].close();
        }
    }

    /**
     * Update left hand side accounts with the computed values.
     */
    private void update() {
        for (String name : writes) {
            int accountNum = getAccountNum(name);
            int newVal = cache.get(name);
            accounts[accountNum].update(newVal);
        }

    }

    /**
     * Perform a random wait when threads are competing for
     * access which causes both to restart, without this it
     * is highly likely that the threads would both compete
     * again, causing repeated failures.
     */
    private void randomWait() {
        try {
            Random rand = new Random();
            Thread.sleep( rand.nextInt(1000) );  // ms
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carries out the 4 operations described above after
     * obtaining a cache of all accounts.
     * 
     * (1) open all accounts you need, for reading, writing, or both 
     * (2) verify all previously peeked-at values
     * (3) perform all updates
     * (4) close all opened accounts
     */
    public void run() {
        // while loop tries to complete the transaction until it is successful
        while (true) {
            carryOutCommands();
            try {
                aquireLocks();
                verifyReads();
            } catch (TransactionAbortException ex) {
                releaseLocks();
                clear();
                randomWait();
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

        ExecutorService exec = Executors.newCachedThreadPool();

        while ((line = input.readLine()) != null) {
            Task t = new Task(accounts, line);
            exec.execute(t);
        }

        //waits here until all threads join back
        exec.shutdown();
        try {
            exec.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        input.close();

    }
}
