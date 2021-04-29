package org.plugin.refactoringassistant.handlers;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;

public class JavaASTParser {

	private ASTParser parser;
	private static CompilationUnit compilationUnit;
	private static Document document;
	private static ASTRewrite rewriter;

	public JavaASTParser(ICompilationUnit icu) throws JavaModelException {
		parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		compilationUnit = (CompilationUnit) parser.createAST(null);
		document = new Document(icu.getSource());
		rewriter = ASTRewrite.create(compilationUnit.getAST());
	}

	public ASTRewrite getRewriter() {
		return rewriter;
	}

	public AST getAST() {
		return compilationUnit.getAST();
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public Document getDocument() {
		return document;
	}

}
