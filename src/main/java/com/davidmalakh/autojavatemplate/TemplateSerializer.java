package com.davidmalakh.autojavatemplate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author David
 */
public class TemplateSerializer {

    public void writeToFileFromList(ArrayList<String> fileLines, String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (String line : fileLines) {
                writer.write(line + "\r\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<String> addAllTemplatesToFileLines(ArrayList<String> startFileLines, Map<Integer, ArrayList<String>> templatesInfo) {
        ArrayList<String> fileLines = startFileLines;

        List<Integer> sortedKeys = new ArrayList<>(templatesInfo.keySet());
        Collections.sort(sortedKeys);
        sortedKeys.forEach((lineNum) -> fileLines.addAll(lineNum, templatesInfo.get(lineNum)));

        return fileLines;
    }

    public Map<Integer, ArrayList<String>> adjustAllTemplatesInfo(Map<Integer, ArrayList<String>> templatesInfo) {
        Map<Integer, ArrayList<String>> fixedTemplatesInfo = new HashMap<>();
        List<Integer> lineNums = new ArrayList<>();
        List<ArrayList<String>> templates = new ArrayList<>();
        List<Integer> templateSizes = new ArrayList<>();

        for (Map.Entry<Integer, ArrayList<String>> entry : templatesInfo.entrySet()) {
            lineNums.add(entry.getKey());
            templates.add(entry.getValue());
            templateSizes.add(entry.getValue().size());
        }

        lineNums = this.fixLineNumbers(lineNums, templateSizes);

        for (int i = 0; i < lineNums.size(); i++) {
            Integer lineNum = lineNums.get(i);
            ArrayList<String> template = templates.get(i);

            fixedTemplatesInfo.put(lineNum, template);
        }

        return fixedTemplatesInfo;
    }

    public List<Integer> fixLineNumbers(List<Integer> start, List<Integer> inTemplateSizes) {
        List<Integer> templateSizes = inTemplateSizes;
        List<Integer> templateLineNums = start;

        for (int i = 0; i < templateLineNums.size(); i++) {
            Integer key = templateLineNums.get(i);

            List<Integer> newKeys = new ArrayList<>();

            for (int j = 0; j < templateLineNums.size(); j++) {
                Integer innerKey = templateLineNums.get(j);
                if (key < innerKey) {
                    newKeys.add(innerKey + templateSizes.get(i));
                } else {
                    newKeys.add(innerKey);
                }
            }

            templateLineNums = newKeys;
        }

        return templateLineNums;
    }
}
