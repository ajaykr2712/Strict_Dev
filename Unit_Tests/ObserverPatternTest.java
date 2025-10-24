package unittests;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests for Observer Pattern Implementation  
 * Tests event notification and observer registration
 */
public class ObserverPatternTest {

    private Subject subject;
    private TestObserver observer1;
    private TestObserver observer2;

    @Before
    public void setUp() {
        subject = new Subject();
        observer1 = new TestObserver("Observer1");
        observer2 = new TestObserver("Observer2");
    }

    @Test
    public void testAttach_Observer_ReceivesNotifications() {
        // Arrange
        subject.attach(observer1);

        // Act
        subject.setState(10);

        // Assert
        assertEquals("Observer should receive notification", 10, observer1.getReceivedState());
    }

    @Test
    public void testAttach_MultipleObservers_AllReceiveNotifications() {
        // Arrange
        subject.attach(observer1);
        subject.attach(observer2);

        // Act
        subject.setState(20);

        // Assert
        assertEquals("Observer1 should receive notification", 20, observer1.getReceivedState());
        assertEquals("Observer2 should receive notification", 20, observer2.getReceivedState());
    }

    @Test
    public void testDetach_Observer_StopsReceivingNotifications() {
        // Arrange
        subject.attach(observer1);
        subject.attach(observer2);
        subject.setState(5);
        
        // Act - Detach observer1
        subject.detach(observer1);
        subject.setState(15);

        // Assert
        assertEquals("Observer1 should not receive new notification", 5, observer1.getReceivedState());
        assertEquals("Observer2 should receive new notification", 15, observer2.getReceivedState());
    }

    @Test
    public void testNotifyObservers_WithNoObservers_NoException() {
        // Act & Assert - Should not throw exception
        try {
            subject.setState(100);
        } catch (Exception e) {
            fail("Should not throw exception with no observers");
        }
    }

    @Test
    public void testStateChange_NotifiesObservers() {
        // Arrange
        subject.attach(observer1);

        // Act
        subject.setState(1);
        subject.setState(2);
        subject.setState(3);

        // Assert
        assertEquals("Observer should have latest state", 3, observer1.getReceivedState());
        assertEquals("Observer should be notified 3 times", 3, observer1.getNotificationCount());
    }

    @Test
    public void testMultipleStateChanges() {
        // Arrange
        subject.attach(observer1);
        subject.attach(observer2);

        // Act
        for (int i = 0; i < 10; i++) {
            subject.setState(i);
        }

        // Assert
        assertEquals("Observer1 should have latest state", 9, observer1.getReceivedState());
        assertEquals("Observer2 should have latest state", 9, observer2.getReceivedState());
        assertEquals("Observer1 should be notified 10 times", 10, observer1.getNotificationCount());
    }

    // Test implementation
    interface Observer {
        void update(int state);
    }

    static class Subject {
        private List<Observer> observers = new ArrayList<>();
        private int state;

        public void attach(Observer observer) {
            observers.add(observer);
        }

        public void detach(Observer observer) {
            observers.remove(observer);
        }

        public void setState(int state) {
            this.state = state;
            notifyObservers();
        }

        public int getState() {
            return state;
        }

        private void notifyObservers() {
            for (Observer observer : observers) {
                observer.update(state);
            }
        }
    }

    static class TestObserver implements Observer {
        private String name;
        private int receivedState;
        private int notificationCount = 0;

        public TestObserver(String name) {
            this.name = name;
        }

        @Override
        public void update(int state) {
            this.receivedState = state;
            this.notificationCount++;
        }

        public int getReceivedState() {
            return receivedState;
        }

        public int getNotificationCount() {
            return notificationCount;
        }
    }
}
