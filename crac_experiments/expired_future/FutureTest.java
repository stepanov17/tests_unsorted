// may obtain an expired result

import java.util.concurrent.*;

public class FutureTest {

    private static int dummy() {

        int s = 0, n = 30_000;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                s = (s + i + j) % 2;
                if ((i == n - 1) && (j == n - 5)) {
                    try { jdk.crac.Core.checkpointRestore(); } // the calculations are not finished
                    catch (Exception e) { throw new RuntimeException(e); }
                }
            }
        }
        return s;
    }

    public static void main(String args[]) throws Exception {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> result = executor.submit(() -> dummy());
        try { System.out.println(result.get(7, TimeUnit.SECONDS)); }
        catch (TimeoutException e) { System.out.println("timeout"); }
        executor.shutdown();
    }
}
