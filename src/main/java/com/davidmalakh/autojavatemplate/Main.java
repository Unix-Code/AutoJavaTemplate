package com.davidmalakh.autojavatemplate;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

/**
 *
 * @author David
 */
public class Main extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    JButton openButton, saveButton;
    JTextArea log;
    JFileChooser fc;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }    

    public void addTemplatesToFile(String path) {
        ArrayList<String> fileLines = this.readFileIntoList(path);

        fileLines = this.removePreviousComments(fileLines);

        this.writeToFileFromList(fileLines, path);

        Map<Integer, ArrayList<String>> allTemplatesInfo = new HashMap<>();
        allTemplatesInfo.putAll(this.addClassTemplates(path));
        allTemplatesInfo.putAll(this.addAllMethodTemplates(path));
        
        allTemplatesInfo = this.adjustAllTemplatesInfo(allTemplatesInfo);
        
        fileLines = this.addAllTemplatesToFileLines(fileLines, allTemplatesInfo);
        
        this.writeToFileFromList(fileLines, path);
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

    public Map<Integer, ArrayList<String>> addAllMethodTemplates(String path) {
        Map<Integer, ArrayList<String>> allMethodTemplatesInfo = new HashMap<>();
        List<JavaClass> classes = this.getClassesFromFile(path);
        List<JavaClass> classesAndInterfaces = this.getClassesAndInterfacesFromFile(path);
        
        classes.forEach((c) -> allMethodTemplatesInfo.putAll(this.addClassMethodTemplates(c, classesAndInterfaces)));

        return allMethodTemplatesInfo;
    }

    public Map<Integer, ArrayList<String>> addClassMethodTemplates(JavaClass c, List<JavaClass> classes) {
        Map<Integer, ArrayList<String>> classMethodTemplatesInfo = new HashMap<>();
        c.getMethods().stream().filter((m) -> !m.isAbstract()).collect(Collectors.toList()).forEach((m) -> {
            ArrayList<String> template = new ArrayList<>();

            ArrayList<String> fieldsFromParams = this.addFieldsFromParams(m, classes);
            ArrayList<String> methodsFromParams = this.addMethodsFromParams(m, classes);
            if (/*!fieldsFromParams.isEmpty() || */!methodsFromParams.isEmpty()) {
                template.add("\t\t/*-");
//                template.addAll(fieldsFromParams);
                template.addAll(methodsFromParams);
                template.add("\t\t-*/");
            }
            classMethodTemplatesInfo.put(m.getLineNumber(), template);
        });

        return classMethodTemplatesInfo;
    }
    
    public ArrayList<String> addFieldsFromParams(JavaMethod m, List<JavaClass> classes) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> fieldsFromParams = new ArrayList<>();
        
        m.getParameters().forEach((p) -> {
            JavaClass pClass = classes.stream().filter((c) -> c.getName().equals(p.getJavaClass().getName())).findFirst().orElse(null);

            if (pClass != null) {
                pClass.getFields().forEach((ffp) -> {
                    fieldsFromParams.add("\t\t * " + p.getName() + "." + ffp.getName() + " --" + ffp.getType().getGenericValue());
                });
            }
        });
        
        if (!fieldsFromParams.isEmpty()) {
            template.add("\t\t * FIELDS FROM PARAMS:");
            template.addAll(fieldsFromParams);
        }
        
        return template;
    }

    public ArrayList<String> addMethodsFromParams(JavaMethod m, List<JavaClass> classes) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> methodsFromParams = new ArrayList<>();

        m.getParameters().forEach((p) -> {
            JavaClass pClass = classes.stream().filter((c) -> c.getName().equals(p.getJavaClass().getName())).findFirst().orElse(null);

            if (pClass != null) {
                pClass.getMethods().stream().filter((pMethods) -> !pMethods.isAbstract()).forEach((mfp) -> {
                    methodsFromParams.add("\t\t * " + p.getName() + "." + mfp.getName() + "(" + this.getParamList(mfp) + ") --" + mfp.getReturns().getGenericValue());
                });
            }
        });

        if (!methodsFromParams.isEmpty()) {
            template.add("\t\t * METHODS FROM PARAMS:");
            template.addAll(methodsFromParams);
        }

        return template;
    }

    public Map<Integer, ArrayList<String>> addClassTemplates(String path) {
        List<JavaClass> classes = this.getClassesFromFile(path);

        Map<Integer, ArrayList<String>> templatesInfo = new HashMap<>();
        
        classes.forEach((c) -> {
            ArrayList<String> template = new ArrayList<>();

            ArrayList<String> fields = this.addFieldsToTemplate(c);
            ArrayList<String> methods = this.addMethodsToTemplate(c);
            ArrayList<String> methodsFromFields = this.addMethodsFromFieldsToTemplate(classes, c);
            if (!fields.isEmpty() || !methods.isEmpty() || !methodsFromFields.isEmpty()) {
                template.add("\t/*-");
                template.addAll(fields);
                template.addAll(methods);
                template.addAll(methodsFromFields);
                if (!c.getSuperJavaClass().getName().equals(c.getName()) && !c.getSuperJavaClass().getName().equals("Object")) {
                    template.add("\t *");
                    template.add("\t * PLUS EVERYTHING FROM SUPER CLASS:  " + c.getSuperJavaClass().getSimpleName());
                }
                template.add("\t-*/");
            }
            templatesInfo.put(c.getLineNumber(), template);
        });

        return templatesInfo;
    }

    public ArrayList<String> removePreviousComments(ArrayList<String> fullFileLines) {
        ArrayList<String> fileLines = fullFileLines;

        boolean inComment = false;
        for (int i = fileLines.size() - 1; i >= 0; i--) {
            String line = fileLines.get(i);
            if (line.endsWith("-*/")) {
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

    public ArrayList<String> addAllTemplatesToFileLines(ArrayList<String> startFileLines, Map<Integer, ArrayList<String>> templatesInfo) {
        ArrayList<String> fileLines = startFileLines;
        
        List<Integer> sortedKeys = new ArrayList<>(templatesInfo.keySet());
        Collections.sort(sortedKeys);
        sortedKeys.forEach((lineNum) -> fileLines.addAll(lineNum, templatesInfo.get(lineNum)));
        
        return fileLines;
    }

    public ArrayList<String> addFieldsToTemplate(JavaClass c) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> fields = new ArrayList<>();

        c.getFields().forEach((f) -> {
            fields.add("\t * this." + f.getName() + " --" + f.getType().getGenericValue());
        });

        if (!fields.isEmpty()) {
            template.add("\t * FIELDS:");
            template.addAll(fields);
        }

        return template;
    }

    public ArrayList<String> addMethodsToTemplate(JavaClass c) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> methods = new ArrayList<>();

        c.getMethods().forEach((m) -> {
            if (!m.isAbstract()) {
                String paramList = this.getParamList(m);
                methods.add("\t * this." + m.getName() + "(" + paramList + ") --" + m.getReturns().getGenericValue());
            }
        });

        if (!methods.isEmpty()) {
            template.add("\t * METHODS:");
            template.addAll(methods);
        }

        return template;
    }

    public ArrayList<String> addMethodsFromFieldsToTemplate(List<JavaClass> classes, JavaClass c) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> methodsFromFields = new ArrayList<>();

        c.getFields().forEach((f) -> {
            JavaClass className = classes.stream().filter((cl) -> cl.getName().equals(f.getType().getName())).findFirst().orElse(null);
            if (className != null) {
                f.getType().getMethods().forEach((me) -> {
                    if (!me.isAbstract()) {
                        String paramList = this.getParamList(me);
                        methodsFromFields.add("\t * this." + f.getName() + "." + me.getName() + "(" + paramList + ") --" + me.getReturns().getGenericValue());
                    }
                });
            }
        });

        if (!methodsFromFields.isEmpty()) {
            template.add("\t * METHODS FROM FIELDS:");
            template.addAll(methodsFromFields);
        }

        return template;
    }

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

    public List<JavaClass> getClassesFromFile(String path) {
        JavaProjectBuilder builder = new JavaProjectBuilder();
        List<JavaClass> classes = new ArrayList<>();
        JavaSource src;
        try {
            src = builder.addSource(new FileReader(path));
            classes = src.getClasses().stream().filter((c) -> !c.isInterface()).collect(Collectors.toList());
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        return classes;
    }
    
    public List<JavaClass> getClassesAndInterfacesFromFile(String path) {
        JavaProjectBuilder builder = new JavaProjectBuilder();
        List<JavaClass> classes = new ArrayList<>();
        JavaSource src;
        try {
            src = builder.addSource(new FileReader(path));
            classes = src.getClasses();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        return classes;
    }

    public void writeToFileFromList(ArrayList<String> fileLines, String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (String line : fileLines) {
                writer.write(line + "\r\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getParamList(JavaMethod m) {
        return m.getParameters().stream()
                .map((p) -> p.getType().getGenericValue() + " " + p.getName())
                .collect(Collectors.joining(", "));
    }

    // GUI
    public Main() {
        super(new BorderLayout());

        log = new JTextArea(5, 20);
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

        fc = new JFileChooser();

        openButton = new JButton("Open a File...");
        openButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(openButton);

        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(Main.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                log.append("Opening: " + file.getName() + ".\n");
                log.append(file.getAbsolutePath() + "\n");
                try {
                    addTemplatesToFile(file.getAbsolutePath());
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

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Auto Template");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new Main());

        frame.pack();
        frame.setVisible(true);
    }
}
