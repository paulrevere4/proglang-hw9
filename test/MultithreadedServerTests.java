package hw09.test;

import hw09.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.Random;

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
    public void testDebug() throws IOException {
//        System.out.println("===STARTING DEBUG TESTS===");
        // initialize accounts
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(Z-i);
        }

        MultithreadedServer.runServer("hw09/data/debug", accounts);
//        MultithreadedServer.runServer("hw09/data/debug", accounts);
    }
    
    @Test
    public void testRotate() throws IOException {
//        System.out.println("===STARTING ROTATE TESTS===");
        // initialize accounts
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(Z-i);
        }

        MultithreadedServer.runServer("hw09/data/rotate", accounts);
//        MultithreadedServer.runServer("src/hw09/data/rotate", accounts);
    }

     @Test
     public void testIncrement() throws IOException {
//        System.out.println("===STARTING INCREMENT TESTS===");
        // initialize accounts
        accounts = new Account[numLetters];
        for (int i = A; i <= Z; i++) {
            accounts[i] = new Account(Z-i);
        }

        MultithreadedServer.runServer("hw09/data/increment", accounts);
//        MultithreadedServer.runServer("src/hw09/data/increment", accounts);

        // assert correct account values
        for (int i = A; i <= Z; i++) {
            Character c = new Character((char) (i+'A'));
            assertEquals("Account "+c+" differs",Z-i+1,accounts[i].getValue());
        }


     }


}
