package minedbms;

import minedbms.operation.RelationOperation;
import minedbms.MineDBMS;
import minedbms.datatype.Relation;
import minedbms.datatype.Condition;
import minedbms.datatype.Condition.Inequality;
import java.io.FileNotFoundException;
import javafx.util.Pair;

public class ParseRAQurey {

    private static class OptLeft {

        private static final String PROJECT = "PROJ_";
        private static final String SELECT = "SELE_";
        private static final String CONDITION_START = "{";
        private static final String CONDITION_END = "}";
        private static final String[] ALL = {PROJECT, SELECT};

    }

    private static class OptCent {

        private static final String UNION = "U";
        private static final String INTERSECT = "INTE";
        private static final String DIFFERENCE = "-";
        private static final String CROSS_PRODUCT = "X";
        private static final String NATURAL_JOIN = "*";
        // do not change the order of array
        private static final String[] ALL = {UNION, INTERSECT, DIFFERENCE, CROSS_PRODUCT, NATURAL_JOIN};

        private static final optCentMethod[] METHOD = {
            RelationOperation::union,
            RelationOperation::intersect,
            RelationOperation::difference,
            RelationOperation::crossProduct,
            RelationOperation::naturalJoin
        };

        @FunctionalInterface
        private interface optCentMethod {

            Relation operate(Relation r1, Relation r2);
        }

    }

    private static class InequalitySymbol {

        private static final String GREATER_THAN_EQUAL_TO = ">=";
        private static final String LESS_THAN_EQUAL_TO = "<=";
        private static final String GREATER_THAN = ">";
        private static final String EQUAL_TO = "=";
        private static final String LESS_TAHN = "<";
        // LESS_THAN_EQUAL_TO, GREATER_THAN_EQUAL_TO should be preceeding for test before the other
        // do not change the order of array
        private static final String[] ALL = {GREATER_THAN_EQUAL_TO, LESS_THAN_EQUAL_TO, GREATER_THAN, EQUAL_TO, LESS_TAHN};
    }

    private static class LogicalConjunctionSymbol {

        private static final String AND = ", ";
        private static final String OR = "OR";
        // LESS_THAN_EQUAL_TO, GREATER_THAN_EQUAL_TO should be preceeding for test before the other
        private static final String[] ALL = {AND, OR};
    }

    private static final char[] PARENTHISIS = {'(', ')'};
    private static final char PARENTHISIS_REPLACED = '|';
    private MineDBMS dbms;

    public ParseRAQurey(MineDBMS dbms) {
        this.dbms = dbms;
    }

    public Relation parse(String query) throws FileNotFoundException {
        if (query == null || !validParenthesis(query)) {
            return null;
        }
        // get rid off redundant parenthesises and space
        // to make query from "   (    ( PROJ_{ANO} (SELE_{Payment > 50} Play)   )   )    " to "PROJ_{ANO} (SELE_{Payment > 50} Play)", so if there is Parenthesis at first char, it must no be a redundant one
        query = cutParenthisis(query);

        if (query.charAt(0) == '(') {  // imply there is a relation at begining of query
            //index of corresponding right parenthesis of first char, which is left parenthesis by passing "query.charAt(0) == '('", of $query
            int rightParentIdx = findPair(query, 0);
            Relation leftR = parse(query.substring(0, rightParentIdx + 1));
            if (isRemain(query.substring(rightParentIdx + 1))) { // if there are string remaining after $rightParentIdx
                int firstParenAfter = findFirstParenAfter(query, rightParentIdx); // find first '(' in $query after $rightParentIdx
                int endOfSearch;
                if (firstParenAfter == -1) {
                    endOfSearch = query.length();
                } else {
                    endOfSearch = firstParenAfter;
                }
                //find optCent in the $query after $rightParentIdx but before $firstParenAfter, and return the type of it, starting index and ending index of it
                // e.g. {1, 29, 30} means the oprCent found is "X" and it is started at 29 and ended at index 30
                // if not found all value will be -1
                int[] optCentFound = findOptCent(query, rightParentIdx, endOfSearch);
                int optCentEndIdx = optCentFound[2];
                if (optCentEndIdx == -1) { // which mean not found optCent, should throw exception since there should be either noting after $rightParentIdx or a oprCent and relation after oprCent
                    throw new IllegalArgumentException();
                } else {
                    Relation rightR = parse(query.substring(optCentEndIdx));
                    return callOptCent(optCentFound[0], leftR, rightR);
                }
            }
        } else { // the first char is not '(', that means the query is either started with a name of a relation file or optLeft
            int firstParenAfter = findFirstParenAfter(query, 0); // find first '(' in $query after $rightParentIdx
            int endOfSearch;
            if (firstParenAfter == -1) {
                endOfSearch = query.length();
            } else {
                endOfSearch = firstParenAfter;
            }
            //find optLeft in the $query, and return the type of it, starting index of condtion, and index of '}'
            // e.g. {1, 6, 21} means the optLeft found is "SELE", condition is from 6 to 20, and '}' at 21
            int[] optLeftFound = findOptLeft(query, 0, endOfSearch);
            int optLeftEndIdx = optLeftFound[2];
            if (optLeftEndIdx != -1) { // which mean found optLeft, anything after it's condition consider as a relation it take
                Relation r = parse(query.substring(optLeftEndIdx + 1));
                int startCond = optLeftFound[1];
                if (optLeftFound[0] == 0) { // 0 is project
                    return projectParse(query.substring(startCond, optLeftEndIdx), r); // passing condtion string and the relation for futher parsing
                } else { // 1 is select
                    return selectParse(query.substring(startCond, optLeftEndIdx), r); // passing condtion string and the relation for futher parsing
                }
            } else { // if optLeft not found, read the query as a name of a relation file, until end of query or optCent            
                // following is similar to what is written in the true clause of first if in parse
                int[] optCentFound = findOptCent(query, 0, endOfSearch);
                int optCentStartIdx = optCentFound[1];
                int optCentEndIdx = optCentFound[2];
                if (optCentEndIdx == -1) { // which mean not found optCent, so the whole query is a name of a relation file 
                    return this.dbms.getRelation(query); // might not find the file, but this is the user's problem instead the program side
                } else {
                    Relation leftR = parse(query.substring(0, optCentStartIdx));
                    Relation rightR = parse(query.substring(optCentEndIdx));
                    return callOptCent(optCentFound[0], leftR, rightR);
                }
            }
        }
        return null;
    }

    private Relation projectParse(String condition, Relation r) {
        String[] attrs = condition.split(",");
        for (int i = 0; i < attrs.length; i++) {
            attrs[i] = cutSpaces(attrs[i]);
        }
        return RelationOperation.project(r, attrs);
    }

    private Relation selectParse(String condition, Relation r) {
        // haven't implemented complex condition parsing
        Pair<Integer, Integer> symbolIdx = indexOf(condition, InequalitySymbol.ALL);
        String attributeName = cutSpaces(condition.substring(0, symbolIdx.getValue()));
        String symbol = InequalitySymbol.ALL[symbolIdx.getKey()];
        String comparedValue = cutSpaces(condition.substring(symbolIdx.getValue() + symbol.length()));
        Condition cond = null;
        switch (symbolIdx.getKey()) {
            case 0:
                cond = new Inequality.GreaterThanEqualTo(attributeName, comparedValue);
                break;
            case 1:
                cond = new Inequality.LessThanEqualTo(attributeName, comparedValue);
                break;
            case 2:
                cond = new Inequality.GreaterThan(attributeName, comparedValue);
                break;
            case 3:
                cond = new Inequality.EqualTo(attributeName, comparedValue);
                break;
            case 4:
                cond = new Inequality.LessThan(attributeName, comparedValue);
                break;
        }
        if (cond == null) {
            return null;
        }
        return RelationOperation.select(r, cond);
    }

    private Relation callOptCent(int optCentType, Relation leftR, Relation rightR) {
        //{UNION, INTERSECT, DIFFERENCE, CROSS_PRODUCT, NATURAL_JOIN};
        return OptCent.METHOD[optCentType].operate(leftR, rightR);
    }

    /*
    Return:
    type
    start index of the optCent
    end index of the optCent (the index right after last char of opeCent)
     */
    private int[] findOptCent(String query, int start, int end) {
        for (int optCentType = 0; optCentType < OptCent.ALL.length; optCentType++) {
            for (int qCharIdx = start; qCharIdx < end && qCharIdx < query.length(); qCharIdx++) {
                int lastChar = qCharIdx + OptCent.ALL[optCentType].length() - 1;
                if (lastChar < end && lastChar < query.length()) { // to check if the lastChar of tested char is out of bounds.
                    boolean foundOptCent = true;
                    for (int opeCentIdx = 0; opeCentIdx < OptCent.ALL[optCentType].length(); opeCentIdx++) {
                        if (query.charAt(opeCentIdx + qCharIdx) != OptCent.ALL[optCentType].charAt(opeCentIdx)) {
                            foundOptCent = false;
                            break;
                        }
                    }
                    if (foundOptCent) {
                        return new int[]{optCentType, qCharIdx, qCharIdx + OptCent.ALL[optCentType].length()};
                    }
                }
            }
        }
        return new int[]{-1, -1, -1};
    }

    /*
    Return:
    type
    start index of the condition
    index of '}'
     */
    // the left opt should be started right at begining of query, can't be after any other substing instead plain spaces.
    // Here is a optional design on the thing between '_' and '{'. I choose to force only spaces or nothing is valid between them
    private int[] findOptLeft(String query, int start, int end) {
        query = cutSpaces(query);
        int condStart = -1;
        int rightBreket = -1;
        for (int optCentType = 0; optCentType < OptLeft.ALL.length; optCentType++) {
            int lastChar = start + OptLeft.ALL[optCentType].length() - 1;
            if (lastChar < end && lastChar < query.length()) { // to check if the lastChar of tested char is out of bounds.
                boolean foundOptLeft = true;
                for (int qCharIdx = 0; qCharIdx < OptLeft.ALL[optCentType].length(); qCharIdx++) {
                    if (query.charAt(qCharIdx + start) != OptLeft.ALL[optCentType].charAt(qCharIdx)) {
                        foundOptLeft = false;
                        break;
                    }
                }
                if (foundOptLeft) {
                    boolean foundLeftBreket = false;
                    boolean foundRedundant = false;
                    boolean foundCond = false;
                    for (int qCharIdx = start + OptLeft.ALL[optCentType].length(); qCharIdx < query.length(); qCharIdx++) {
                        if (!foundLeftBreket && query.charAt(qCharIdx) != '{' && query.charAt(qCharIdx) != ' ') {
                            foundRedundant = true;
                        } else if (query.charAt(qCharIdx) == '{') {
                            if (foundRedundant) {
                                throw new IllegalArgumentException(String.format("Error: There should not be anything except spaces between \"%s\" and '}'.", OptLeft.ALL[optCentType]));
                            }
                            condStart = qCharIdx + 1;
                            foundLeftBreket = true;
                        } else if (foundLeftBreket && query.charAt(qCharIdx) == '}') {
                            rightBreket = qCharIdx;
                            foundCond = true;
                            break;
                        }
                    }
                    if (foundCond) {
                        return new int[]{optCentType, condStart, rightBreket};
                    }
                }
            }
        }
        return new int[]{-1, -1, -1};
    }

    private boolean isRemain(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ' ') {
                return true;
            }
        }
        return false;
    }

    private int findFirstParenAfter(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '(') {
                return i;
            }
        }
        return -1;
    }

    public static String cutSpaces(String str) {
        int start = -1, end = -1;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ' ') {
                start = i;
                break;
            }
        }
        for (int i = str.length() - 1; i > start; i--) {
            if (str.charAt(i) != ' ') {
                end = i + 1;
                break;
            }
        }
        return str.substring(start, end);
    }

    private static String cutParenthisis(String str) {
        String cut = cutSpaces(str);
        int[] index = findParenthesisPair(cut, 0);
        while (index[0] == 0 && index[1] == cut.length() - 1) {
            cut = cutSpaces(cut.substring(index[0] + 1, index[1]));
            index = findParenthesisPair(cut, 0);
        }
        return cut;
    }

    /*
    return: 
    First int: the index of the "matched string" in target
    Second int: the index of the "matched string" in str
     */
    private static Pair<Integer, Integer> indexOf(String str, String[] target) {
        for (int i = 0; i < target.length; i++) {
            int idx = str.indexOf(target[i]);
            if (idx != -1) {
                return new Pair<Integer, Integer>(i, idx);
            }
        }
        return new Pair<Integer, Integer>(-1, null);
    }

    // From the start of str, found a paired parenthesis and return the index of them.
    // If 
    // 1. there is no parenthesis, 
    // 2. no matched right parenthesis for the first left parenthesis, 
    // return [-1, -1]
    private static int[] findParenthesisPair(String str, int startIdx) {
        int[] parenthesisIdx = {-1, -1};
        if (startIdx >= str.length()) {
            return parenthesisIdx;
        }
        boolean foundLeft = false;
        int count = 0;
        for (int i = startIdx; i < str.length(); i++) {
            if (str.charAt(i) == PARENTHISIS[0]) {
                if (!foundLeft) {
                    foundLeft = true;
                    parenthesisIdx[0] = i;
                } else {
                    count++;
                }
            } else if (str.charAt(i) == PARENTHISIS[1]) {
                if (foundLeft && count == 0) {
                    parenthesisIdx[1] = i;
                    break;
                }
                count--;
            }
        }
        if (parenthesisIdx[1] == -1) {
            parenthesisIdx[0] = -1;
        }
        return parenthesisIdx;
    }

    private static int findPair(String str, int leftParenIdx) {
        if (leftParenIdx >= str.length() - 1 || str.charAt(leftParenIdx) != '(') {
            return -1;
        }
        int count = 0;
        for (int i = leftParenIdx + 1; i < str.length(); i++) {
            if (str.charAt(i) == PARENTHISIS[0]) {
                count++;
            } else if (str.charAt(i) == PARENTHISIS[1]) {
                if (count == 0) {
                    return i;
                }
                count--;
            }
        }
        return -1;
    }

    private static boolean validParenthesis(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == PARENTHISIS[0]) {
                count++;
            } else if (str.charAt(i) == PARENTHISIS[1]) {
                count--;
            }
            if (count < 0) {
                return false;
            }
        }
        return count == 0;
    }
}
