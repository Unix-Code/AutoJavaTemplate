package com.davidmalakh.autojavatemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David
 */
public class FileLinesDeserializer {
    
    public ArrayList<String> readFileIntoList(String path) {
        ArrayList<String> fileLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                fileLines.add(tmp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileLines;
    }
    
    public ArrayList<String> removePreviousComments(String path) {
        ArrayList<String> fileLines = this.readFileIntoList(path);
        
        ArrayList<Integer> lineNumsToRemove = new ArrayList<>();
        
        boolean inComment = false;
        for (int i = 0; i < fileLines.size(); i++) {
            String line = fileLines.get(i);
            
            if (line.contains("/*-")) {
                inComment = true;
            }
            
            if (inComment) {
                lineNumsToRemove.add(i);
            }
            
            if (line.endsWith("*/")) {
                inComment = false;
            }
        }
        
        Collections.sort(lineNumsToRemove, Collections.reverseOrder());
        
        for (int i : lineNumsToRemove) fileLines.remove(i);
        
        return fileLines;
    }
    
    public File getTempMultiFile(List<String> paths) {
        TemplateSerializer ts = new TemplateSerializer();
        ArrayList<String> multiFileLines = new ArrayList<>();
        File temp = null;
        
        for (String path : paths) {
            multiFileLines.addAll(this.readFileIntoList(path));
            multiFileLines.add("// End of Class");
        }
        
        try {
            temp = File.createTempFile("temp_multi", ".java");
            ts.writeToFileFromList(multiFileLines, temp.getAbsolutePath());
            temp.deleteOnExit();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return temp;
    }
}
