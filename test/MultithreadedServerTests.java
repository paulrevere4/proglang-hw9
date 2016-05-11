package hw09.test;

import hw09.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;

public class MultithreadedServerTests extends TestCase {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;
    private static Account[] accounts;

    protected static void dumpAccounts() {
        // output values:
        for (int i = A; i <= Z; i++) {
           System.out.print("    ");
           if (i < 10) System.out.print("0");
           System.out.print(i + " ");
           System.out.print(new Character((char) (i + 'A')) + ": ");
           accounts[i].print();
           System.out.print(" (");
           accounts[i].printMod();
           System.out.print(")\n");
        }
     }

    @Test
    public void testCache1() throws IOException {
        // initialize accounts to 0
        System.out.println("===STARTING CHACHE TEST 1===");
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(0);
        }

        MultithreadedServer.runServer("hw09/data/testCache1", accounts);

        assertEquals("Account A differs",1,accounts[0].getValue());
        assertEquals("Account B differs",1,accounts[1].getValue());
        assertEquals("Account C differs",1,accounts[2].getValue());
        assertEquals("Account D differs",2,accounts[3].getValue());
    }
    
    @Test
    public void testCache2() throws IOException {
        // initialize accounts to 0
        System.out.println("===STARTING CHACHE TEST 2===");
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(0);
        }

        MultithreadedServer.runServer("hw09/data/testCache2", accounts);

        assertEquals("Account A differs",1,accounts[0].getValue());
        assertEquals("Account B differs",1,accounts[1].getValue());
        assertEquals("Account C differs",1,accounts[2].getValue());
        assertEquals("Account D differs",0,accounts[3].getValue());
    }
    
    @Test
    public void testReadWrite1() throws IOException {
        // initialize accounts to 0
        System.out.println("===STARTING READ/WRITE TEST 1===");
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(Z-i);
        }

        MultithreadedServer.runServer("hw09/data/testReadWrite1", accounts);

        Set<Integer> possibleZs = new HashSet();
        possibleZs.add(26);
        possibleZs.add(2);
        Set<Integer> possibleYs = new HashSet();
        possibleYs.add(25);
        possibleYs.add(1);

        assertEquals("Account A differs",25,accounts[0].getValue());
        assertEquals("Account B differs",0,accounts[1].getValue());
        assertTrue("Account Z differs, should be 26 or 2 was " + accounts[25].getValue()
            ,possibleZs.contains( accounts[25].getValue()) );
        assertTrue("Account Y differs, should be 25 or 1 was " + accounts[24].getValue()
            ,possibleYs.contains( accounts[24].getValue()) );
    }

    @Test
    public void testRotate() throws IOException {
        System.out.println("===STARTING ROTATE TESTS===");
        // initialize accounts
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(Z-i);
        }

        MultithreadedServer.runServer("hw09/data/rotate", accounts);
    }

    @Test
    public void testIncrement() throws IOException {
        System.out.println("===STARTING INCREMENT TESTS===");
        // initialize accounts
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(Z-i);
        }

        MultithreadedServer.runServer("hw09/data/increment", accounts);

        // assert correct account values
        for (int i = A; i <= Z; i++) {
            Character c = new Character((char) (i+'A'));
            assertEquals("Account "+c+" differs",Z-i+1,accounts[i].getValue());
        }


    }


}
