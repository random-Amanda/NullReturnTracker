package org.plugin.refactoringassistant.handlers;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class JavaASTRewriter {

	private CompilationUnit compilationunit;
	private ASTRewrite rewriter;
	private Document document;

	public JavaASTRewriter(CompilationUnit compilationunit, ASTRewrite rewriter, Document doc) {
		this.compilationunit = compilationunit;
		this.rewriter = rewriter;
		this.document = doc;

	}

	public String rewriteAndGetNewSource(ASTVisitor visitor) throws MalformedTreeException, BadLocationException {
		compilationunit.accept(visitor);
		TextEdit edits = rewriter.rewriteAST(document, null);
		edits.apply(document);
		return document.get();
	}

}
