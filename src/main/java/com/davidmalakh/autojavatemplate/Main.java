package com.davidmalakh.autojavatemplate;


import spark.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.nio.file.*;
import static spark.Spark.*;
import static spark.debug.DebugScreen.*;
import java.util.ArrayList;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;

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
        File file = fc.getSelectedFile();

        log.append("Opening: " + file.getName() + ".\n");
        log.append(file.getAbsolutePath() + "\n");
        try {
          this.generateTemplates(file.getAbsolutePath());
          log.append("Template Made");
        } catch (Exception er) {
          log.append(er.getMessage());
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

  //  public static void main(String[] args) {
  //
  //    SwingUtilities.invokeLater(new Runnable() {
  //      public void run() {
  //        UIManager.put("swing.boldMetal", Boolean.FALSE);
  //        createAndShowGUI();
  //      }
  //    });
  //  }

  public static void main(String[] args) {
    port(getHerokuAssignedPort());
    enableDebugScreen();

    File uploadDir = new File("upload");
    
    staticFiles.externalLocation("upload");

    get("/", (req, res) ->
    "<form method='post' enctype='multipart/form-data'>" // note the enctype
    + "    <input type='file' name='uploaded_file' accept='.java'>" // make sure to call getPart using the same "name" in the post
    + "    <button>Upload Java File</button>"
    + "</form>"
        );

    post("/", (req, res) -> {

      uploadDir.mkdir(); // create the upload directory if it doesn't exist
      try {
        FileUtils.cleanDirectory(uploadDir);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      
      Path tempFile = Files.createTempFile(uploadDir.toPath(), "file", ".java");

      req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

      try (InputStream input = req.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use same "name" as input field in form
        Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
      }

      logInfo(req, tempFile);
      Main main = new Main();
      main.generateTemplates(tempFile.toString());
      
      return "<a href='" + tempFile.getFileName().toString() + "'>Download</a>";
    });
  }


  // methods used for logging
  private static void logInfo(Request req, Path tempFile) throws IOException, ServletException {
    System.out.println("Uploaded file '" + getFileName(req.raw().getPart("uploaded_file")) + "' saved as '" + tempFile.toAbsolutePath() + "'");
  }

  private static String getFileName(Part part) {
    for (String cd : part.getHeader("content-disposition").split(";")) {
      if (cd.trim().startsWith("filename")) {
        return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
      }
    }
    return null;
  }
  
  static int getHerokuAssignedPort() {
    ProcessBuilder processBuilder = new ProcessBuilder();
    if (processBuilder.environment().get("PORT") != null) {
        return Integer.parseInt(processBuilder.environment().get("PORT"));
    }
    return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
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
    if (this.mop.isSelected()) {
      allTemplatesInfo.putAll(mt.addAllMethodTemplates(path));
    }
    allTemplatesInfo = ts.adjustAllTemplatesInfo(allTemplatesInfo);

    ts.writeToFileFromList(ts.addAllTemplatesToFileLines(fileLines, allTemplatesInfo), path);
  }
}
