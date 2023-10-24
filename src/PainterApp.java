import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Stack;

public class PainterApp extends JFrame {

    private JPanel canvas;
    private Color currentColor;
    private int brushSize;
    private BufferedImage image;
    private Graphics2D graphics;
    private Stack<BufferedImage> undoStack;
    private Stack<BufferedImage> redoStack;
    private JButton editButton;

    private Color menuStrokeColor; // Nowe pole przechowujące kolor obramowania menu

    public PainterApp() {
        setTitle("Aplikacja malarska");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        menuStrokeColor = Color.BLACK; // Domyślny kolor obramowania menu

        canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        };

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                undoStack.push(copyImage(image));
                graphics.setColor(currentColor);
                graphics.fillOval(e.getX(), e.getY(), brushSize, brushSize);
                canvas.repaint();
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                graphics.setColor(currentColor);
                graphics.fillOval(e.getX(), e.getY(), brushSize, brushSize);
                canvas.repaint();
            }
        });

        currentColor = Color.BLACK;
        brushSize = 5;

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Plik");
        JMenuItem saveItem = new JMenuItem("Zapisz");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });

        fileMenu.add(saveItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JButton colorButton = new JButton("Wybierz kolor");
        colorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseColor();
            }
        });

        JButton brushSizeButton = new JButton("Wybierz rozmiar pędzla");
        brushSizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseBrushSize();
            }
        });

        editButton = new JButton("Edycja");
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu editMenu = createEditMenu();
                editMenu.show(editButton, 0, editButton.getHeight());
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(colorButton);
        buttonPanel.add(brushSizeButton);
        buttonPanel.add(editButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonPanel, BorderLayout.WEST);
        getContentPane().add(canvas, BorderLayout.CENTER);

        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, getWidth(), getHeight());

        undoStack = new Stack<>();
        redoStack = new Stack<>();

        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "undo");
        canvas.getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "redo");
        canvas.getActionMap().put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        setVisible(true);
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(copyImage(image));
            image = undoStack.pop();
            graphics = image.createGraphics();
            canvas.repaint();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(copyImage(image));
            image = redoStack.pop();
            graphics = image.createGraphics();
            canvas.repaint();
        }
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    private void saveToFile() {
        try {
            File file = new File("painting.png");
            ImageIO.write(image, "png", file);
            System.out.println("Zapisano obraz do pliku: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void chooseColor() {
        Color initialColor = currentColor;

        JColorChooser colorChooser = new JColorChooser(initialColor);
        AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
        for (AbstractColorChooserPanel panel : panels) {
            if (panel.getDisplayName().equals("Swatches")) {
                customizeSwatchesPanel(panel);
            } else if (panel.getDisplayName().equals("HSV")) {
                customizeHSVPanel(panel);
            } else if (panel.getDisplayName().equals("HSL")) {
                customizeHSLPanel(panel);
            } else if (panel.getDisplayName().equals("RGB")) {
                customizeRGBPanel(panel);
            } else if (panel.getDisplayName().equals("CMYK")) {
                customizeCMYKPanel(panel);
            }
        }

        Color selectedColor = JColorChooser.showDialog(this, "Wybierz kolor", initialColor);
        if (selectedColor != null) {
            currentColor = selectedColor;
        }
    }

    private void customizeSwatchesPanel(AbstractColorChooserPanel panel) {
        Container container = (Container) panel.getComponent(0); // Access the container holding the components

        // Customize the components in the Swatches panel
        JLabel label = findLabelComponent(container, "Label.ColorChooser.labels");
        if (label != null) {
            label.setText("Wybrany Kolor");
        }

        JLabel sampleTextTop = findLabelComponent(container, "Label.ColorChooser.sample");
        if (sampleTextTop != null) {
            sampleTextTop.setText("Wybrany Kolor");
        }

        JLabel sampleTextBottom = findLabelComponent(container, "Label.ColorChooser.sample");
        if (sampleTextBottom != null) {
            sampleTextBottom.setText("Wybrany Kolor");
        }
    }

    private JLabel findLabelComponent(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel && name.equals(component.getName())) {
                return (JLabel) component;
            }
            if (component instanceof Container) {
                JLabel label = findLabelComponent((Container) component, name);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    private void customizeHSVPanel(AbstractColorChooserPanel panel) {
        // Customize the HSV panel if desired
    }

    private void customizeHSLPanel(AbstractColorChooserPanel panel) {
        // Customize the HSL panel if desired
    }

    private void customizeRGBPanel(AbstractColorChooserPanel panel) {
        // Customize the RGB panel if desired
    }

    private void customizeCMYKPanel(AbstractColorChooserPanel panel) {
        // Customize the CMYK panel if desired
    }

    private void chooseBrushSize() {
        String input = JOptionPane.showInputDialog(this, "Podaj rozmiar pędzla:", brushSize);
        try {
            int size = Integer.parseInt(input);
            if (size > 0) {
                brushSize = size;
            } else {
                JOptionPane.showMessageDialog(this, "Podano nieprawidłową wartość.", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Podano nieprawidłową wartość.", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPopupMenu createEditMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem undoItem = new JMenuItem("Cofnij");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        undoItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        JMenuItem redoItem = new JMenuItem("Ponów");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        redoItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        JMenuItem menuStrokeColorItem = new JMenuItem("Kolor obramowania menu");
        menuStrokeColorItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseMenuStrokeColor();
            }
        });

        popupMenu.add(undoItem);
        popupMenu.add(redoItem);
        popupMenu.addSeparator();
        popupMenu.add(menuStrokeColorItem);

        return popupMenu;
    }

    private void chooseMenuStrokeColor() {
        Color initialColor = menuStrokeColor;
        Color selectedColor = JColorChooser.showDialog(this, "Wybierz kolor obramowania menu", initialColor);
        if (selectedColor != null) {
            menuStrokeColor = selectedColor;
            editButton.setBorder(BorderFactory.createLineBorder(menuStrokeColor, 2));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PainterApp();
            }
        });
    }
}