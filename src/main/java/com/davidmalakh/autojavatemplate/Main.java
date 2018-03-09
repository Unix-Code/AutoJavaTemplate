package com.davidmalakh.autojavatemplate;

import java.util.ArrayList;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

/**
 *
 * @author David
 */
public class Main extends JPanel implements ActionListener, ComponentListener {

    private static final long serialVersionUID = 1L;

    JButton openButton;
    JTextArea log;
    JFileChooser fc;

    // GUI
    public Main() {
        super(new BorderLayout());
        
        log = new JTextArea(5, 20);
        log.setDragEnabled(false);
        
        log.setFont(new Font("Consolas", Font.BOLD, this.getWidth()/35));
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

        fc = new JFileChooser();

        fc.setPreferredSize(new Dimension(640, 480));
        fc.setMinimumSize(new Dimension(640, 480));
        fc.setMaximumSize(new Dimension(960, 720));
        
        openButton = new JButton("Open a File...");
        openButton.setFont(new Font("Consolas", Font.PLAIN, Math.max(14, this.getWidth()/70)));
        
        openButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(openButton);

        this.add(buttonPanel, BorderLayout.PAGE_START);
        this.add(logScrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(Main.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                
                log.append("Opening: " + file.getName() + ".\n");
                log.append(file.getAbsolutePath() + "\n");
                try {
                    this.generateTemplates(file.getAbsolutePath());
                    log.append("Template Made");
                } catch (Exception er) {
                    log.append("Error");
                }

            } else {
                log.append("Open command cancelled by user.\n");
            }
            log.setCaretPosition(log.getDocument().getLength());
        }
    }
    
    public void componentResized(ComponentEvent e) {
        int width = e.getComponent().getWidth();
        
        int fontSize = width/35;
        this.log.setFont(new Font("Consolas", Font.BOLD, Math.max(10, fontSize)));
        this.openButton.setPreferredSize(new Dimension(width/2, this.openButton.getHeight()));
        this.openButton.setFont(new Font("Consolas", Font.PLAIN, Math.max(14, fontSize/2)));
        
        JFrame frame = (JFrame) SwingUtilities.windowForComponent(this);
        frame.revalidate();
    }

    public void componentMoved(ComponentEvent e) {}

    public void componentShown(ComponentEvent e) {}

    public void componentHidden(ComponentEvent e) {}

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Auto Template");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setPreferredSize(new Dimension(960, 720));
        frame.setMaximumSize(new Dimension(960, 720));
        frame.setMinimumSize(new Dimension(320, 240));
        
        Main m = new Main();
        
        frame.add(m);

        frame.addComponentListener(m);
        
        frame.pack();

        frame.setLocationRelativeTo(null);
        
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }    

    public void generateTemplates(String path) {
        TemplateSerializer ts = new TemplateSerializer();
        FileLinesDeserializer fd = new FileLinesDeserializer();
        ClassTemplate ct = new ClassTemplate();
        MethodTemplate mt = new MethodTemplate();
        
        ArrayList<String> fileLines = fd.removePreviousComments(path);
        ts.writeToFileFromList(fileLines, path);

        Map<Integer, ArrayList<String>> allTemplatesInfo = new HashMap<>();
        allTemplatesInfo.putAll(ct.addClassTemplates(path));
        allTemplatesInfo.putAll(mt.addAllMethodTemplates(path));
        allTemplatesInfo = ts.adjustAllTemplatesInfo(allTemplatesInfo);
        
        ts.writeToFileFromList(ts.addAllTemplatesToFileLines(fileLines, allTemplatesInfo), path);
    }
}
