import uk.ac.leedsbeckett.oop.LBUGraphics;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class TurtleGraphics extends LBUGraphics {

    private boolean penIsDown = true;
    private Color currentColor = Color.BLUE;
    private int penWidth = 1;
    private boolean hasUnsavedChanges = false;
    private boolean isReplaying = false;

    private final Stack<String> commandStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();

    public TurtleGraphics() {
        super();
        setBackground_Col(Color.WHITE);
        clear();
        setPenColour(currentColor);
        setStroke(penWidth);
    }

    @Override
    public void about() {
        super.about();
        JOptionPane.showMessageDialog(this, "Turtle Graphics - Created by Ashma");
    }

    // extra feature  method to allow semicolon-separated commands
    public void processCommand(String input) {
        if (input == null || input.trim().isEmpty()) return;

        String[] commands = input.split(";");
        for (String cmd : commands) {
            processSingleCommand(cmd.trim());
        }
    }


    public void processSingleCommand(String input) {
        if (input == null || input.trim().isEmpty()) return;

        String[] parts = input.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        String[] params = Arrays.copyOfRange(parts, 1, parts.length);

        try {
            switch (command) {
                case "about":
                    about();
                    break;

                case "penup":
                    penIsDown = false;
                    drawOff();
                    saveCommand("penup");
                    break;

                case "pendown":
                    penIsDown = true;
                    drawOn();
                    saveCommand("pendown");
                    break;

                case "move":

                    if (params.length == 0) {
                        JOptionPane.showMessageDialog(this, "Missing parameter for 'move'.");
                        return;
                    }
                    int moveDist = Integer.parseInt(params[0]);
                    if (!isValidDistance(moveDist)) return;
                    forward(moveDist);
                    saveCommand("move " + moveDist);
                    break;

                case "reverse":

                    if (params.length == 0) {
                        JOptionPane.showMessageDialog(this, "Missing parameter for 'reverse'.");
                        return;
                    }
                    int revDist = Integer.parseInt(params[0]);
                    if (!isValidDistance(revDist)) return;
                    forward(-revDist);
                    saveCommand("reverse " + revDist);
                    break;

                case "left":

                    int leftAngle = (params.length >= 1) ? Integer.parseInt(params[0]) : 90;
                    left(leftAngle);
                    saveCommand("left " + leftAngle);
                    break;

                case "right":

                    int rightAngle = (params.length >= 1) ? Integer.parseInt(params[0]) : 90;
                    right(rightAngle);
                    saveCommand("right " + rightAngle);
                    break;


                case "red":
                case "green":
                case "pink":
                case "black":
                    currentColor = switch (command) {
                        case "red" -> Color.RED;
                        case "green" -> Color.GREEN;
                        case "black" -> Color.BLACK;
                        default -> Color.PINK;
                    };
                    setPenColour(currentColor);
                    saveCommand(command);
                    break;

                case "pen":
                    int r = Integer.parseInt(params[0]);
                    int g = Integer.parseInt(params[1]);
                    int b = Integer.parseInt(params[2]);
                    currentColor = new Color(r, g, b);
                    setPenColour(currentColor);
                    saveCommand("pen " + r + " " + g + " " + b);
                    break;

                case "penwidth":
                    int width = Integer.parseInt(params[0]);
                    if (width <= 0 || width > 10) {
                        JOptionPane.showMessageDialog(this, "Pen width must be between 1 and 10.");
                        return;
                    }
                    penWidth = width;
                    setStroke(penWidth);
                    saveCommand("penwidth " + penWidth);
                    break;

                case "clear":
                    if (hasUnsavedChanges && !confirmSave()) return;
                    clear();
                    commandStack.clear();
                    redoStack.clear();
                    break;

                case "reset":
                    reset();
                    break;

                case "square":
                    int len = Integer.parseInt(params[0]);
                    if (!isValidDistance(len)) return;
                    recordPenState();
                    for (int i = 0; i < 4; i++) {
                        forward(len);
                        saveCommand("move " + len);
                        right(90);
                        saveCommand("right 90");
                    }
                    break;

                case "triangle":
                    recordPenState();
                    if (params.length == 1) {
                        int size = Integer.parseInt(params[0]);
                        if (!isValidDistance(size)) return;
                        for (int i = 0; i < 3; i++) {
                            forward(size);
                            saveCommand("move " + size);
                            right(120);
                            saveCommand("right 120");
                        }
                    } else if (params.length == 3) {
                        int a = Integer.parseInt(params[0]);
                        int b_ = Integer.parseInt(params[1]);
                        int c = Integer.parseInt(params[2]);
                        if (!isValidDistance(a) || !isValidDistance(b_) || !isValidDistance(c)) return;
                        forward(a); saveCommand("move " + a); right(120); saveCommand("right 120");
                        forward(b_); saveCommand("move " + b_); right(120); saveCommand("right 120");
                        forward(c); saveCommand("move " + c);
                    } else {
                        throw new IllegalArgumentException("Invalid triangle parameters.");
                    }
                    break;

                case "circle":
                    int radius = Integer.parseInt(params[0]);
                    if (!isValidDistance(radius)) return;
                    recordPenState();
                    circle(radius);
                    saveCommand("circle " + radius);
                    break;

                case "polygon":
                    int sides = Integer.parseInt(params[0]);
                    int sideLength = Integer.parseInt(params[1]);
                    if (sides < 3 || sides > 12) {
                        JOptionPane.showMessageDialog(this, "Polygon sides must be between 3 and 12.");
                        return;
                    }
                    if (!isValidDistance(sideLength)) return;
                    double angle = 360.0 / sides;
                    recordPenState();
                    for (int i = 0; i < sides; i++) {
                        forward(sideLength);
                        saveCommand("move " + sideLength);
                        right((int) angle);
                        saveCommand("right " + (int) angle);
                    }
                    break;

                case "undo":
                    if (!commandStack.isEmpty()) {
                        redoStack.push(commandStack.pop());
                        clear(); reset();
                        java.util.List<String> temp = new java.util.ArrayList<>(commandStack);
                        commandStack.clear();
                        isReplaying = true;
                        for (String cmd : temp) {
                            processCommand(cmd);
                        }
                        isReplaying = false;
                    }
                    break;

                case "redo":
                    if (!redoStack.isEmpty()) {
                        String cmd = redoStack.pop();
                        isReplaying = true;
                        processCommand(cmd);
                        isReplaying = false;
                    }
                    break;

                case "savecmd":
                    JFileChooser saveChooser = new JFileChooser();
                    saveChooser.setDialogTitle("Save Command File");
                    if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File file = saveChooser.getSelectedFile();
                        try (PrintWriter out = new PrintWriter(file)) {
                            for (String cmd : commandStack) {
                                out.println(cmd);
                            }
                            JOptionPane.showMessageDialog(this, "Commands saved to " + file.getName());
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
                        }
                    }
                    break;

                case "loadcmd":
                    JFileChooser loadChooser = new JFileChooser();
                    loadChooser.setDialogTitle("Load Command File");
                    if (loadChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File file = loadChooser.getSelectedFile();
                        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                            String cmd;
                            while ((cmd = in.readLine()) != null) {
                                processCommand(cmd);
                            }
                            JOptionPane.showMessageDialog(this, "Commands loaded from " + file.getName());
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage());
                        }
                    }
                    break;

                case "save_ig":
                    JFileChooser imgSaver = new JFileChooser();
                    imgSaver.setDialogTitle("Save  Image");
                    if (imgSaver.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        String filePath = imgSaver.getSelectedFile().getAbsolutePath();
                        if (!filePath.toLowerCase().endsWith(".png")) {
                            filePath += ".png";
                        }
                        saveCanvasAsImage(filePath);
                    }
                    break;

                case "load_ig":
                    JFileChooser imgLoader = new JFileChooser();
                    imgLoader.setDialogTitle("Load  Image");
                    if (imgLoader.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        loadImageOntoCanvas(imgLoader.getSelectedFile().getAbsolutePath());
                    }
                    break;

                case "help":
                    showHelp();
                    break;

                default:
                    JOptionPane.showMessageDialog(this, "Invalid command: " + command);
            }

            hasUnsavedChanges = true;

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Non-numeric parameter and missing parameter in command.");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private boolean isValidDistance(int dist) {
        if (dist <= 0 || dist > 200) {
            JOptionPane.showMessageDialog(this, "Distance must be between 1 and 200.");
            return false;
        }
        return true;
    }

    private void saveCommand(String cmd) {
        commandStack.push(cmd);
        if (!isReplaying) {
            redoStack.clear();
        }
    }

    private void recordPenState() {
        saveCommand("pen " + currentColor.getRed() + " " + currentColor.getGreen() + " " + currentColor.getBlue());
        saveCommand("penwidth " + penWidth);
        saveCommand(penIsDown ? "pendown" : "penup");
    }

    private boolean confirmSave() {
        int response = JOptionPane.showConfirmDialog(this, "You have unsaved changes. Save?", "Confirm", JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.NO_OPTION || response == JOptionPane.YES_OPTION;
    }

    private void saveCanvasAsImage(String fileName) {
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        paintAll(g2);
        try {
            ImageIO.write(img, "png", new File(fileName));
            JOptionPane.showMessageDialog(this, " saved as " + fileName);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving image: " + e.getMessage());
        }
    }

    private void loadImageOntoCanvas(String fileName) {
        try {
            BufferedImage img = ImageIO.read(new File(fileName));
            Graphics g = getGraphics();
            g.drawImage(img, 0, 0, this);
            JOptionPane.showMessageDialog(this, "Image loaded from " + fileName);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading image: " + e.getMessage());
        }
    }

    private void showHelp() {
        String helpMessage = """
                - move <distance>           : move forward (1–1000)
                - reverse <distance>        : move backward (1–1000)
                - left <degrees>            : turn left
                - right <degrees>           : turn right
                - penup / pendown           : lift or lower the pen
                - red / blue / green / black: set pen color
                - pen <r> <g> <b>           : RGB pen color
                - penwidth <w>              : set pen stroke width (1–50)
                - square <length>           : draw square (length 1–1000)
                - triangle <size> or <a b c>: draw triangle sides (1–1000)
                - circle <radius>           : draw circle (radius 1–1000)
                - polygon <sides> <length>  : polygon (3–12 sides, 1–1000 length)
                - undo / redo               : undo or redo last action
                - savecmd / loadcmd         : save/load commands (choose file)
                - save_ig / load_ig         : save/load canvas (choose image)
                - clear / reset / about / help
                - separate multiple commands using ;
                """;
        JOptionPane.showMessageDialog(this, helpMessage, "Help", JOptionPane.INFORMATION_MESSAGE);
    }
}