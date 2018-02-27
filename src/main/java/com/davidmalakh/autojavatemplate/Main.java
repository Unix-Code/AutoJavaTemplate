package com.davidmalakh.autojavatemplate;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
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

        fileLines = this.addClassTemplates(fileLines, path);

        fileLines = this.addAllMethodTemplates(fileLines, path);

        this.writeToFileFromList(fileLines, path);
    }

    public ArrayList<String> addAllMethodTemplates(ArrayList<String> startFileLines, String path) {
        ArrayList<String> fileLines = startFileLines;
        List<JavaClass> classes = this.getClassesFromFile(path);

        for (JavaClass c : classes) {
            fileLines = this.addClassMethodTemplates(fileLines, c, classes);
        }

        return fileLines;
    }

    public ArrayList<String> addClassMethodTemplates(ArrayList<String> startFileLines, JavaClass c, List<JavaClass> classes) {
        ArrayList<String> fileLines = startFileLines;

        for (JavaMethod m : c.getMethods().stream().filter((m) -> !m.isAbstract()).collect(Collectors.toList())) {
            ArrayList<String> template = new ArrayList<>();

            ArrayList<String> methodsFromParams = this.addMethodsFromParams(m, classes);
            if (!methodsFromParams.isEmpty()) {
                template.add("\t\t/*-");
                template.addAll(methodsFromParams);
                template.add("\t\t-*/");
            }

            fileLines = this.addMethodTemplate(fileLines, template, m);
        }

        return fileLines;
    }

    public ArrayList<String> addMethodsFromParams(JavaMethod m, List<JavaClass> classes) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> methodsFromParams = new ArrayList<>();

        m.getParameters().forEach((p) -> {
            JavaClass pClass = classes.stream().filter((c) -> c.getName().equals(p.getJavaClass().getName())).findFirst().orElse(null);

            if (pClass != null) {
                pClass.getMethods().stream().filter((pMethods) -> !pMethods.isAbstract()).forEach((mfp) -> {
                    methodsFromParams.add("\t\t* this." + p.getName() + "." + mfp.getName() + "(" + this.getParamList(mfp) + ") --" + mfp.getReturns().getName());
                });
            }
        });

        if (!methodsFromParams.isEmpty()) {
            template.add("\t\t* METHODS FROM PARAMS:");
            template.addAll(methodsFromParams);
        }

        return template;
    }

    public ArrayList<String> addClassTemplates(ArrayList<String> startFileLines, String path) {
        ArrayList<String> fileLines = startFileLines;
        List<JavaClass> classes = this.getClassesFromFile(path);

        for (JavaClass c : classes) {
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
                    template.add("\t*");
                    template.add("\t* PLUS EVERYTHING FROM SUPER CLASS:  " + c.getSuperJavaClass().getSimpleName());
                }
                template.add("\t-*/");
            }
            fileLines = this.addClassTemplate(fileLines, template, c);
        }

        return fileLines;
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

    public ArrayList<String> addClassTemplate(ArrayList<String> startFileLines, ArrayList<String> template, JavaClass c) {
        ArrayList<String> fileLines = startFileLines;

        for (int i = 0; i < fileLines.size(); i++) {
            String line = fileLines.get(i);
            if (line.contains("class " + c.getSimpleName()) && line.endsWith("{")) {
                fileLines.addAll(i + 1, template);
            }
        }

        return fileLines;
    }

    public ArrayList<String> addMethodTemplate(ArrayList<String> startFileLines, ArrayList<String> template, JavaMethod m) {
        ArrayList<String> fileLines = startFileLines;

        String insideClass = "";
        for (int i = 0; i < fileLines.size(); i++) {
            String line = fileLines.get(i);
            if ((line.contains("class ") || line.contains("interface ")) && line.endsWith("{")) {
                List<String> tokens = Arrays.asList(line.split("[ ]")).stream().filter((l) -> !l.isEmpty()).collect(Collectors.toList());
                insideClass = tokens.get(tokens.indexOf((line.contains("class") ? "class" : "interface")) + 1);
            }
            if (insideClass.equals(m.getDeclaringClass().getName()) && line.contains(m.getReturns().getName() + " " + m.getName() + "(" + this.getParamList(m) + ")") && line.endsWith("{")) {
                fileLines.addAll(i + 1, template);
            }
        }

        return fileLines;
    }

    public ArrayList<String> addFieldsToTemplate(JavaClass c) {
        ArrayList<String> template = new ArrayList<>();
        ArrayList<String> fields = new ArrayList<>();

        c.getFields().forEach((f) -> {
            fields.add("\t* this." + f.getName() + " --" + f.getType().getName());
        });

        if (!fields.isEmpty()) {
            template.add("\t* FIELDS:");
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
                methods.add("\t* this." + m.getName() + "(" + paramList + ") --" + m.getReturns().getName());
            }
        });

        if (!methods.isEmpty()) {
            template.add("\t* METHODS:");
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
                        methodsFromFields.add("\t* this." + f.getName() + "." + me.getName() + "(" + paramList + ") --" + me.getReturns().getName());
                    }
                });
            }
        });

        if (!methodsFromFields.isEmpty()) {
            template.add("\t* METHODS FROM FIELDS:");
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
                .map((p) -> p.getType().getValue() + " " + p.getName())
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
                //This is where a real application would open the file.
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
