public class NullObjectExample {
    interface Logger { void log(String message); }

    static class ConsoleLogger implements Logger {
        public void log(String message){ System.out.println("[LOG] " + message); }
    }
    static class NullLogger implements Logger {
        public void log(String message) { /* do nothing */ }
    }

    static class Service {
        private final Logger logger;
        Service(Logger logger){ this.logger = logger == null ? new NullLogger() : logger; }
        void doWork(){
            logger.log("Starting work");
            // ...do some work...
            logger.log("Finished work");
        }
    }

    public static void main(String[] args){
        new Service(new ConsoleLogger()).doWork();
        new Service(null).doWork();
        System.out.println("Null Object avoids null checks and side effects.");
    }
}
