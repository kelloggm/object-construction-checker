package org.checkerframework.checker.objectconstruction;

import java.util.Set;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.printer.YamlPrinter;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/** This class parses and evaluates a single @CalledMethodsPredicate argument. */
public class CalledMethodsPredicateEvaluator {

  // A set containing all the names of methods that ought to evaluate to true.
  private final Set<String> cmMethods;

  public CalledMethodsPredicateEvaluator(final Set<String> cmMethods) {
    this.cmMethods = cmMethods;
  }

  /**
   * Return true iff the boolean formula "lhs -> rhs" is valid, treating all variables as uninterpreted.
   *
   * Used to determine subtyping between two @CalledMethodsPredicate annotations that are not identical.
   */
  public static boolean implies(final String lhsOrig, final String rhsOrig) {

    JavaParser parser = new JavaParser();

    String preamble = "class DUMMY { boolean DUMMY() { return ";
    String afterward = "; } }";

    String lhs = preamble + lhsOrig + afterward;
    String rhs = preamble + rhsOrig + afterward;

    CompilationUnit lhsAst = parser.parse(lhs).getResult().orElse(null);
    CompilationUnit rhsAst = parser.parse(rhs).getResult().orElse(null);

    if (lhsAst == null || rhsAst == null) {
      return false;
    }

    Configuration config = Configuration.defaultConfiguration();
    LogManager log = LogManager.createNullLogManager();
    ShutdownNotifier notifier = ShutdownManager.create().getNotifier();
    SolverContext context = null;
    try {
      context = SolverContextFactory.createSolverContext(
              config, log, notifier, SolverContextFactory.Solvers.SMTINTERPOL);
    } catch (InvalidConfigurationException e) {
      return false;
    }

    BooleanFormulaManager booleanFormulaManager = context.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula lhsBool, rhsBool;

    try {
      lhsBool = compilationUnitToBooleanFormula(lhsAst, booleanFormulaManager);
      rhsBool = compilationUnitToBooleanFormula(rhsAst, booleanFormulaManager);
    } catch (UnsupportedOperationException e) {
      return false;
    }

    BooleanFormula satQuery = booleanFormulaManager.not(booleanFormulaManager.implication(lhsBool, rhsBool));

    ProverEnvironment prover = context.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS);

    try {
      prover.addConstraint(satQuery);
    } catch (InterruptedException e) {
      return false;
    }

    try {
      return prover.isUnsat();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Converts a Javaparser AST representing a boolean expression into
   * a boolean formula that could be passed to an SMT solver.
   */
  private static BooleanFormula compilationUnitToBooleanFormula(CompilationUnit ast, BooleanFormulaManager booleanFormulaManager) {

    BlockStmt theBlock = ast.getType(0).getMembers().get(0).asMethodDeclaration().getBody().get();

    com.github.javaparser.ast.expr.Expression theExpression = theBlock.getStatements().get(0).asReturnStmt().getExpression().get();

    try {
      return expressionToBooleanFormula(theExpression, booleanFormulaManager);
    } catch (UnsupportedOperationException e) {
      throw e;
    }
  }

  /**
   * Actual implementation of conversion between Javaparser expression and boolean formula.
   */
  private static BooleanFormula expressionToBooleanFormula(com.github.javaparser.ast.expr.Expression theExpression, BooleanFormulaManager booleanFormulaManager) {
    if (theExpression.isNameExpr()) {
      return booleanFormulaManager.makeVariable(theExpression.asNameExpr().getNameAsString());
    } else if (theExpression.isBinaryExpr()) {
      if (theExpression.asBinaryExpr().getOperator().equals(BinaryExpr.Operator.OR)) {
        return booleanFormulaManager.or(expressionToBooleanFormula(theExpression.asBinaryExpr().getLeft(), booleanFormulaManager),
                expressionToBooleanFormula(theExpression.asBinaryExpr().getRight(), booleanFormulaManager));
      } else if (theExpression.asBinaryExpr().getOperator().equals(BinaryExpr.Operator.AND)) {
        return booleanFormulaManager.and(expressionToBooleanFormula(theExpression.asBinaryExpr().getLeft(), booleanFormulaManager),
                expressionToBooleanFormula(theExpression.asBinaryExpr().getRight(), booleanFormulaManager));
      }
    }
    throw new UnsupportedOperationException();
  }


  protected boolean evaluate(String expression) {

    for (String cmMethod : cmMethods) {
      expression = expression.replaceAll(cmMethod, "true");
    }

    expression = expression.replaceAll("((?!true)[a-zA-Z0-9])+", "false");

    // horrible hack but I can't figure out the right regex to make the above not replace "true"
    // with "tfalse"
    expression = expression.replaceAll("tfalse", "true");

    ExpressionParser parser = new SpelExpressionParser();
    Expression exp = parser.parseExpression(expression);
    return exp.getValue(Boolean.class);
  }
}
