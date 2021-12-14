package minedbms;

import minedbms.operation.RelationOperation;
import minedbms.MineDBMS;
import minedbms.datatype.Relation;
import minedbms.datatype.Condition;
import minedbms.datatype.Condition.Inequality;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import javafx.util.Pair;

public class ParseRAQureyOld {

    private static class OptLeft {

        private static final String PROJECT = "PROJ_{";
        private static final String SELECT = "SELE_{";
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

    public ParseRAQureyOld(MineDBMS dbms) {
        this.dbms = dbms;
    }

    public Relation parse(String query) throws FileNotFoundException {
        if (query == null || !validParenthesis(query)) {
            return null;
        }
        query = cutParenthisis(query);
        ArrayList<Pair<int[], String>> parsedRelation = new ArrayList();
        int scaned = 0;
        int[] index = new int[2];
        do {
            index[0] = index[1] = -1;
            index = findParenthesisPair(query, scaned);
            int leftParen = index[0];
            int rightParen = index[1];
            if (leftParen != -1 && rightParen != -1) {
                String subquery = query.substring(leftParen, rightParen + 1);
                int[] key = {index[0], index[1]};
                parsedRelation.add(new Pair<int[], String>(key, subquery));
                scaned = rightParen + 1;
            }
        } while (index[0] != -1 && index[1] != -1);

        return parseOpt(query, parsedRelation);
    }

    private Relation parseOpt(String query, ArrayList<Pair<int[], String>> parsedRelation) throws FileNotFoundException {
        query = cutParenthisis(query);
        if (query.length() >= OptLeft.PROJECT.length() && query.substring(0, OptLeft.PROJECT.length()).equals(OptLeft.PROJECT)) {
            return projectParse(query, parsedRelation);
        } else if (query.length() >= OptLeft.SELECT.length() && query.substring(0, OptLeft.SELECT.length()).equals(OptLeft.SELECT)) {
            return selectParse(query, parsedRelation);
        }
        Pair<Integer, Pair<Integer, Integer>> optCentIdx = findOptCent(query, parsedRelation);
        if (optCentIdx.getKey() != -1) {
            switch (optCentIdx.getKey()) {
                case 0:
                    return unionParse(query, optCentIdx.getValue(), parsedRelation);
                case 1:
                    return intersectParse(query, optCentIdx.getValue(), parsedRelation);
                case 2:
                    return differenceParse(query, optCentIdx.getValue(), parsedRelation);
                case 3:
                    return crossProductParse(query, optCentIdx.getValue(), parsedRelation);
                case 4:
                    return naturalJoinParse(query, optCentIdx.getValue(), parsedRelation);

            }
        }
        Relation r = this.dbms.getRelation(query);
        return r;
    }

    private Relation projectParse(String query, ArrayList<Pair<int[], String>> parsedRelation) throws FileNotFoundException {
        if (!query.substring(0, OptLeft.PROJECT.length()).equals(OptLeft.PROJECT)) {
            return null;
        }
        if (parsedRelation != null) {
            query = replaceQuery(query, parsedRelation);
        }

        int conditionEndIdx = query.indexOf(OptLeft.CONDITION_END);
        String attributeList = query.substring(OptLeft.PROJECT.length(), conditionEndIdx);
        String[] attrs = attributeList.split(",");
        for (int i = 0; i < attrs.length; i++) {
            attrs[i] = cutSpaces(attrs[i]);
        }
        Relation r;
        if (parsedRelation == null || query.indexOf("|0|") == -1) {
            String remain = query.substring(conditionEndIdx + 1);
            r = parse(cutSpaces(remain));
        } else {
            r = parse(parsedRelation.get(0).getValue());
        }
        return RelationOperation.project(r, attrs);
    }

    // for condition, only implement constant comparing, and constant should at right of inequality symbol
    private Relation selectParse(String query, ArrayList<Pair<int[], String>> parsedRelation) throws FileNotFoundException {
        if (!query.substring(0, OptLeft.SELECT.length()).equals(OptLeft.SELECT)) {
            return null;
        }
        if (parsedRelation != null) {
            query = replaceQuery(query, parsedRelation);
        }
        int conditionEndIdx = query.indexOf(OptLeft.CONDITION_END);
        String conditionList = query.substring(OptLeft.SELECT.length(), conditionEndIdx);

        // haven't implemented complex condition parsing
        Pair<Integer, Integer> symbolIdx = indexOf(conditionList, InequalitySymbol.ALL);
        String attributeName = cutSpaces(conditionList.substring(0, symbolIdx.getValue()));
        String symbol = InequalitySymbol.ALL[symbolIdx.getKey()];
        String comparedValue = cutSpaces(conditionList.substring(symbolIdx.getValue() + symbol.length()));
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
        Relation r;
        if (parsedRelation == null || query.indexOf("|0|") == -1) {
            String remain = query.substring(conditionEndIdx + 1);
            r = parse(cutSpaces(remain));
        } else {
            r = parse(parsedRelation.get(0).getValue());
        }
        return RelationOperation.select(r, cond);
    }

    private Relation unionParse(String query, Pair<Integer, Integer> idx, ArrayList<Pair<int[], String>> parsedRelation) throws FileNotFoundException {
        String optCent = OptCent.UNION;
        Relation r1, r2;
        Pair<Relation, Relation> r1r2 = getR1R2(query, idx, parsedRelation, optCent);
        r1 = r1r2.getKey();
        r2 = r1r2.getValue();
        return RelationOperation.union(r1, r2);
    }

    private Relation intersectParse(String query, Pair<Integer, Integer> idx, ArrayList<Pair<int[], String>> parsedRelation) throws FileNotFoundException {
        String optCent = OptCent.INTERSECT;
        Relation r1, r2;
        Pair<Relation, Relation> r1r2 = getR1R2(query, idx, parsedRelation, optCent);
        r1 = r1r2.getKey();
        r2 = r1r2.getValue();
        return RelationOperation.intersect(r1, r2);
    }

    private Relation differenceParse(String query, Pair<Integer, Integer> idx, ArrayList<Pair<int[], String>> parsedRelation) throws FileNotFoundException {
        String optCent = OptCent.DIFFERENCE;
        Relation r1, r2;
        Pair<Relation, Relation> r1r2 = getR1R2(query, idx, parsedRelation, optCent);
        r1 = r1r2.getKey();
        r2 = r1r2.getValue();
        return RelationOperation.difference(r1, r2);
    }

    private Relation crossProductParse(String query, Pair<Integer, Integer> idx, ArrayList<Pair<int[], String>> parsedRelation) throws FileNotFoundException {
        String optCent = OptCent.CROSS_PRODUCT;
        Relation r1, r2;
        Pair<Relation, Relation> r1r2 = getR1R2(query, idx, parsedRelation, optCent);
        r1 = r1r2.getKey();
        r2 = r1r2.getValue();
        return RelationOperation.crossProduct(r1, r2);
    }

    private Relation naturalJoinParse(String query, Pair<Integer, Integer> idx, ArrayList<Pair<int[], String>> parsedRelation) throws FileNotFoundException {
        String optCent = OptCent.NATURAL_JOIN;
        Relation r1, r2;
        Pair<Relation, Relation> r1r2 = getR1R2(query, idx, parsedRelation, optCent);
        r1 = r1r2.getKey();
        r2 = r1r2.getValue();
        return RelationOperation.naturalJoin(r1, r2);
    }

    private Pair<Relation, Relation> getR1R2(String query, Pair<Integer, Integer> idx, ArrayList<Pair<int[], String>> parsedRelation, String optCent) throws FileNotFoundException {
        Pair<String, String> leftRightQueries = getQuery(query, idx, optCent);
        if (idx == null) {
            idx = findOptCent(query, parsedRelation).getValue();
        }

        Relation r1, r2;
        if (parsedRelation == null || parsedRelation.size() == 0) {
            r1 = parse(cutSpaces(leftRightQueries.getKey()));
            r2 = parse(cutSpaces(leftRightQueries.getValue()));

        } else if (parsedRelation.size() == 1) {
            if (query.indexOf(parsedRelation.get(0).getValue()) > idx.getValue()) { // if the parenthesis is after the operator symbol
                r1 = parse(cutSpaces(leftRightQueries.getKey()));
                r2 = parse(cutSpaces(parsedRelation.get(0).getValue()));
            } else {// if the parenthesis is before the operator symbol
                r1 = parse(cutSpaces(parsedRelation.get(0).getValue()));
                r2 = parse(cutSpaces(leftRightQueries.getValue()));
            }
        } else {
            r1 = parse(cutSpaces(parsedRelation.get(0).getValue()));
            r2 = parse(cutSpaces(parsedRelation.get(1).getValue()));
        }
        return new Pair<Relation, Relation>(r1, r2);
    }

    private Pair<String, String> getQuery(String query, Pair<Integer, Integer> symbolIdx, String symbol) {
        String leftQuery = cutSpaces(query.substring(0, symbolIdx.getValue() + 1));
        String rightQuery = cutSpaces(query.substring(symbolIdx.getValue() + symbol.length() + 2 - 1)); // plussing 2 due to actual match string in findOptCent have extra two char
        return new Pair<String, String>(leftQuery, rightQuery);
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

    private static String replaceQuery(String query, ArrayList<Pair<int[], String>> parsedRelation) {
        int start = 0;
        int end = 0;
        String newQuery = "";
        for (int i = 0; i < parsedRelation.size(); i++) {
            end = parsedRelation.get(i).getKey()[0];
            newQuery += query.substring(start, end) + PARENTHISIS_REPLACED + i + PARENTHISIS_REPLACED;
            start = parsedRelation.get(i).getKey()[1] + 1;
        }
        end = query.length();
        newQuery += query.substring(start, end);
        return newQuery;
    }

    private static Pair<Integer, Pair<Integer, Integer>> findOptCent(String query, ArrayList<Pair<int[], String>> parsedRelation) {
        //String newQuery = replaceQuery(query, parsedRelation);

        for (int i = 0; i < OptCent.ALL.length; i++) {
            String[] validComb = {
                " " + OptCent.ALL[i] + " ",
                " " + OptCent.ALL[i] + PARENTHISIS_REPLACED,
                PARENTHISIS_REPLACED + OptCent.ALL[i] + " ",
                PARENTHISIS_REPLACED + OptCent.ALL[i] + PARENTHISIS_REPLACED};

            /*
            return: 
            First int: the index of the "matched string" in target
            Second int: the index of the "matched string" in str
             */
            Pair<Integer, Integer> symbolIdx = indexOf(query, validComb, parsedRelation);
            if (symbolIdx.getKey() != -1) {
                return new Pair<Integer, Pair<Integer, Integer>>(i, symbolIdx);
            }
        }
        return new Pair<Integer, Pair<Integer, Integer>>(-1, null);
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

    /*
    return: 
    First int: the index of the "matched string" in target
    Second int: the index of the "matched string" in str
     */
    private static Pair<Integer, Integer> indexOf(String query, String[] target, ArrayList<Pair<int[], String>> parsedRelation) {

        int start = 0;
        int end = 0;
        String section = "";
        for (int i = 0; i < parsedRelation.size(); i++) {
            end = parsedRelation.get(i).getKey()[0];
            section = query.substring(start, end);
            Pair<Integer, Integer> symbolIdx = indexOf(section, target);
            if (symbolIdx.getKey() != -1) {
                return new Pair<Integer, Integer>(symbolIdx.getKey(), symbolIdx.getValue() + start);
            }
            start = parsedRelation.get(i).getKey()[1] + 1;
        }
        end = query.length();
        section = query.substring(start, end);
        Pair<Integer, Integer> symbolIdx = indexOf(section, target);
        if (symbolIdx.getKey() != -1) {
            return new Pair<Integer, Integer>(symbolIdx.getKey(), symbolIdx.getValue() + start);
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
