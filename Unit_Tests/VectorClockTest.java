import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for VectorClock
 * Tests distributed system clock operations including tick, merge, and comparison
 */
public class VectorClockTest {

    private VectorClockExample.VectorClock clock1;
    private VectorClockExample.VectorClock clock2;

    @Before
    public void setUp() {
        clock1 = new VectorClockExample.VectorClock();
        clock2 = new VectorClockExample.VectorClock();
    }

    @Test
    public void testTick_IncrementsNodeVersion() {
        // Act
        clock1.tick("NodeA");
        clock1.tick("NodeA");

        // Assert
        String result = clock1.toString();
        assertTrue("Clock should contain NodeA", result.contains("NodeA"));
    }

    @Test
    public void testTick_MultipleNodes() {
        // Act
        clock1.tick("NodeA");
        clock1.tick("NodeB");
        clock1.tick("NodeA");

        // Assert
        String result = clock1.toString();
        assertTrue("Clock should track multiple nodes", 
                   result.contains("NodeA") && result.contains("NodeB"));
    }

    @Test
    public void testMerge_CombinesTwoClocks() {
        // Arrange
        clock1.tick("NodeA");
        clock1.tick("NodeA");
        
        clock2.tick("NodeB");
        clock2.tick("NodeB");

        // Act
        clock1.merge(clock2);

        // Assert
        String result = clock1.toString();
        assertTrue("Merged clock should contain both nodes", 
                   result.contains("NodeA") && result.contains("NodeB"));
    }

    @Test
    public void testMerge_TakesMaximumVersion() {
        // Arrange
        clock1.tick("NodeA");
        clock1.tick("NodeA");
        clock1.tick("NodeA");
        
        clock2.tick("NodeA");
        
        // Act
        clock2.merge(clock1);

        // Assert - clock2 should now have max version of NodeA
        String result = clock2.toString();
        assertTrue("Should take maximum version", result.contains("NodeA=3"));
    }

    @Test
    public void testCompare_Equal() {
        // Arrange
        clock1.tick("NodeA");
        clock2.tick("NodeA");

        // Act
        VectorClockExample.Relation result = clock1.compare(clock2);

        // Assert
        assertEquals("Clocks with same versions should be EQUAL", 
                     VectorClockExample.Relation.EQUAL, result);
    }

    @Test
    public void testCompare_After() {
        // Arrange
        clock1.tick("NodeA");
        clock1.tick("NodeA");
        
        clock2.tick("NodeA");

        // Act
        VectorClockExample.Relation result = clock1.compare(clock2);

        // Assert
        assertEquals("Clock with higher version should be AFTER", 
                     VectorClockExample.Relation.AFTER, result);
    }

    @Test
    public void testCompare_Before() {
        // Arrange
        clock1.tick("NodeA");
        
        clock2.tick("NodeA");
        clock2.tick("NodeA");

        // Act
        VectorClockExample.Relation result = clock1.compare(clock2);

        // Assert
        assertEquals("Clock with lower version should be BEFORE", 
                     VectorClockExample.Relation.BEFORE, result);
    }

    @Test
    public void testCompare_Concurrent() {
        // Arrange
        clock1.tick("NodeA");
        clock1.tick("NodeA");
        
        clock2.tick("NodeB");
        clock2.tick("NodeB");

        // Act
        VectorClockExample.Relation result = clock1.compare(clock2);

        // Assert
        assertEquals("Divergent clocks should be CONCURRENT", 
                     VectorClockExample.Relation.CONCURRENT, result);
    }

    @Test
    public void testCompare_ComplexConcurrent() {
        // Arrange
        clock1.tick("NodeA");
        clock1.tick("NodeB");
        
        clock2.tick("NodeB");
        clock2.tick("NodeC");

        // Act
        VectorClockExample.Relation result = clock1.compare(clock2);

        // Assert
        assertEquals("Partially overlapping clocks should be CONCURRENT", 
                     VectorClockExample.Relation.CONCURRENT, result);
    }

    @Test
    public void testEmptyClocks_AreEqual() {
        // Act
        VectorClockExample.Relation result = clock1.compare(clock2);

        // Assert
        assertEquals("Empty clocks should be EQUAL", 
                     VectorClockExample.Relation.EQUAL, result);
    }

    @Test
    public void testMerge_EmptyClock() {
        // Arrange
        clock1.tick("NodeA");

        // Act
        clock1.merge(clock2);

        // Assert
        String result = clock1.toString();
        assertTrue("Merging with empty clock should preserve state", 
                   result.contains("NodeA"));
    }

    @Test
    public void testSequentialTicks_SingleNode() {
        // Act
        for (int i = 0; i < 5; i++) {
            clock1.tick("NodeA");
        }

        // Assert
        String result = clock1.toString();
        assertTrue("Should increment to 5", result.contains("NodeA=5"));
    }

    @Test
    public void testMergeIsIdempotent() {
        // Arrange
        clock1.tick("NodeA");
        clock2.tick("NodeB");
        
        // Act
        clock1.merge(clock2);
        String firstMerge = clock1.toString();
        
        clock1.merge(clock2);
        String secondMerge = clock1.toString();

        // Assert
        assertEquals("Multiple merges of same clock should be idempotent", 
                     firstMerge, secondMerge);
    }
}
