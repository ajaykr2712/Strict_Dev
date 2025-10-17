import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Command Pattern
 * Tests command encapsulation, execution, and undo operations
 */
public class CommandPatternTest {

    private Light light;
    private RemoteControl remote;

    @Before
    public void setUp() {
        light = new Light();
        remote = new RemoteControl();
    }

    @Test
    public void testLightOnCommand() {
        // Arrange
        Command lightOn = new LightOnCommand(light);
        remote.setCommand(lightOn);

        // Act
        remote.pressButton();

        // Assert
        assertTrue("Light should be ON", light.isOn());
    }

    @Test
    public void testLightOffCommand() {
        // Arrange
        light.turnOn();
        Command lightOff = new LightOffCommand(light);
        remote.setCommand(lightOff);

        // Act
        remote.pressButton();

        // Assert
        assertFalse("Light should be OFF", light.isOn());
    }

    @Test
    public void testUndoCommand() {
        // Arrange
        Command lightOn = new LightOnCommand(light);
        remote.setCommand(lightOn);
        
        // Act
        remote.pressButton(); // Turn on
        remote.pressUndo();   // Undo (turn off)

        // Assert
        assertFalse("Light should be OFF after undo", light.isOn());
    }

    @Test
    public void testMultipleCommands() {
        // Arrange
        Command lightOn = new LightOnCommand(light);
        Command lightOff = new LightOffCommand(light);

        // Act
        remote.setCommand(lightOn);
        remote.pressButton();
        
        remote.setCommand(lightOff);
        remote.pressButton();

        // Assert
        assertFalse("Light should be OFF", light.isOn());
    }

    @Test
    public void testMacroCommand() {
        // Arrange
        Light light1 = new Light();
        Light light2 = new Light();
        List<Command> commands = new ArrayList<>();
        commands.add(new LightOnCommand(light1));
        commands.add(new LightOnCommand(light2));
        
        Command macro = new MacroCommand(commands);
        remote.setCommand(macro);

        // Act
        remote.pressButton();

        // Assert
        assertTrue("Light 1 should be ON", light1.isOn());
        assertTrue("Light 2 should be ON", light2.isOn());
    }

    @Test
    public void testCommandHistory() {
        // Arrange
        CommandHistory history = new CommandHistory();
        Command lightOn = new LightOnCommand(light);
        Command lightOff = new LightOffCommand(light);

        // Act
        history.execute(lightOn);
        history.execute(lightOff);
        history.execute(lightOn);

        // Assert
        assertEquals("History should have 3 commands", 3, history.size());
    }

    @Test
    public void testUndoMultipleTimes() {
        // Arrange
        Command lightOn = new LightOnCommand(light);
        Command lightOff = new LightOffCommand(light);

        // Act
        remote.setCommand(lightOn);
        remote.pressButton();
        
        remote.setCommand(lightOff);
        remote.pressButton();
        
        remote.pressUndo(); // Undo OFF -> ON
        remote.pressUndo(); // Undo ON -> OFF

        // Assert
        assertFalse("Light should be OFF after two undos", light.isOn());
    }

    // Command pattern implementation
    interface Command {
        void execute();
        void undo();
    }

    static class Light {
        private boolean on = false;

        public void turnOn() {
            on = true;
        }

        public void turnOff() {
            on = false;
        }

        public boolean isOn() {
            return on;
        }
    }

    static class LightOnCommand implements Command {
        private Light light;

        public LightOnCommand(Light light) {
            this.light = light;
        }

        @Override
        public void execute() {
            light.turnOn();
        }

        @Override
        public void undo() {
            light.turnOff();
        }
    }

    static class LightOffCommand implements Command {
        private Light light;

        public LightOffCommand(Light light) {
            this.light = light;
        }

        @Override
        public void execute() {
            light.turnOff();
        }

        @Override
        public void undo() {
            light.turnOn();
        }
    }

    static class MacroCommand implements Command {
        private List<Command> commands;

        public MacroCommand(List<Command> commands) {
            this.commands = commands;
        }

        @Override
        public void execute() {
            for (Command command : commands) {
                command.execute();
            }
        }

        @Override
        public void undo() {
            for (int i = commands.size() - 1; i >= 0; i--) {
                commands.get(i).undo();
            }
        }
    }

    static class RemoteControl {
        private Command command;
        private Command lastCommand;

        public void setCommand(Command command) {
            this.command = command;
        }

        public void pressButton() {
            command.execute();
            lastCommand = command;
        }

        public void pressUndo() {
            if (lastCommand != null) {
                lastCommand.undo();
            }
        }
    }

    static class CommandHistory {
        private List<Command> history = new ArrayList<>();

        public void execute(Command command) {
            command.execute();
            history.add(command);
        }

        public int size() {
            return history.size();
        }
    }
}
