
package org.plugin.refactoringassistant.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.handlers.HandlerUtil;

public class FixReturnNullHandler extends AbstractHandler {
	static Logger logger = Logger.getLogger(FixReturnNullHandler.class.getName());

	// keeping track of all the changed methods
	List<String> changedMethods = new ArrayList<String>();

	@Override
	public Object execute(ExecutionEvent event) {

		// get the selected file that is open in editor
		ICompilationUnit iCompilationUnit = null;
		ISelection iSelection = HandlerUtil.getCurrentSelection(event);

		if (iSelection instanceof TreeSelection) {
			IStructuredSelection selection = (IStructuredSelection) iSelection;

			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof ICompilationUnit) {
				iCompilationUnit = (ICompilationUnit) firstElement;
			}
		} else if (iSelection instanceof TextSelection) {
			ITypeRoot typeRoot = JavaUI.getEditorInputTypeRoot(HandlerUtil.getActiveEditor(event).getEditorInput());
			iCompilationUnit = (ICompilationUnit) typeRoot.getAdapter(ICompilationUnit.class);
		}

		try {

			// change the return types of methods that return null.
			changeMethodDeclaration(iCompilationUnit);

			// change the return null statement to return Optional.empty()
			// and the other return statements of that same method to return Optional.of()
			changeReturnStatements(iCompilationUnit);

		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * This method changes the return null statements to Optional.empty() and
	 * non-null return statements to Optional.of()
	 */
	private void changeReturnStatements(final ICompilationUnit iCompilationUnit)
			throws JavaModelException, MalformedTreeException, BadLocationException {

		// parse Icompilationunit and generate AST
		JavaASTParser parser = new JavaASTParser(iCompilationUnit);
		ASTRewrite astRewriter = parser.getRewriter();
		AST ast = parser.getAST();
		JavaASTRewriter rewriter = new JavaASTRewriter(parser.getCompilationUnit(), parser.getRewriter(),
				parser.getDocument());

		// create new AST visitor to traverse the AST
		String newSource = rewriter.rewriteAndGetNewSource(new ASTVisitor() {

			// visiting all the return statements in the AST
			@Override
			public boolean visit(ReturnStatement node) {

				// getting the method containing the return statement
				ASTNode parentNode = ASTNodes.getParent(node, ASTNode.METHOD_DECLARATION);

				// change the return statement if the method deceleration has also been changed
				if (!changedMethods.isEmpty() && changedMethods.contains(parentNode.toString())) {
					MethodInvocation returnOptional = ast.newMethodInvocation();
					ReturnStatement newReturn = ast.newReturnStatement();

					// checking if the method return statement is return null
					if (((ReturnStatement) node).getExpression() instanceof NullLiteral) {

						// change from return null to return Optional.empty()
						returnOptional.setName(ast.newSimpleName("empty"));
						returnOptional.setExpression(ASTNodeFactory.newName(ast, "Optional"));
						newReturn.setExpression(returnOptional);

						logger.log(Level.INFO, "Changed return statement from "
								+ ((ReturnStatement) node).getExpression() + " to " + returnOptional);

					} else {

						// change from return <expression> to return Optional.of(<expression>)
						returnOptional.setName(ast.newSimpleName("of"));
						Expression argument = (Expression) ASTNode.copySubtree(ast,
								((ReturnStatement) node).getExpression());
						returnOptional.arguments().add(argument);
						returnOptional.setExpression(ASTNodeFactory.newName(ast, "Optional"));
						newReturn.setExpression(returnOptional);

						logger.log(Level.INFO, "Changed return statement from "
								+ ((ReturnStatement) node).getExpression() + " to " + returnOptional);

					}
					// rewrite the AST with return statement changes
					astRewriter.replace(node, newReturn, null);
				}
				return true;
			}
		});

		// updating the compilation unit with the changes
		iCompilationUnit.getBuffer().setContents(newSource);
	}

	/*
	 * This method changes method return types if the method return null
	 */
	private void changeMethodDeclaration(final ICompilationUnit icu)
			throws JavaModelException, MalformedTreeException, BadLocationException {

		JavaASTParser parser = new JavaASTParser(icu);
		ASTRewrite rew = parser.getRewriter();
		AST ast = parser.getAST();
		JavaASTRewriter rewriter = new JavaASTRewriter(parser.getCompilationUnit(), parser.getRewriter(),
				parser.getDocument());

		String newSource = rewriter.rewriteAndGetNewSource(new ASTVisitor() {

			// visit all the methods
			@Override
			public boolean visit(MethodDeclaration node) {
				String methodBody = node.toString();

				// check if method returns null
				Matcher m = Pattern.compile("return\\s.*null;").matcher(methodBody);
				if (m.find()) {

					// change method declaration if method returns null
					MethodDeclaration optMethodDec = (MethodDeclaration) ASTNode.copySubtree(ast, node);

					// change method deceleration from Type to Optional<Type>
					optMethodDec.setReturnType2(ASTNodeFactory.newType(ast, "Optional<" + node.getReturnType2() + ">"));
					rew.replace(node, optMethodDec, null);

					logger.log(Level.INFO,
							"Changed method return type form " + node.getReturnType2() + " to " + optMethodDec);

					// adding method to the changes methods list
					changedMethods.add(optMethodDec.toString());
				}

				return true;
			}
		});

		// updating the compilation unit with the changes
		icu.getBuffer().setContents(newSource);
	}

}