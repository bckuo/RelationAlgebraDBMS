package minedbms;

import minedbms.operation.RelationOperation;
import minedbms.datatype.Relation;
import minedbms.datatype.Condition;
import minedbms.datatype.Condition.Inequality;
import java.io.FileNotFoundException;
import javafx.util.Pair;

import minedbms.ParseRAQureyConst.InequalitySymbol;
// import minedbms.ParseRAQureyConst.LogicalConjunctionSymbol;
import minedbms.ParseRAQureyConst.OptCent;
import minedbms.ParseRAQureyConst.OptLeft;

// they way to import functions: https://stackoverflow.com/questions/50217731
import static minedbms.ParseRAQureyHelper.cutParenthisis;
import static minedbms.ParseRAQureyHelper.cutSpaces;
import static minedbms.ParseRAQureyHelper.findFirstParenAfter;
import static minedbms.ParseRAQureyHelper.indexOf;
// import static minedbms.ParseRAQureyHelper.isRemain;
import static minedbms.ParseRAQureyHelper.validParenthesis;
// import static minedbms.ParseRAQureyHelper.findOuterParenPair;
import static minedbms.ParseRAQureyHelper.findParenPair;

public class ParseRAQurey {

    private MineDBMS dbms;
    
    public ParseRAQurey(MineDBMS dbms) {
        this.dbms = dbms;
    }

    /*
    0. remove redundant paranthesis
    1. check if query start with '('
        True: The query is '( subQ ) centOpt R2', R2 might be another query
            
        False:  
    */
    public Relation parse(String query) throws FileNotFoundException {
        Relation parse_result = null;
        if (query == null || !validParenthesis(query)) {
            return parse_result;
        }
        // get rid off redundant parenthesises and space
        // to make query from "   (    ( PROJ_{ANO} (SELE_{Payment > 50} Play)   )   )    " to "PROJ_{ANO} (SELE_{Payment > 50} Play)", so if there is Parenthesis at first char, it must no be a redundant one
        query = cutParenthisis(query);

        if (query.charAt(0) == '(') {  // imply there is a relation at begining of query
            //index of corresponding right parenthesis of first char, which is left parenthesis by passing "query.charAt(0) == '('", of $query
            int rightParentIdx = findParenPair(query, 0);
            int firstParenAfter = findFirstParenAfter(query, rightParentIdx); // find first '(' in $query after $rightParentIdx
            int endOfSearch = firstParenAfter==-1 ? query.length():firstParenAfter;            
            int[] optCentFound = findOptCent(query, rightParentIdx, endOfSearch);
            int optCentEndIdx = optCentFound[2];
            if (optCentEndIdx == -1) { // which mean not found optCent, should throw exception since there should be either noting after $rightParentIdx or a oprCent and relation after oprCent
                throw new IllegalArgumentException();
            } else {
                Relation leftR = parse(query.substring(0, rightParentIdx + 1));
                Relation rightR = parse(query.substring(optCentEndIdx));
                return callOptCent(optCentFound[0], leftR, rightR);
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

    /*
    find optCent in the $query after $start but before $end
    
    Return:
    type
    start index of the optCent
    end index of the optCent (the index right after last char of opeCent)
    
    e.g. {1, 29, 30} means the oprCent found is "X" and it is started at 29 and ended at index 30
    if not found all value will be -1
     */
    private int[] findOptCent(String query, int start, int end) {
        for (int optCentType = 0; optCentType < OptCent.ALL.length; optCentType++) {
            for (int qCharIdx = start; qCharIdx < end && qCharIdx < query.length(); qCharIdx++) {
                int lastChar = qCharIdx + OptCent.ALL[optCentType].length() - 1;
                if (lastChar < end && lastChar < query.length()) { // to check if the lastChar of tested char is out of bounds.
                    boolean foundOptCent = true;
                    for (int optCentIdx = 0; optCentIdx < OptCent.ALL[optCentType].length(); optCentIdx++) {
                        if (query.charAt(optCentIdx + qCharIdx) != OptCent.ALL[optCentType].charAt(optCentIdx)) {
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

    private Relation callOptCent(int optCentType, Relation leftR, Relation rightR) {
        //{UNION, INTERSECT, DIFFERENCE, CROSS_PRODUCT, NATURAL_JOIN};
        return OptCent.METHOD[optCentType].operate(leftR, rightR);
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
}
