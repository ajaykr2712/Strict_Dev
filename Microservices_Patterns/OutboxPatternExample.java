import java.util.*;
import java.util.concurrent.*;

public class OutboxPatternExample {
    static class DbRow { final String id; final String data; DbRow(String id, String data){ this.id=id; this.data=data; } }
    static class EventRow { final String id; final String topic; final String payload; volatile boolean published=false; EventRow(String id, String topic, String payload){ this.id=id; this.topic=topic; this.payload=payload; } }

    static class FakeDB {
        final Map<String, DbRow> table = new ConcurrentHashMap<>();
        final Map<String, EventRow> outbox = new ConcurrentHashMap<>();
        synchronized void insertWithOutbox(DbRow row, EventRow event){
            table.put(row.id, row);
            outbox.put(event.id, event);
        }
    }

    static class EventRelay {
        private final FakeDB db;
        EventRelay(FakeDB db){ this.db = db; }
        void pollAndPublish(){
            for (EventRow e : db.outbox.values()){
                if (!e.published){
                    System.out.println("Publishing to " + e.topic + ": " + e.payload);
                    e.published = true;
                }
            }
        }
    }

    public static void main(String[] args){
        FakeDB db = new FakeDB();
        EventRelay relay = new EventRelay(db);
        db.insertWithOutbox(new DbRow("1", "order-1"), new EventRow("evt-1", "orders", "created order-1"));
        relay.pollAndPublish();
        System.out.println("Outbox size: " + db.outbox.size());
    }
}
