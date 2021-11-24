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
 * @test ParallelStreamTest.java
 * @requires (os.family == "linux")
 * @library /test/lib
 * @summary check if the checkpoint/restore does not break a parallel stream behaviour
 * @run main ParallelStreamTest
 */

import jdk.crac.Core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;



public class ParallelStreamTest {

    private static void delay(long msec) {

        try { Thread.sleep(msec); }
            catch (InterruptedException ie) {} // fixme: rethrow?
    }


    private static void runTest() {

        AtomicBoolean doCheckpoint = new AtomicBoolean(true);

        IntConsumer action = (int i) -> {

            delay((i % 2 == 0) ? 500 : 1500);

            if (doCheckpoint.compareAndSet(true, false)) {

                delay(100);
                // C/R
                try { jdk.crac.Core.checkpointRestore(); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
        };

        IntStream range = IntStream.rangeClosed(1, 30);

        // the operations will be performed partly before the checkpoint, partly after the restore
        long sum = range.parallel().peek(action).filter(i -> i % 2 == 0).reduce(0, Integer::sum);

        if (sum != 240) {
            throw new RuntimeException("invalid sum: " + sum);
        }
    }


    public static void main(String args[]) throws Exception {

        if (args.length > 0) {

            runTest();

        } else {

            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:CRaCCheckpointTo=cr", "ParallelStreamTest", "runTest");
            OutputAnalyzer out = new OutputAnalyzer(pb.start());
            out.shouldContain("CR: Checkpoint");
            out.shouldHaveExitValue(137);

            pb = ProcessTools.createJavaProcessBuilder(
                "-XX:CRaCRestoreFrom=cr", "ParallelStreamTest", "runTest");
            out = new OutputAnalyzer(pb.start());
            out.shouldHaveExitValue(0);
        }
    }
}
