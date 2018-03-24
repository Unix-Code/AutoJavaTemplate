package com.davidmalakh.autojavatemplate;

import java.util.ArrayList;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 *
 * @author David
 */
public class Main extends JPanel implements ActionListener, ComponentListener {

    private static final long serialVersionUID = 1L;

    JButton openButton;
    JCheckBox mop;
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
        fc.setMultiSelectionEnabled(true);
        fc.setPreferredSize(new Dimension(640, 480));
        fc.setMinimumSize(new Dimension(640, 480));
        fc.setMaximumSize(new Dimension(960, 720));
        
        // Open Files Button
        openButton = new JButton("Open a File...");
        openButton.setFont(new Font("Consolas", Font.PLAIN, Math.max(14, this.getWidth()/70)));
        openButton.setFocusable(false);
        
        openButton.addActionListener(this);
        
        // Methods On Parameters
        mop = new JCheckBox("Methods On Parameters", true);
        mop.setBounds(100,100, 50,50);
        
        // Add Elements to GUI
        JPanel f = new JPanel();
        f.add(openButton);        
        
        f.add(mop);

        this.add(f, BorderLayout.PAGE_START);
        this.add(logScrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(Main.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File[] files = fc.getSelectedFiles();
                for (File file : files) {
                    log.append("Opening: " + file.getName() + ".\n");
                    log.append(file.getAbsolutePath() + "\n");
                }
                
                try {
                    List<String> paths = Arrays.asList(files).stream()
                            .map((f) -> f.getAbsolutePath())
                            .collect(Collectors.toList());
                    this.generateTemplates(paths);
                    log.append("Template Made");
                } catch (Exception er) {
                    log.append("Error: " + er);
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

    public void generateTemplates(List<String> paths) {
        TemplateSerializer ts = new TemplateSerializer();
        FileLinesDeserializer fd = new FileLinesDeserializer();
        ClassTemplate ct = new ClassTemplate();
        MethodTemplate mt = new MethodTemplate();
        
        String path = fd.getTempMultiFile(paths).getAbsolutePath();
        
        ArrayList<String> fileLines = fd.removePreviousComments(path);
        ts.writeToFileFromList(fileLines, path);

        Map<Integer, ArrayList<String>> allTemplatesInfo = new HashMap<>();
        allTemplatesInfo.putAll(ct.addClassTemplates(path));
        if (this.mop.isSelected()) {
          allTemplatesInfo.putAll(mt.addAllMethodTemplates(path));
        }
        allTemplatesInfo = ts.adjustAllTemplatesInfo(allTemplatesInfo);
        
        ts.writeToFilesFromList(ts.addAllTemplatesToFileLines(fileLines, allTemplatesInfo), paths);
    }
}
