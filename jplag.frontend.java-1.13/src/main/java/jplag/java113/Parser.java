package jplag.java113;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.stmt.*;
import jplag.Token;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

class JavaParserWalker {
	private Parser parser;
	private String filename;

	@Contract
	private void add(@NotNull JavaTokenConstants tok, @NotNull Position position, int length) {
		parser.add(tok.getValue(), filename, position.line, position.column, length);
	}

	@Contract
	private void add(JavaTokenConstants tok, Position position, @NotNull String token) {
		this.add(tok, position, token.length());
	}

	@Contract(pure = true)
	JavaParserWalker(Parser parser, String filename) {
		this.parser = parser;
		this.filename = filename;
	}

	private void afterDispatch(@NotNull Node node) {
		var pos = node.getRange().map(r -> r.end).orElseGet(() -> new Position(-1, -1));

		if (node instanceof CatchClause) {
			add(JavaTokenConstants.J_CATCH_END, pos, 1);
		} else if (node instanceof ModuleDeclaration) {
			add(JavaTokenConstants.J_MODULE_END, pos, 1);
		} else if (node instanceof BodyDeclaration) {
			BodyDeclaration bodyDeclaration = (BodyDeclaration) node;
			bodyDeclaration.ifEnumDeclaration($ -> add(JavaTokenConstants.J_ENUM_END, pos, 1));
			bodyDeclaration.ifClassOrInterfaceDeclaration(new Consumer<ClassOrInterfaceDeclaration>() {
				@Override
				public void accept(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
					if (classOrInterfaceDeclaration.isInterface()) {
						add(JavaTokenConstants.J_INTERFACE_END, pos, 1);
					} else {
						add(JavaTokenConstants.J_CLASS_END, pos, 1);
					}
				}
			});
			bodyDeclaration.ifAnnotationDeclaration($ -> add(JavaTokenConstants.J_ANNO_T_END, pos, 1));
			bodyDeclaration.ifAnnotationMemberDeclaration($ -> add(JavaTokenConstants.J_METHOD_END, pos, 1));
			bodyDeclaration.ifEnumConstantDeclaration($ -> add(JavaTokenConstants.J_ENUM_CONSTANT, pos, 1));
		} else if (node instanceof Expression) {
			Expression expr = (Expression) node;

			expr.ifSwitchExpr($ -> add(JavaTokenConstants.J_SWITCH_END, pos, 1));
		} else if (node instanceof Statement) {
			Statement statement = (Statement)node;

			statement.ifSynchronizedStmt($ -> add(JavaTokenConstants.J_SYNC_END, pos, 1));
			statement.ifSwitchStmt($ -> add(JavaTokenConstants.J_SWITCH_END, pos, 1));
			statement.ifDoStmt($ -> add(JavaTokenConstants.J_DO_END, pos, 1));
			statement.ifForEachStmt($ -> add(JavaTokenConstants.J_FOR_END, pos, 1));
			statement.ifForStmt($ -> add(JavaTokenConstants.J_FOR_END, pos, 1));

			statement.ifWhileStmt($ -> add(JavaTokenConstants.J_WHILE_END, pos, 1));
		}
	}

	private boolean beforeDispatch(@NotNull Node node) {
		boolean traversed = false;
		var pos = node.getRange().map(r -> r.begin).orElseGet(() -> new Position(-1, -1));


		if (node instanceof SwitchEntry) {
			add(JavaTokenConstants.J_CASE, pos, "case");
		} else if (node instanceof CatchClause) {
			add(JavaTokenConstants.J_CATCH_BEGIN, pos, "catch");
		} else if (node instanceof ImportDeclaration) {
			add(JavaTokenConstants.J_IMPORT, pos, "import");
		} else if (node instanceof PackageDeclaration) {
			add(JavaTokenConstants.J_PACKAGE, pos, "package");
		} else if (node instanceof ModuleDirective) {
			ModuleDirective moduleDirective = (ModuleDirective) node;

			moduleDirective.ifModuleExportsDirective($ -> add(JavaTokenConstants.J_EXPORTS, pos, "export"));
			moduleDirective.ifModuleRequiresDirective($ -> add(JavaTokenConstants.J_REQUIRES, pos, "require"));
			moduleDirective.ifModuleProvidesDirective($ -> add(JavaTokenConstants.J_PROVIDES, pos, "provides"));
		} else if (node instanceof ModuleDeclaration) {
			add(JavaTokenConstants.J_MODULE_BEGIN, pos, "module");
		} else if (node instanceof BodyDeclaration) {
			BodyDeclaration bodyDeclaration = (BodyDeclaration) node;
			bodyDeclaration.ifEnumDeclaration($ -> add(JavaTokenConstants.J_ENUM_BEGIN, pos, "enum"));
			bodyDeclaration.ifClassOrInterfaceDeclaration(new Consumer<ClassOrInterfaceDeclaration>() {
				@Override
				public void accept(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
					if (classOrInterfaceDeclaration.isInterface()) {
						add(JavaTokenConstants.J_INTERFACE_BEGIN, pos, "interface");
					} else {
						add(JavaTokenConstants.J_CLASS_BEGIN, pos, "class");
					}
				}
			});
			bodyDeclaration.ifAnnotationDeclaration($ -> add(JavaTokenConstants.J_ANNO_T_BEGIN, pos, "@interface"));
			bodyDeclaration.ifAnnotationMemberDeclaration($ -> add(JavaTokenConstants.J_METHOD_BEGIN, pos, 1));
			if (bodyDeclaration.isMethodDeclaration()) {
				traversed = true;
				MethodDeclaration meth = bodyDeclaration.asMethodDeclaration();
				meth.getAnnotations().forEach(this::dispatch);
				add(JavaTokenConstants.J_METHOD_BEGIN, meth.getType().getRange().map(r -> r.begin).orElse(pos), meth.getNameAsString());
				meth.getBody().ifPresent(this::dispatch);
				add(JavaTokenConstants.J_METHOD_END, meth.getRange().map(r -> r.end).orElse(pos), 1);
			}
			if (bodyDeclaration.isConstructorDeclaration()) {
				traversed = true;
				ConstructorDeclaration meth = bodyDeclaration.asConstructorDeclaration();
				meth.getAnnotations().forEach(this::dispatch);
				add(JavaTokenConstants.J_CONSTR_BEGIN, meth.getName().getRange().map(r -> r.begin).orElse(pos), meth.getNameAsString());
				this.dispatch(meth.getBody());
				add(JavaTokenConstants.J_CONSTR_END, meth.getRange().map(r -> r.end).orElse(pos), 1);
			}
			bodyDeclaration.ifEnumConstantDeclaration($ -> add(JavaTokenConstants.J_ENUM_CONSTANT, pos, 1));
		} else if (node instanceof VariableDeclarator) {
			VariableDeclarator variableDeclarator = (VariableDeclarator) node;
			add(JavaTokenConstants.J_VARDEF, pos, variableDeclarator.getNameAsString());
		} else if (node instanceof Expression) {
			Expression expr = (Expression) node;
			if (expr.isArrayCreationExpr()) {
				traversed = true;
				add(JavaTokenConstants.J_NEWARRAY, pos, "new");
				ArrayCreationExpr arrayCreationExpr = expr.asArrayCreationExpr();
				arrayCreationExpr.getLevels().forEach(this::dispatch);
				dispatch(arrayCreationExpr.getElementType());

				arrayCreationExpr.getInitializer().ifPresent(arrayInitializerExpr -> {
					add(JavaTokenConstants.J_ARRAY_INIT_BEGIN, arrayInitializerExpr.getRange().map(r -> r.begin).orElse(pos), 1);
					dispatch(arrayInitializerExpr);
					add(JavaTokenConstants.J_ARRAY_INIT_END, arrayInitializerExpr.getRange().map(r -> r.end).orElse(pos), 1);
				});
			}

			expr.ifAnnotationExpr($ -> add(JavaTokenConstants.J_ANNO, pos, "@".length() + $.getNameAsString().length()));
			expr.ifAssignExpr($ -> add(JavaTokenConstants.J_ASSIGN, pos, "="));

			if (expr.isObjectCreationExpr()) {
				traversed = true;
				ObjectCreationExpr objectCreationExpr = expr.asObjectCreationExpr();
				add(JavaTokenConstants.J_NEWCLASS, pos, "new");
				objectCreationExpr.getScope().ifPresent(this::dispatch);
				dispatch(objectCreationExpr.getType());
				objectCreationExpr.getTypeArguments().ifPresent(args -> args.forEach(this::dispatch));
				objectCreationExpr.getArguments().forEach(this::dispatch);

				objectCreationExpr.getAnonymousClassBody().ifPresent(body -> {
					if (body.isNonEmpty()) {
						add(JavaTokenConstants.J_INIT_BEGIN, body.get(0).getRange().map(r -> r.begin).orElse(pos), "{");
						body.forEach(this::dispatch);
						add(JavaTokenConstants.J_INIT_END, body.get(body.size() - 1).getRange().map(r -> r.end).orElse(pos), "}");
					}
				});
			}

			expr.ifConditionalExpr($ -> add(JavaTokenConstants.J_COND, pos, "?"));
			expr.ifSwitchExpr($ -> add(JavaTokenConstants.J_SWITCH_BEGIN, pos, "switch"));
			expr.ifMethodCallExpr(methodCallExpr -> add(JavaTokenConstants.J_APPLY, pos, methodCallExpr.getNameAsString()));
			expr.ifLambdaExpr($ -> add(JavaTokenConstants.J_LAMBDA, pos, "->"));
		} else if (node instanceof Statement) {
			Statement statement = (Statement)node;

			statement.ifSynchronizedStmt($ -> add(JavaTokenConstants.J_SYNC_BEGIN, pos, "synchronized"));
			statement.ifSwitchStmt($ -> add(JavaTokenConstants.J_SWITCH_BEGIN, pos, "switch"));
			statement.ifAssertStmt($ -> add(JavaTokenConstants.J_ASSERT, pos, "assert"));
			statement.ifBreakStmt($ -> add(JavaTokenConstants.J_BREAK, pos, "break"));
			statement.ifContinueStmt($ -> add(JavaTokenConstants.J_CONTINUE, pos, "continue"));
			statement.ifDoStmt($ -> add(JavaTokenConstants.J_DO_BEGIN, pos, "do"));
			statement.ifForEachStmt($ -> add(JavaTokenConstants.J_FOR_BEGIN, pos, "for"));
			statement.ifForStmt($ -> add(JavaTokenConstants.J_FOR_BEGIN, pos, "for"));
			if (statement.isIfStmt()) {
				traversed = true;
				IfStmt ifStmt = statement.asIfStmt();
				add(JavaTokenConstants.J_IF_BEGIN, pos, "if");

				dispatch(ifStmt.getCondition());
				dispatch(ifStmt.getThenStmt());

				Optional<Statement> elseStatement = ifStmt.getElseStmt();
				if (elseStatement.isPresent()) {
					Position thenEnd = ifStmt.getThenStmt().getRange().map(r -> r.end).orElse(
							elseStatement.get().getRange().map(r -> r.end).orElse(pos)
					);
					add(JavaTokenConstants.J_ELSE, thenEnd, "else");
					dispatch(elseStatement.get());
				}

				add(JavaTokenConstants.J_IF_END, node.getRange().map(r -> r.end).orElse(pos), 1);
			}

			statement.ifWhileStmt($ -> add(JavaTokenConstants.J_WHILE_BEGIN, pos, "while"));
			statement.ifReturnStmt($ -> add(JavaTokenConstants.J_RETURN, pos, "return"));
			statement.ifYieldStmt($ -> add(JavaTokenConstants.J_YIELD, pos, "yield"));
			statement.ifThrowStmt($ -> add(JavaTokenConstants.J_THROW, pos, "throw"));
			if (statement.isTryStmt()) {
				TryStmt tryStmt = statement.asTryStmt();
				traversed = true;
				if (tryStmt.getResources().isEmpty()) {
					add(JavaTokenConstants.J_TRY_BEGIN, pos, "try");
				} else {
					add(JavaTokenConstants.J_TRY_WITH_RESOURCE, pos, "try");
				}
				tryStmt.getResources().forEach(this::dispatch);
				NodeList<CatchClause> catchClauses = tryStmt.getCatchClauses();
				catchClauses.forEach(this::dispatch);
				Optional<BlockStmt> finallyBlock = tryStmt.getFinallyBlock();
				if (finallyBlock.isPresent()) {
					Optional<Range> finalRange = Optional.empty();
					if (catchClauses.isNonEmpty()) {
						finalRange = catchClauses.get(catchClauses.size() - 1).getRange();
					}
					var finalPos = finalRange.map((Range r) -> r.end).orElseGet(
							() -> finallyBlock.get().getRange().map(r -> r.begin).orElse(pos)
					);

					add(JavaTokenConstants.J_FINALLY, finalPos, "finally");
					dispatch(finallyBlock.get());
				}
			}
			statement.ifExplicitConstructorInvocationStmt(invocationStmt -> add(
					JavaTokenConstants.J_APPLY,
					pos,
					invocationStmt.isThis() ? "this" : "super"));
		}

		return traversed;
	}

	void dispatch(Node node) {
		boolean traversed = this.beforeDispatch(node);

		if (!traversed) {
			node.getChildNodes().forEach(this::dispatch);

			this.afterDispatch(node);
		}
	}
}

class JavaParserAdapter {
	int parseFiles(File dir, File file, Parser parser) {
		String filename = dir.toURI().relativize(file.toURI()).getPath();

		CompilationUnit compilationUnit;
		try {
			compilationUnit = StaticJavaParser.parse(file);
		} catch (FileNotFoundException | ParseProblemException e) {
			e.printStackTrace();
			return 1;

		}

		try {
			new JavaParserWalker(parser, filename).dispatch(compilationUnit);
		} finally {
			parser.add(JavaTokenConstants.FILE_END.getValue(), filename, -1, -1, 1);
		}

		return 0;
	}
}

public class Parser  extends jplag.Parser {
	private jplag.Structure struct;

	public static void main(String[] args) {
		var parser = new Parser();
		for (Token token : parser.parse(new File("/home/thomas/software/jplag/jplag.frontend.java-1.13/src/main/java/jplag/java113/"), new String[]{"Parser.java"}).tokens) {
			if (token != null) {
				System.out.println(token.getLine() + ":" + token.getColumn() + "\t" + token.toString() + " (" + token.getLength() + ")");
			}
		}
	}

	jplag.Structure parse(File dir, String[] files) {
		struct = new jplag.Structure();
		this.errors = 0;
		for (String file : files) {
			this.errors += new JavaParserAdapter().parseFiles(dir, new File(dir, file), this);
		}

		this.parseEnd();
		return struct;
	}

	void add(int type,String filename, long line, long col, long length) {
		struct.addToken(new JavaToken(type, filename, (int) line, (int)col,(int)length));
	}

	public void errorsInc() {
		errors++;
	}
}
