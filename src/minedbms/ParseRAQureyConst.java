package minedbms;

import minedbms.datatype.Relation;
import minedbms.operation.RelationOperation;

public class ParseRAQureyConst {
    
    public static class OptLeft {

        public static final String PROJECT = "PROJ_";
        public static final String SELECT = "SELE_";
        public static final String CONDITION_START = "{";
        public static final String CONDITION_END = "}";
        public static final String[] ALL = {PROJECT, SELECT};

    }

    public static class OptCent {

        public static final String UNION = "U";
        public static final String INTERSECT = "INTE";
        public static final String DIFFERENCE = "-";
        public static final String CROSS_PRODUCT = "X";
        public static final String NATURAL_JOIN = "*";
        // do not change the order of array
        public static final String[] ALL = {UNION, INTERSECT, DIFFERENCE, CROSS_PRODUCT, NATURAL_JOIN};

        public static final optCentMethod[] METHOD = {
            RelationOperation::union,
            RelationOperation::intersect,
            RelationOperation::difference,
            RelationOperation::crossProduct,
            RelationOperation::naturalJoin
        };

        @FunctionalInterface
        public interface optCentMethod {

            Relation operate(Relation r1, Relation r2);
        }

    }

    public static class InequalitySymbol {

        public static final String GREATER_THAN_EQUAL_TO = ">=";
        public static final String LESS_THAN_EQUAL_TO = "<=";
        public static final String GREATER_THAN = ">";
        public static final String EQUAL_TO = "=";
        public static final String LESS_TAHN = "<";
        // LESS_THAN_EQUAL_TO, GREATER_THAN_EQUAL_TO should be preceeding for test before the other
        // do not change the order of array
        public static final String[] ALL = {GREATER_THAN_EQUAL_TO, LESS_THAN_EQUAL_TO, GREATER_THAN, EQUAL_TO, LESS_TAHN};
    }

    public static class LogicalConjunctionSymbol {

        public static final String AND = ", ";
        public static final String OR = "OR";
        // LESS_THAN_EQUAL_TO, GREATER_THAN_EQUAL_TO should be preceeding for test before the other
        public static final String[] ALL = {AND, OR};
    }

    static final char[] PARENTHISIS = {'(', ')'};
    static final char PARENTHISIS_REPLACED = '|';
}
