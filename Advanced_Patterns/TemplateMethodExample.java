// Template Method Design Pattern Example
public class TemplateMethodExample {
    static abstract class DataProcessor {
        // template method
        public final void process() {
            read();
            parse();
            validate();
            write();
        }
        protected abstract void read();
        protected abstract void parse();
        protected void validate() { System.out.println("Default validation"); }
        protected void write() { System.out.println("Writing output"); }
    }

    static class CsvProcessor extends DataProcessor {
        @Override protected void read() { System.out.println("Reading CSV"); }
        @Override protected void parse() { System.out.println("Parsing CSV rows"); }
    }

    static class JsonProcessor extends DataProcessor {
        @Override protected void read() { System.out.println("Reading JSON"); }
        @Override protected void parse() { System.out.println("Parsing JSON nodes"); }
        @Override protected void validate() { System.out.println("JSON schema validation"); }
    }

    public static void main(String[] args) {
        new CsvProcessor().process();
        new JsonProcessor().process();
    }
}
