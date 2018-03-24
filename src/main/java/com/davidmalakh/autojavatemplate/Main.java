package com.davidmalakh.autojavatemplate;


import spark.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.nio.file.*;
import static spark.Spark.*;
import static spark.debug.DebugScreen.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author David
 */

public class Main {

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
      
      return "<a href=\'" + tempFile.getFileName() + "\' download>Download</a>";
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
    allTemplatesInfo.putAll(mt.addAllMethodTemplates(path));
    
    allTemplatesInfo = ts.adjustAllTemplatesInfo(allTemplatesInfo);

    ts.writeToFileFromList(ts.addAllTemplatesToFileLines(fileLines, allTemplatesInfo), path);
  }
}
