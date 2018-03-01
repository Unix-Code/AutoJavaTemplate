package com.davidmalakh.autojavatemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

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
        
        boolean inComment = false;
        for (int i = fileLines.size() - 1; i >= 0; i--) {
            String line = fileLines.get(i);
            if (line.endsWith("*/")) {
                inComment = true;
            }
            
            if (inComment) {
                fileLines.remove(i);
            }
            
            if (line.endsWith("/*-")) {
                inComment = false;
            }
        }
        return fileLines;
    }
}
