package minedbms;

import javafx.util.Pair;

class ParseRAQureyHelper {
    

    static boolean isRemain(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ' ') {
                return true;
            }
        }
        return false;
    }

    static int findFirstParenAfter(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '(') {
                return i;
            }
        }
        return -1;
    }

    // remove the leading and trailing spaces 
    // e.g. '     asd asdasd asd        ' -> 'asd asdasd asd' 
    @SuppressWarnings("empty-statement")
    static String cutSpaces(String str) {
        int start = 0, end = str.length();
        // find the first index that is not ' ', else it will be the last index
        for (; start < str.length() && str.charAt(start) == ' '; start++); // stop at str.charAt(start) != ' '
        // find the last index that is not ' ', else it will be the last index
        for (; end > start && str.charAt(end-1) == ' '; end--); // stop at str.charAt(end-1) != ' '
        return str.substring(start, end);
    }

    static String cutParenthisis(String str) {
        String cut = cutSpaces(str);
        int[] index = findOuterParenPair(cut, 0);
        while (index[0] == 0 && index[1] == cut.length() - 1) {
            cut = cutSpaces(cut.substring(index[0] + 1, index[1]));
            index = findOuterParenPair(cut, 0);
        }
        return cut;
    }

    /*
    return: 
    First int: the index of the "matched string" in target
    Second int: the index of the "matched string" in str
     */
    static Pair<Integer, Integer> indexOf(String str, String[] target) {
        for (int i = 0; i < target.length; i++) {
            int idx = str.indexOf(target[i]);
            if (idx != -1) {
                return new Pair<Integer, Integer>(i, idx);
            }
        }
        return new Pair<Integer, Integer>(-1, null);
    }

    // From the start of str, found a paired parenthesis and return the index of them.
    //
    // If 
    // 1. there is no parenthesis, 
    // 2. no matched right parenthesis for the first left parenthesis, 
    // return [-1, -1]
    @SuppressWarnings("empty-statement")
    static int[] findOuterParenPair(String str, int startIdx) {
        int[] parenthesisIdx = {-1, -1};
        if (startIdx >= str.length()) {
            return parenthesisIdx;
        }
        
        //// Find the index of left paranthesis
        //
        // the loop stop at either leftIndex = str.length()-1 or str.charAt(leftIndex) == ParseRAQureyConst
        // which mean leftIndex is the last index of string or the first left paranthesis
        int leftIndex = startIdx;
        for (; leftIndex < str.length() && str.charAt(leftIndex) != ParseRAQureyConst.PARENTHISIS[0]; leftIndex++);
        
        //// Find the right one
        //
        if (leftIndex != str.length()-1) {
            int rightIndex = findParenPair(str, leftIndex);
            if (rightIndex != -1){
                parenthesisIdx[0] = leftIndex;
                parenthesisIdx[1] = rightIndex;                
            }
        }
        
        return parenthesisIdx;
    }
    
    // the char at $leftParenIdx in $str should be '(' 
    static int findParenPair(String str, int leftParenIdx) {
        if (leftParenIdx >= str.length() - 1 || str.charAt(leftParenIdx) != '(') {
            return -1;
        }
        int count = 0;
        for (int i = leftParenIdx + 1; i < str.length(); i++) {
            if (str.charAt(i) == ParseRAQureyConst.PARENTHISIS[0]) {
                count++;
            } else if (str.charAt(i) == ParseRAQureyConst.PARENTHISIS[1]) {
                if (count == 0) {
                    return i;
                }
                count--;
            }
        }
        return -1;
    }

    static boolean validParenthesis(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ParseRAQureyConst.PARENTHISIS[0]) {
                count++;
            } else if (str.charAt(i) == ParseRAQureyConst.PARENTHISIS[1]) {
                count--;
            }
            if (count < 0) {
                return false;
            }
        }
        return count == 0;
    }
    
    static public class optCentIdxes{
    } 
}
