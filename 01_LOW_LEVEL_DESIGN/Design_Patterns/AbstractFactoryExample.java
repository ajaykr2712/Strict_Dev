public class AbstractFactoryExample {
    interface Button { void paint(); }
    interface Checkbox { void paint(); }

    static class WinButton implements Button { public void paint(){ System.out.println("Render Windows Button"); } }
    static class MacButton implements Button { public void paint(){ System.out.println("Render MacOS Button"); } }
    static class WinCheckbox implements Checkbox { public void paint(){ System.out.println("Render Windows Checkbox"); } }
    static class MacCheckbox implements Checkbox { public void paint(){ System.out.println("Render MacOS Checkbox"); } }

    interface GUIFactory { Button createButton(); Checkbox createCheckbox(); }
    static class WinFactory implements GUIFactory {
        public Button createButton(){ return new WinButton(); }
        public Checkbox createCheckbox(){ return new WinCheckbox(); }
    }
    static class MacFactory implements GUIFactory {
        public Button createButton(){ return new MacButton(); }
        public Checkbox createCheckbox(){ return new MacCheckbox(); }
    }

    static class Application {
        private final Button button; private final Checkbox checkbox;
        Application(GUIFactory factory){
            this.button = factory.createButton();
            this.checkbox = factory.createCheckbox();
        }
        void render(){
            button.paint();
            checkbox.paint();
        }
    }

    public static void main(String[] args){
        GUIFactory factory = System.getProperty("os.name", "").toLowerCase().contains("mac") ? new MacFactory() : new WinFactory();
        Application app = new Application(factory);
        app.render();
    }
}
