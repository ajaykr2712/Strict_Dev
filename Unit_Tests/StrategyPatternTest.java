import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Strategy Pattern
 * Tests different algorithm strategies and runtime selection
 */
public class StrategyPatternTest {

    @Test
    public void testBubbleSortStrategy() {
        // Arrange
        SortContext context = new SortContext(new BubbleSortStrategy());
        int[] array = {5, 2, 8, 1, 9};

        // Act
        int[] result = context.executeStrategy(array);

        // Assert
        assertArrayEquals("Array should be sorted", new int[]{1, 2, 5, 8, 9}, result);
    }

    @Test
    public void testQuickSortStrategy() {
        // Arrange
        SortContext context = new SortContext(new QuickSortStrategy());
        int[] array = {5, 2, 8, 1, 9};

        // Act
        int[] result = context.executeStrategy(array);

        // Assert
        assertArrayEquals("Array should be sorted", new int[]{1, 2, 5, 8, 9}, result);
    }

    @Test
    public void testMergeSortStrategy() {
        // Arrange
        SortContext context = new SortContext(new MergeSortStrategy());
        int[] array = {5, 2, 8, 1, 9};

        // Act
        int[] result = context.executeStrategy(array);

        // Assert
        assertArrayEquals("Array should be sorted", new int[]{1, 2, 5, 8, 9}, result);
    }

    @Test
    public void testChangeStrategy_AtRuntime() {
        // Arrange
        SortContext context = new SortContext(new BubbleSortStrategy());
        int[] array = {3, 1, 2};

        // Act - First sort with bubble sort
        context.executeStrategy(array);

        // Change strategy
        context.setStrategy(new QuickSortStrategy());
        int[] result = context.executeStrategy(new int[]{7, 3, 5});

        // Assert
        assertArrayEquals("Should sort with new strategy", new int[]{3, 5, 7}, result);
    }

    @Test
    public void testEmptyArray() {
        // Arrange
        SortContext context = new SortContext(new BubbleSortStrategy());
        int[] array = {};

        // Act
        int[] result = context.executeStrategy(array);

        // Assert
        assertEquals("Empty array should remain empty", 0, result.length);
    }

    @Test
    public void testSingleElement() {
        // Arrange
        SortContext context = new SortContext(new QuickSortStrategy());
        int[] array = {5};

        // Act
        int[] result = context.executeStrategy(array);

        // Assert
        assertArrayEquals("Single element array should remain unchanged", 
                          new int[]{5}, result);
    }

    // Strategy pattern implementation
    interface SortStrategy {
        int[] sort(int[] array);
    }

    static class BubbleSortStrategy implements SortStrategy {
        @Override
        public int[] sort(int[] array) {
            int[] result = array.clone();
            for (int i = 0; i < result.length - 1; i++) {
                for (int j = 0; j < result.length - i - 1; j++) {
                    if (result[j] > result[j + 1]) {
                        int temp = result[j];
                        result[j] = result[j + 1];
                        result[j + 1] = temp;
                    }
                }
            }
            return result;
        }
    }

    static class QuickSortStrategy implements SortStrategy {
        @Override
        public int[] sort(int[] array) {
            int[] result = array.clone();
            quickSort(result, 0, result.length - 1);
            return result;
        }

        private void quickSort(int[] arr, int low, int high) {
            if (low < high) {
                int pi = partition(arr, low, high);
                quickSort(arr, low, pi - 1);
                quickSort(arr, pi + 1, high);
            }
        }

        private int partition(int[] arr, int low, int high) {
            int pivot = arr[high];
            int i = low - 1;
            for (int j = low; j < high; j++) {
                if (arr[j] < pivot) {
                    i++;
                    int temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
            }
            int temp = arr[i + 1];
            arr[i + 1] = arr[high];
            arr[high] = temp;
            return i + 1;
        }
    }

    static class MergeSortStrategy implements SortStrategy {
        @Override
        public int[] sort(int[] array) {
            int[] result = array.clone();
            if (result.length <= 1) return result;
            mergeSort(result, 0, result.length - 1);
            return result;
        }

        private void mergeSort(int[] arr, int left, int right) {
            if (left < right) {
                int mid = (left + right) / 2;
                mergeSort(arr, left, mid);
                mergeSort(arr, mid + 1, right);
                merge(arr, left, mid, right);
            }
        }

        private void merge(int[] arr, int left, int mid, int right) {
            int n1 = mid - left + 1;
            int n2 = right - mid;
            int[] L = new int[n1];
            int[] R = new int[n2];
            
            System.arraycopy(arr, left, L, 0, n1);
            System.arraycopy(arr, mid + 1, R, 0, n2);
            
            int i = 0, j = 0, k = left;
            while (i < n1 && j < n2) {
                if (L[i] <= R[j]) {
                    arr[k++] = L[i++];
                } else {
                    arr[k++] = R[j++];
                }
            }
            while (i < n1) arr[k++] = L[i++];
            while (j < n2) arr[k++] = R[j++];
        }
    }

    static class SortContext {
        private SortStrategy strategy;

        public SortContext(SortStrategy strategy) {
            this.strategy = strategy;
        }

        public void setStrategy(SortStrategy strategy) {
            this.strategy = strategy;
        }

        public int[] executeStrategy(int[] array) {
            return strategy.sort(array);
        }
    }
}
