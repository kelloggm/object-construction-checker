package org.checkerframework.checker.builder;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Set;

/**
 * This class parses and evaluates a single @CalledMethodsPredicate argument.
 */
public class CalledMethodsPredicateEvaluator {

    // A set containing all the names of methods that ought to evaluate to true.
    private final Set<String> cmMethods;

    public CalledMethodsPredicateEvaluator(final Set<String> cmMethods) {
        this.cmMethods = cmMethods;
    }

    protected boolean evaluate(String expression) {

        for (String cmMethod : cmMethods) {
            expression = expression.replaceAll(cmMethod, "true");
        }

        expression = expression.replaceAll("((?!true)[a-zA-Z0-9])+", "false");

        // horrible hack but I can't figure out the right regex to make the above not replace "true" with "tfalse"
        expression = expression.replaceAll("tfalse", "true");

        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(expression);
        return exp.getValue(Boolean.class);
    }
}
