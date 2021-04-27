package org.plugin.tracknullreturn.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.framework.Bundle;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;



public class SampleHandler extends AbstractHandler {
	
	private static boolean canReturnNullDirect(List<String> returnStatements) {
        for (String statement : returnStatements) {
            if (statement.contains(" null;")) {
                return true;
            }
        }
        return false;
    }

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		File testFile;
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = 
                workbench == null ? null : workbench.getActiveWorkbenchWindow();
        IWorkbenchPage activePage = 
                window == null ? null : window.getActivePage();

        IEditorPart editor = 
                activePage == null ? null : activePage.getActiveEditor();
        IEditorInput input = 
                editor == null ? null : editor.getEditorInput();
        IPath path = input instanceof FileEditorInput 
                ? ((FileEditorInput)input).getPath()
                : null;
        if (path != null)
        {
        	testFile = path.toFile();
        }
        else
        {
    		MessageDialog.openInformation(
    				window.getShell(),
    				"Refactoring Assistant",
    				"Failed to find open Java project!");
    		return null;
        }      
        
        //set source
        try {
			JavaParser.setSource(testFile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        // get compilation unit from parser
        CompilationUnit cu = JavaParser.getCompilationUnit();

        //getMethods
        TypeDeclaration p = (TypeDeclaration) cu.types().get(0);
        MethodDeclaration[] methods = p.getMethods();

        // getting all the return statements of the methods
        for (MethodDeclaration method : methods) {
            String methodBody = method.toString();
            List<String> allMatches = new ArrayList<String>();
            Matcher m = Pattern.compile("return\\s.*;")
                    .matcher(methodBody);
            while (m.find()) {
                allMatches.add(m.group());
            }

            // check if methods return null
            if (canReturnNullDirect(allMatches)) {

					try {
						JavaParser.addAnnotation(method, "//@RETURNING_NULL");
					} catch (IllegalArgumentException | BadLocationException | IOException | JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

            }

        }                
        MessageDialog.openInformation(
				window.getShell(),
				"Refactoring Assistant",
				"Annotation Completed!");
		return null;
	}
}
