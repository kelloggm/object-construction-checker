package org.checkerframework.checker.builder;

import com.fathzer.soft.javaluator.AbstractEvaluator;
import com.fathzer.soft.javaluator.BracketPair;
import com.fathzer.soft.javaluator.Operator;
import com.fathzer.soft.javaluator.Parameters;

import java.util.Iterator;
import java.util.Set;

/**
 * This class parses and evaluates a single @CalledMethodsPredicate argument.
 * It is based on an answer to this StackOverflow question:
 * https://stackoverflow.com/questions/12203003/boolean-expression-parser-in-java
 */
public class CalledMethodsPredicateEvaluator extends AbstractEvaluator<String> {
    /**
     * The logical AND operator.
     */
    private final static Operator AND =
            new Operator("&&", 2, Operator.Associativity.LEFT, 2);
    /**
     * The logical OR operator.
     */
    private final static Operator OR =
            new Operator("||", 2, Operator.Associativity.LEFT, 1);

    private static final Parameters PARAMETERS;

    // A set containing all the names of methods that ought to evaluate to true.
    private final Set<String> cmMethods;

    static {
        // Create the evaluator's parameters
        PARAMETERS = new Parameters();
        // Add the supported operators
        PARAMETERS.add(AND);
        PARAMETERS.add(OR);
        // Add the parentheses
        PARAMETERS.addExpressionBracket(BracketPair.PARENTHESES);
    }

    public CalledMethodsPredicateEvaluator(final Set<String> cmMethods) {
        super(PARAMETERS);
        this.cmMethods = cmMethods;
    }

    @Override
    protected String toValue(final String literal, final Object evaluationContext) {
        return literal;
    }

    private boolean getValue(final String literal) {
        return cmMethods.contains(literal) || "true".equals(literal);
    }

    @Override
    protected String evaluate(final Operator operator, final Iterator<String> operands,
                              final Object evaluationContext) {
        String o1 = operands.next();
        String o2 = operands.next();
        Boolean result;
        if (operator == OR) {
            result = getValue(o1) || getValue(o2);
        } else if (operator == AND) {
            result = getValue(o1) && getValue(o2);
        } else {
            throw new IllegalArgumentException();
        }
        return result.toString();
    }
}
