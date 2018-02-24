package com.davidmalakh.autojavatemplate;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
/**
 *
 * @author David
 */
public class Main {
    public static void main(String[] args) {
        Main main = new Main();
        
        String path = "src\\main\\java\\com\\davidmalakh\\autojavatemplate\\TestCase.java";
        
        main.addTemplatesToFile(path);
    }
    
    public void addTemplatesToFile(String path) {
        ArrayList<String> fileLines = this.readFileIntoList(path);

        fileLines = this.removePreviousComments(fileLines);

        List<JavaClass> classes = this.getClassesFromFile(path);

        for (JavaClass c : classes){
            ArrayList<String> template = new ArrayList<>();
            
            template.add("\t/*-");
            template.addAll(this.addFieldsToTemplate(c));
            template.addAll(this.addMethodsToTemplate(c));
            template.addAll(this.addMethodsFromFieldsToTemplate(classes, c));
            
            if (!c.getSuperJavaClass().getName().equals(c.getName()) && !c.getSuperJavaClass().getName().equals("Object")) {
                template.add("\t*");
                template.add("\t* PLUS EVERYTHING FROM SUPER CLASS:  " + c.getSuperJavaClass().getSimpleName());
            }
            template.add("\t-*/");

            fileLines = this.addTemplate(fileLines, template, c);
        }

        this.writeToFileFromList(fileLines, path);
    }
    
    public ArrayList<String> removePreviousComments(ArrayList<String> fullFileLines) {
        ArrayList<String> fileLines = fullFileLines;
        
        boolean inComment = false;
        for (int i = fileLines.size() - 1; i >= 0; i--) {
            String line = fileLines.get(i);
            if (line.endsWith("-*/")) inComment = true;

            if (inComment) fileLines.remove(i);

            if (line.endsWith("/*-")) inComment = false;
        }
        return fileLines;
    }
    
    public ArrayList<String> addTemplate(ArrayList<String> startFileLines, ArrayList<String> template, JavaClass c) {
        ArrayList<String> fileLines = startFileLines;
        
        for (int i = 0; i < fileLines.size(); i++) {
            String line = fileLines.get(i);
            if (line.contains("class " + c.getName()) && line.endsWith("{")) {
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
            String paramList = this.getParamList(m);
            methods.add("\t* this." + m.getName() + "(" + paramList + ") --" + m.getReturns().getName());
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
            JavaClass className = classes.stream().filter((cl) ->cl.getName().equals(f.getType().getName())).findFirst().orElse(null);
            if (className != null) {
                f.getType().getMethods().forEach((me) -> {
                    String paramList = this.getParamList(me);
                    methodsFromFields.add("\t* this." + f.getName() + "." + me.getName() + "(" + paramList + ") --" + me.getReturns().getName());
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
        try (BufferedReader reader = new BufferedReader(new FileReader(path))){
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
}
