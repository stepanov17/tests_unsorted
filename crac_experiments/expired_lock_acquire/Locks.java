
// concurrently acquire expired lock

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Locks {

    public static void main(String args[]) throws Exception {

        final int s[] = new int[]{0};

        Lock l = new ReentrantLock(true);

        Runnable r1 = () -> {

            try {
                l.lock();
                Thread.sleep(2000);
            } catch (InterruptedException ie) {}
            finally { l.unlock(); }
        };

        Runnable r2 = () -> {

            boolean acq = false;
            try {
                acq = l.tryLock(1, TimeUnit.SECONDS);
                if (acq) {
                    ++s[0];
                    Thread.sleep(2000);
                }
            } catch (InterruptedException ie) {}
            //finally { if (acq) { l.unlock(); } }
        };

        Runnable r3 = () -> {

            boolean acq = false;
            try {
                acq = l.tryLock(1, TimeUnit.SECONDS);
                if (acq) {
                    ++s[0];
                    Thread.sleep(2000);
                }
            } catch (InterruptedException ie) {}
            //finally { if (acq) { l.unlock(); } }
        };

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

        Thread t1 = new Thread(r1);
        t1.start();
        Thread.sleep(300);

        for (int i = 0; i < 20; ++i) { executor.submit(r2); }

        Thread.sleep(500);

        jdk.crac.Core.checkpointRestore();

        Thread.sleep(150);
        System.out.println(s[0]);
        System.exit(0);

        //t1.join();
        //t2.join();
        //t3.join();

        //System.out.println();
    }
}
