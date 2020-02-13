package org.checkerframework.checker.objectconstruction;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import java.util.Collection;
import org.checkerframework.javacutil.BugInCF;
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
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * This class contains static methods that parse and evaluate the arguments of {@link
 * org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate} annotations, so that
 * they can be compared to each other and to {@link
 * org.checkerframework.checker.objectconstruction.qual.CalledMethods} annotations.
 */
public class CalledMethodsPredicateEvaluator {

  /** The parser to use when converting formulae to ASTs. */
  private static final JavaParser parser = new JavaParser();

  /** Do not use. This class is not instantiable; use the static methods instead. */
  private CalledMethodsPredicateEvaluator() {
    throw new Error("Do not instantiate");
  }

  /** Construct a solver to use when comparing predicates. Each query should use a new solver. */
  private static SolverContext setupSolver() {
    Configuration config = Configuration.defaultConfiguration();
    LogManager log = LogManager.createNullLogManager();
    ShutdownNotifier notifier = ShutdownManager.create().getNotifier();
    try {
      return SolverContextFactory.createSolverContext(
          config, log, notifier, SolverContextFactory.Solvers.SMTINTERPOL);
    } catch (InvalidConfigurationException e) {
      throw new BugInCF("Could not initialize solver.");
    }
  }

  /**
   * Return true iff the boolean formula "lhs &rarr; rhs" is valid, treating all variables as
   * uninterpreted.
   *
   * @param lhs a Java boolean expression containing only variables, {@literal &}{@literal &}, ||,
   *     !, and ()
   * @param rhs a Java boolean expression containing only variables, {@literal &}{@literal &}, ||,
   *     !, and ()
   * @return true iff lhs implies rhs
   * @throws UnsupportedOperationException if the input contains an unexpected kind of expression
   */
  public static boolean implies(final String lhs, final String rhs) {

    // General approach: parse each formula into an AST, then convert each AST into a boolean
    // formula that can be passed to a solver, and then construct a query to check if "not (lhs ->
    // rhs)" is unsatisfiable. The result is the result of that query.

    // setup solver
    SolverContext context = setupSolver();

    BooleanFormulaManager booleanFormulaManager =
        context.getFormulaManager().getBooleanFormulaManager();

    // parse the formulas into the solver's format
    BooleanFormula lhsBool = formulaStringToBooleanFormula(lhs, booleanFormulaManager);
    BooleanFormula rhsBool = formulaStringToBooleanFormula(rhs, booleanFormulaManager);

    BooleanFormula satQuery =
        booleanFormulaManager.not(booleanFormulaManager.implication(lhsBool, rhsBool));
    ProverEnvironment prover =
        context.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS);

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

  /** Converts a Java boolean expression into a boolean formula in the SMT solver's format. */
  private static BooleanFormula formulaStringToBooleanFormula(
      String formula, BooleanFormulaManager booleanFormulaManager) {

    String classWithFormula = "class DUMMY { boolean DUMMY() { return " + formula + "; } }";

    CompilationUnit ast = parser.parse(classWithFormula).getResult().orElse(null);

    if (ast == null) {
      throw new BugInCF("Unparseable formula: " + formula);
    }

    BlockStmt theBlock = ast.getType(0).getMembers().get(0).asMethodDeclaration().getBody().get();

    com.github.javaparser.ast.expr.Expression theExpression =
        theBlock.getStatements().get(0).asReturnStmt().getExpression().get();

    return expressionToBooleanFormula(theExpression, booleanFormulaManager);
  }

  /**
   * Convert between Javaparser expression and boolean formula via recursive descent.
   *
   * @throws UnsupportedOperationException if an unexpected expression is encountered
   */
  private static BooleanFormula expressionToBooleanFormula(
      com.github.javaparser.ast.expr.Expression theExpression,
      BooleanFormulaManager booleanFormulaManager) {
    if (theExpression.isNameExpr()) {
      return booleanFormulaManager.makeVariable(theExpression.asNameExpr().getNameAsString());
    } else if (theExpression.isBinaryExpr()) {
      if (theExpression.asBinaryExpr().getOperator().equals(BinaryExpr.Operator.OR)) {
        return booleanFormulaManager.or(
            expressionToBooleanFormula(
                theExpression.asBinaryExpr().getLeft(), booleanFormulaManager),
            expressionToBooleanFormula(
                theExpression.asBinaryExpr().getRight(), booleanFormulaManager));
      } else if (theExpression.asBinaryExpr().getOperator().equals(BinaryExpr.Operator.AND)) {
        return booleanFormulaManager.and(
            expressionToBooleanFormula(
                theExpression.asBinaryExpr().getLeft(), booleanFormulaManager),
            expressionToBooleanFormula(
                theExpression.asBinaryExpr().getRight(), booleanFormulaManager));
      }
    } else if (theExpression.isEnclosedExpr()) {
      return expressionToBooleanFormula(
          theExpression.asEnclosedExpr().getInner(), booleanFormulaManager);
    } else if (theExpression.isUnaryExpr()) {
      if (theExpression.asUnaryExpr().getOperator().equals(UnaryExpr.Operator.LOGICAL_COMPLEMENT)) {
        return booleanFormulaManager.not(
            expressionToBooleanFormula(
                theExpression.asUnaryExpr().getExpression(), booleanFormulaManager));
      }
    }
    throw new UnsupportedOperationException(
        "encountered an unexpected type of expression in an "
            + "@CalledMethodsPredicate expression: "
            + theExpression
            + " was of type "
            + theExpression.getClass());
  }

  /**
   * Evaluate the given expression if every String in {@code trueVariables} is replaced by "true" in
   * the boolean formula.
   *
   * @param expression a boolean formula in Java format, as a String
   * @param trueVariables the variables in the boolean expression to treat as "true"
   * @return whether the expression evaluates to true in the context where only the variables in
   *     trueVariables are true, and all other variables are false
   */
  public static boolean evaluate(String expression, Collection<String> trueVariables) {

    for (String cmMethod : trueVariables) {
      expression = expression.replaceAll(cmMethod, "true");
    }

    expression = expression.replaceAll("(?!true)\\b[_a-zA-Z][_a-zA-Z0-9]*\\b", "false");

    ExpressionParser parser = new SpelExpressionParser();
    Expression exp = parser.parseExpression(expression);
    return exp.getValue(Boolean.class);
  }
}
