import java.util.concurrent.*;

public class ForkJoinExample {
    static class SumTask extends RecursiveTask<Long> {
        private final long[] arr; private final int start, end; private static final int THRESHOLD = 10_000;
        SumTask(long[] arr, int start, int end){ this.arr = arr; this.start = start; this.end = end; }
        protected Long compute(){
            int len = end - start;
            if (len <= THRESHOLD){
                long sum = 0;
                for (int i = start; i < end; i++) sum += arr[i];
                return sum;
            }
            int mid = start + len/2;
            SumTask left = new SumTask(arr, start, mid);
            SumTask right = new SumTask(arr, mid, end);
            left.fork();
            long r = right.compute();
            long l = left.join();
            return l + r;
        }
    }
    public static void main(String[] args){
        long[] data = new long[5_000_00]; // 500k
        for (int i = 0; i < data.length; i++) data[i] = i % 100;
        ForkJoinPool pool = ForkJoinPool.commonPool();
        long sum = pool.invoke(new SumTask(data, 0, data.length));
        System.out.println("Sum = " + sum);
    }
}
