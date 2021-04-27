package org.plugin.tracknullreturn.handlers;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.TextEdit;
//import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jface.text.source.Annotation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JavaParser {
    private static File sourceFile = null;
    private static String source = null;
    private static CompilationUnit compilationUnit = null;
    public static void setSource(File file) throws IOException {
    	source = FileUtils.readFileToString(file, String.valueOf(StandardCharsets.UTF_8));
        sourceFile = file;
    }
    public static void setCompilationUnit(CompilationUnit cu){
        compilationUnit = cu;
    }
    
    public static CompilationUnit getCompilationUnit(){
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(source.toCharArray());
        setCompilationUnit ((CompilationUnit) parser.createAST(null));
        return compilationUnit;
    }
    
    
    public static void addMarker()
    {
    	// Must match the "id" attribute from plugin.xml
    	String NULL_RETURN_MARKER_ID = "com.example.my.plugin.mymarker";
    	// Must not be -1 or any of the values in org.eclipse.jdt.core.compiler.IProblem
    	int MY_JDT_PROBLEM_ID = 1234;

    	// ....
    	//IMarker marker = resource.createMarker(NULL_RETURN_MARKER_ID);
    	//marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
    	//marker.setAttribute(IMarker.MESSAGE, "xxxxxxxxxxxxxxx");
    	//marker.setAttribute(IMarker.CHAR_START, start);
    	//marker.setAttribute(IMarker.CHAR_END, end);
    	//marker.setAttribute(IJavaModelMarker.ID, MY_JDT_PROBLEM_ID);
    }
    
    
    public static void addAnnotation(MethodDeclaration method, String text)
            throws BadLocationException, IOException, JavaModelException, IllegalArgumentException {
        AST ast = compilationUnit.getAST();
        
        /*
        ASTRewrite rewriter = ASTRewrite.create(ast);  
        MarkerAnnotation annotation= ast.newMarkerAnnotation();
        annotation.setTypeName(ast.newSimpleName("Override")); //$NON-NLS-1$
		method.modifiers().add(annotation);  
        TextEdit annotate = rewriter.rewriteAST();
        //.getListRewrite(method.getModifiers(), Block.STATEMENTS_PROPERTY);
        source = FileUtils.readFileToString(sourceFile, String.valueOf(StandardCharsets.UTF_8));
        org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(source);        
        annotate.apply(document);
        */
 	
		
        
        ASTRewrite rewriter = ASTRewrite.create(ast);       
        ListRewrite listRewrite = rewriter.getListRewrite(method.getBody(), Block.STATEMENTS_PROPERTY);
        Statement placeHolder = (Statement) rewriter.
                createStringPlaceholder(text, ASTNode.EMPTY_STATEMENT);
        listRewrite.insertFirst(placeHolder, null);
        source = FileUtils.readFileToString(sourceFile, String.valueOf(StandardCharsets.UTF_8));
        org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(source);
        TextEdit edits = rewriter.rewriteAST(document, null);
        edits.apply(document);
        
        FileUtils.write(sourceFile, document.get(), StandardCharsets.UTF_8);
    }
    
    
}

