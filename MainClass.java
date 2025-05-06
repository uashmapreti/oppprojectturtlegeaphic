import javax.swing.JFrame;
import javax.swing.JTextField;
import java.awt.BorderLayout;

public class MainClass {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Turtle Graphics");
        TurtleGraphics turtlePanel = new TurtleGraphics();
        JTextField commandField = new JTextField();

        commandField.addActionListener(_ -> {
            String command = commandField.getText();
            turtlePanel.processCommand(command);
            commandField.setText("");
        });

        frame.setLayout(new BorderLayout());
        frame.add(turtlePanel, BorderLayout.CENTER);
        frame.add(commandField, BorderLayout.SOUTH);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
