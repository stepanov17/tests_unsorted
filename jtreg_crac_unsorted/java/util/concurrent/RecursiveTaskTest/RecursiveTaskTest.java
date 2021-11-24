// Copyright 2019-2021 Azul Systems, Inc.  All Rights Reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License version 2 only, as published by
// the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE.  See the GNU General Public License version 2 for more
// details (a copy is included in the LICENSE file that accompanied this code).
//
// You should have received a copy of the GNU General Public License version 2
// along with this work; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Azul Systems, 385 Moffett Park Drive, Suite 115, Sunnyvale,
// CA 94089 USA or visit www.azul.com if you need additional information or
// have any questions.

/*
 * @test RecursiveTaskTest.java
 * @requires (os.family == "linux")
 * @library /test/lib
 * @summary check if the checkpoint/restore does not break RecursiveTask behaviour
 * @run main RecursiveTaskTest
 */

import jdk.crac.Core;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


public class RecursiveTaskTest {

    private final static int N = 45;
    private final static int F = 1134903170; // fibonacci(45)

    private static class Fibonacci extends RecursiveTask<Integer> {

        final int n;

        Fibonacci(int n) { this.n = n; }

        @Override
        protected Integer compute() {

            if (n <= 1) { return n; }

            Fibonacci f1 = new Fibonacci(n - 1);
            f1.fork();
            Fibonacci f2 = new Fibonacci(n - 2);
            return f2.compute() + f1.join();
        }
    }


    private static void runTest(boolean cancel) throws Exception {

        ForkJoinPool pool = new ForkJoinPool();
        Fibonacci fib = new Fibonacci(N);
        ForkJoinTask<Integer> result = pool.submit(fib);

        Thread.sleep(500); // definitely will not finish calculations in 0.5 sec


        // C/R
        jdk.crac.Core.checkpointRestore();

        if (cancel) { // cancel after restore

            result.cancel(true);

            if (!result.isDone()) {
                throw new RuntimeException("isDone() did not return true");
            }

            if (!result.isCancelled()) {
                throw new RuntimeException("isCancelled() did not return true");
            }

            if (!result.isCompletedAbnormally()) {
                throw new RuntimeException("isCompletedAbnormally() did not return true");
            }

            if (result.isCompletedNormally()) {
                throw new RuntimeException("isCompletedNormally() returned true");
            }

            // check if getRawResult() returns null
            if (result.getRawResult() != null) {

                throw new RuntimeException("a raw result "
                        + "should be null for an uncompleted calculation");
            }

            // check if get() throws CancellationException
            boolean thrown = false;
            try {
                result.get();
            } catch (CancellationException ce) {
                thrown = true;
            }
            if (!thrown) {
                throw new RuntimeException("no CancellationException was thrown");
            }

        } else { // check if the restored calculations gave the correct result

            int res = result.join();
            if (res != F) {
                throw new RuntimeException("invalid result: " + res);
            }

            if (!result.isDone()) {
                throw new RuntimeException("isDone() did not return true");
            }

            if (result.isCancelled()) {
                throw new RuntimeException("isCancelled() returned true");
            }

            if (result.isCompletedAbnormally()) {
                throw new RuntimeException("isCompletedAbnormally() returned true");
            }

            if (!result.isCompletedNormally()) {
                throw new RuntimeException(
                        "isCompletedNormally() did not return true");
            }

            int raw = result.getRawResult();
            if (raw != F) {
                throw new RuntimeException("invalid raw result: " + raw);
            }
        }

        pool.shutdown();
    }



    public static void main(String args[]) throws Exception {

        if (args.length > 0) {

            boolean cancel = Boolean.parseBoolean(args[0]);
            runTest(cancel);

        } else {

            String cancel[] = {"false", "true"};

            for (String c: cancel) {

                ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-XX:CRaCCheckpointTo=cr", "RecursiveTaskTest", c);
                OutputAnalyzer out = new OutputAnalyzer(pb.start());
                out.shouldContain("CR: Checkpoint");
                out.shouldHaveExitValue(137);

                pb = ProcessTools.createJavaProcessBuilder(
                    "-XX:CRaCRestoreFrom=cr", "RecursiveTaskTest", c);
                out = new OutputAnalyzer(pb.start());
                out.shouldHaveExitValue(0);
            }
        }
    }
}
