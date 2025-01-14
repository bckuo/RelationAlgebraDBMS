package minedbms.operation;

import minedbms.datatype.Attribute;
import minedbms.datatype.Relation;
import minedbms.datatype.Condition;
import static minedbms.operation.GeneralTool.IntArray;
import static minedbms.operation.GeneralTool.noRepeat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RelationOperation {

    // order of attrs matter the result
    public static Relation project(Relation r, String... attrs) {
        int[] attrsKept = new int[attrs.length];
        for (int i = 0; i < attrs.length; i++) {
            attrsKept[i] = r.getAttrIdx(attrs[i]);
        }
        if (!noRepeat(attrs)) {
            throw new IllegalArgumentException("Error: There is a repeat in the giving attributes.");
        }

        IntArray pickedAttrs = new IntArray.NormalIntArray(attrsKept);
        IntArray pickedTuples = new IntArray.SimplifiedIntArray(r.getNum_tuple());

        Relation projR = copyData(r, pickedAttrs, pickedTuples);

        return projR;
    }

    public static Relation select(Relation r, Condition cond) {

        int[] tuplesKept = new int[r.getNum_tuple()];
        int count = 0;
        for (int tupleIdx = 0; tupleIdx < r.getNum_tuple(); tupleIdx++) {
            if (cond.testCondition(r, tupleIdx)) {
                tuplesKept[count] = tupleIdx;
                count++;
            }
        }

        int[] tuplesKeptCut = new int[count];

        System.arraycopy(tuplesKept, 0, tuplesKeptCut, 0, count);

        IntArray pickedAttrs = new IntArray.SimplifiedIntArray(r.getNum_attribute());
        IntArray pickedTuples = new IntArray.NormalIntArray(tuplesKeptCut);

        Relation seleR = copyData(r, pickedAttrs, pickedTuples);

        return seleR;
    }

    public static Relation union(Relation r1, Relation r2) {
        int[] r2MapR1 = relationMap(r2, r1);

        int[] r2TupleKept = new int[r2.getNum_tuple()];
        int count = 0;
        for (int i = 0; i < r2.getNum_tuple(); i++) {
            if (r2MapR1[i] == -1) { // find the one not in r1, which mean no maping
                r2TupleKept[count] = i;
                count++;
            }
        }

        int[] r2TuplesKeptCut = new int[count];

        System.arraycopy(r2TupleKept, 0, r2TuplesKeptCut, 0, count);

        IntArray pickedAttrs = new IntArray.SimplifiedIntArray(r1.getNum_attribute());
        IntArray pickedTuplesR1 = new IntArray.SimplifiedIntArray(r1.getNum_tuple());
        IntArray pickedTuplesR2 = new IntArray.NormalIntArray(r2TuplesKeptCut);

        Relation unioR = copyDataVertc(r1, r2, pickedAttrs, pickedTuplesR1, pickedTuplesR2);

        return unioR;
    }

    public static Relation intersect(Relation r1, Relation r2) {
        int[] r1MapR2 = relationMap(r1, r2);

        int[] tuplesKept = new int[r1.getNum_tuple()];
        int count = 0;
        for (int i = 0; i < r1.getNum_tuple(); i++) {
            if (r1MapR2[i] != -1) { // tuple i HAS a map to r2
                tuplesKept[count] = i;
                count++;
            }
        }

        int[] tuplesKeptCut = new int[count];

        System.arraycopy(tuplesKept, 0, tuplesKeptCut, 0, count);

        IntArray pickedAttrs = new IntArray.SimplifiedIntArray(r1.getNum_attribute());
        IntArray pickedTuples = new IntArray.NormalIntArray(tuplesKeptCut);

        Relation inteR = copyData(r1, pickedAttrs, pickedTuples);

        return inteR;
    }

    public static Relation difference(Relation r1, Relation r2) {
        int[] r1MapR2 = relationMap(r1, r2);

        int[] tuplesKept = new int[r1.getNum_tuple()];
        int count = 0;
        for (int i = 0; i < r1.getNum_tuple(); i++) {
            if (r1MapR2[i] == -1) { // tuple i DOES NOT HAS a map to r2
                tuplesKept[count] = i;
                count++;
            }
        }

        int[] tuplesKeptCut = new int[count];

        System.arraycopy(tuplesKept, 0, tuplesKeptCut, 0, count);

        IntArray pickedAttrs = new IntArray.SimplifiedIntArray(r1.getNum_attribute());
        IntArray pickedTuples = new IntArray.NormalIntArray(tuplesKeptCut);

        Relation diffR = copyData(r1, pickedAttrs, pickedTuples);

        return diffR;
    }

    // for the name conflict on attributes, I will follow this logic to solve: https://stackoverflow.com/questions/21647379/
    public static Relation crossProduct(Relation r1, Relation r2) {
        // resolve the name conflict
        String[] attributesNames = new String[r1.getNum_attribute()];
        for (int i = 0; i < r1.getNum_attribute(); i++) {
            int r2Idx = r2.getAttrIdx(r1.getAttribute(i).name);
            if (r2Idx != -1) {
                attributesNames[i] = r1.getAttribute(i).name;
                if (r1.name.equals(r2.name)) {
                    r1.getAttribute(i).name = r1.name + "." + attributesNames[i] + ".1";
                    r2.getAttribute(r2Idx).name = r2.name + "." + attributesNames[i] + ".2";
                } else {
                    r1.getAttribute(i).name = r1.name + "." + attributesNames[i];
                    r2.getAttribute(r2Idx).name = r2.name + "." + attributesNames[i];
                }
            }
        }
        // At this print, their name should be changed as expected
        // System.out.println(r1);
        // System.out.println(r2);

        IntArray pickedAttrsR1 = new IntArray.SimplifiedIntArray(r1.getNum_attribute());
        IntArray pickedAttrsR2 = new IntArray.SimplifiedIntArray(r2.getNum_attribute());
        IntArray pickedTuplesR1 = new IntArray.RepeatedIntArray(r1.getNum_tuple(), r2.getNum_tuple(), true);
        IntArray pickedTuplesR2 = new IntArray.RepeatedIntArray(r2.getNum_tuple(), r1.getNum_tuple(), false);

        Relation corsProdR = copyDataHoriz(r1, r2, pickedAttrsR1, pickedAttrsR2, pickedTuplesR1, pickedTuplesR2);

        // restore their attibute names back
        for (int i = 0; i < attributesNames.length; i++) {
            if (attributesNames[i] != null && !attributesNames[i].equals("")) {
                r1.getAttribute(i).name = attributesNames[i];
                r2.getAttribute(r2.getAttrIdx(attributesNames[i])).name = attributesNames[i];
            }
        }

        return corsProdR;
    }

    // for the name conflict on attributes with different domain, I will follow this logic to solve: https://stackoverflow.com/questions/21647379/    
    // for duplicate match, this method will be applied: https://stackoverflow.com/questions/43959462/
    // for multiple shared attributes, two tuple have to have same corresponding value in each shared attributes to join.
    // Since there are only three kind of domains, match attribute name is much more inportant.
    // If no match name with domain also match, than the join will not be process.
    public static Relation naturalJoin(Relation r1, Relation r2) {
        int[] attrsMatchR1 = findSameAttribute(r1, r2);
        int[] attrsMatchR2 = new int[attrsMatchR1.length];
        // record mappped r2 attribute's indexs
        for (int i = 0; i < attrsMatchR2.length; i++) {
            attrsMatchR2[i] = r2.getAttrIdx(r1.getAttribute(attrsMatchR1[i]).name);
        }

        // resolve the name conflict
        String[] attributesNames = new String[r1.getNum_attribute()];
        for (int i = 0; i < r1.getNum_attribute(); i++) {
            int r2Idx = r2.getAttrIdx(r1.getAttribute(i).name);
            if (r2Idx != -1) {
                if (!r1.getAttribute(i).equals(r2.getAttribute(r2Idx))) {
                    attributesNames[i] = r1.getAttribute(i).name;
                    if (r1.name.equals(r2.name)) {
                        r1.getAttribute(i).name = r1.name + "." + attributesNames[i] + ".1";
                        r2.getAttribute(r2Idx).name = r2.name + "." + attributesNames[i] + ".2";
                    } else {
                        r1.getAttribute(i).name = r1.name + "." + attributesNames[i];
                        r2.getAttribute(r2Idx).name = r2.name + "." + attributesNames[i];
                    }
                }
            }
        }

        // classify the r2 tuple by the shared attribute
        HashMap<List<String>, ArrayList<Integer>> r2Classification = new HashMap<>();
        for (int i = 0; i < r2.getNum_tuple(); i++) {
            String[] key = getKey(r2, attrsMatchR2, i);
            if (!r2Classification.containsKey(Collections.unmodifiableList(Arrays.asList(key)))) {
                r2Classification.put(Collections.unmodifiableList(Arrays.asList(key)), new ArrayList<>());
            }
            r2Classification.get(Collections.unmodifiableList(Arrays.asList(key))).add(i);
        }
        // record the matchs tuple pairs
        int[] r1TuplesKept = new int[r1.getNum_tuple() * r2.getNum_tuple()];
        int[] r2TuplesKept = new int[r1.getNum_tuple() * r2.getNum_tuple()];
        int count = 0;
        for (int i = 0; i < r1.getNum_tuple(); i++) {
            String[] key = getKey(r1, attrsMatchR1, i);
            if (r2Classification.containsKey(Collections.unmodifiableList(Arrays.asList(key)))) {
                ArrayList<Integer> al = r2Classification.get(Collections.unmodifiableList(Arrays.asList(key)));
                for (int j = 0; j < al.size(); j++) {
                    r1TuplesKept[count] = i;
                    r2TuplesKept[count] = al.get(j);
                    count++;
                }
            }
        }
        // transfer to the proper length, or you could say cut off the redundant
        int[] r1TuplesKeptCut = new int[count];
        int[] r2TuplesKeptCut = new int[count];
        System.arraycopy(r1TuplesKept, 0, r1TuplesKeptCut, 0, count);
        System.arraycopy(r2TuplesKept, 0, r2TuplesKeptCut, 0, count);

        // purge the same attribute
        int[] attrR2 = new int[r2.getNum_attribute() - attrsMatchR2.length];
        count = 0;
        boolean match;
        for (int i = 0; i < r2.getNum_attribute(); i++) {
            match = false;
            for (int j = 0; j < attrsMatchR2.length; j++) {
                if (i == attrsMatchR2[j]) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                attrR2[count++] = i;
            }
        }

        // set up IntArray
        IntArray pickedAttrsR1 = new IntArray.SimplifiedIntArray(r1.getNum_attribute());
        IntArray pickedAttrsR2 = new IntArray.NormalIntArray(attrR2);
        IntArray pickedTuplesR1 = new IntArray.NormalIntArray(r1TuplesKeptCut);
        IntArray pickedTuplesR2 = new IntArray.NormalIntArray(r2TuplesKeptCut);

        // call the copy function to copy data
        Relation natuJoinR = copyDataHoriz(r1, r2, pickedAttrsR1, pickedAttrsR2, pickedTuplesR1, pickedTuplesR2);

        // restore their attibute names back
        for (int i = 0; i < attributesNames.length; i++) {
            if (attributesNames[i] != null && !attributesNames[i].equals("")) {
                r1.getAttribute(i).name = attributesNames[i];
                r2.getAttribute(r2.getAttrIdx(attributesNames[i])).name = attributesNames[i];
            }
        }

        return natuJoinR;
    }

    private static boolean allSameDomain(Relation r1, Relation r2) {
        if (r1.getNum_attribute() != r2.getNum_attribute()) {
            throw new IllegalArgumentException("Error: Two Relations should at least have same number of attributes.");
        }
        for (int i = 0; i < r1.getNum_attribute(); i++) {
            if (!r1.getAttribute(i).sameDomain(r2.getAttribute(i))) {
                throw new IllegalArgumentException(String.format("Error: Two Relations's attributes don't match at attribute with index %d.", i));
            }
        }
        return true;
    }

    // this is for UNION, INTERSECT and DIFFERENCE
    private static int[] relationMap(Relation r1, Relation r2) {
        if (allSameDomain(r1, r2)) {
            // after the test above, two relations' all attributes should has same domain correspondingly
            int[] r1MapR2 = new int[r1.getNum_tuple()];
            for (int i = 0; i < r1.getNum_tuple(); i++) {
                r1MapR2[i] = r2.getTupleIdx(r1.getTuple(i));
            }

            return r1MapR2;
        }
        return null;
    }

    // Since there are only three kind of domains, match attribute name is much more inportant.
    private static int[] findSameAttribute(Relation r1, Relation r2) {
        int[] validAttrs = new int[r1.getNum_attribute()];
        int count = 0;
        for (int i = 0; i < r1.getNum_attribute(); i++) {
            int r2AttrIdx = r2.getAttrIdx(r1.getAttribute(i).name);
            if (r2AttrIdx != -1 && r1.getAttribute(i).equals(r2.getAttribute(r2AttrIdx))) {
                validAttrs[count] = i;
                count++;
            }
        }
        int[] attrsMatch = new int[count];
        System.arraycopy(validAttrs, 0, attrsMatch, 0, count);

        return attrsMatch;
    }

    private static String[] getKey(Relation r, int[] matched, int index) {
        String[] key = new String[matched.length];
        for (int j = 0; j < matched.length; j++) {
            key[j] = r.getTuples()[index][matched[j]];
        }
        return key;
    }

    // There is no reason both arrays are simplified
    private static Relation copyData(Relation copiedR, IntArray pickedAttrs, IntArray pickedTuples) {
        if (pickedAttrs.getType() == IntArray.Types.SIMPLIFIED && pickedTuples.getType() == IntArray.Types.SIMPLIFIED) {
            throw new IllegalArgumentException("Error: Two IntArrays should not be simplified at sametime.");
        }
        Relation newR = new Relation(copiedR.name, pickedAttrs.getLength(), pickedTuples.getLength());

        int attrOffset, tupleOffset;

        tupleOffset = 0;
        attrOffset = 0;
        copyAttr(newR, copiedR, pickedAttrs, attrOffset);
        copyTuple(newR, copiedR, pickedAttrs, pickedTuples, tupleOffset, attrOffset);

        newR.setAttrIdxTable();
        newR.setTupleIdxTable();
        return newR;
    }

    // order matters. R1 will be the upper, R2 will be the lower
    // the name will simly use r1's name
    private static Relation copyDataVertc(Relation copiedR1, Relation copiedR2,
            IntArray pickedAttrs, /* R1 and R2 should have same attributes*/
            IntArray pickedTuplesR1, IntArray pickedTuplesR2) {

        allSameDomain(copiedR1, copiedR2);
        Relation newR = new Relation(copiedR1.name, pickedAttrs.getLength(), pickedTuplesR1.getLength() + pickedTuplesR2.getLength());

        int attrOffset, tupleOffset;

        tupleOffset = 0;
        attrOffset = 0;
        copyAttr(newR, copiedR1, pickedAttrs, attrOffset);
        copyTuple(newR, copiedR1, pickedAttrs, pickedTuplesR1, tupleOffset, attrOffset);

        tupleOffset = pickedTuplesR1.getLength();
        attrOffset = 0;
        copyTuple(newR, copiedR2, pickedAttrs, pickedTuplesR2, tupleOffset, attrOffset);

        newR.setAttrIdxTable();
        newR.setTupleIdxTable();
        return newR;
    }

    // order matters. R1 will be the left, R2 will be the right
    // the name will simly use r1's name
    // Two tuple array should be the same length.
    private static Relation copyDataHoriz(Relation copiedR1, Relation copiedR2,
            IntArray pickedAttrsR1, IntArray pickedAttrsR2,
            IntArray pickedTuplesR1, IntArray pickedTuplesR2) {

        if (pickedTuplesR1.getLength() != pickedTuplesR2.getLength()) {
            throw new IllegalArgumentException("Error: Two tuple array should be the same length.");
        }
        Relation newR = new Relation(copiedR1.name, pickedAttrsR1.getLength() + pickedAttrsR2.getLength(), pickedTuplesR1.getLength());

        int attrOffset, tupleOffset;

        tupleOffset = 0;
        attrOffset = 0;
        copyAttr(newR, copiedR1, pickedAttrsR1, attrOffset);
        copyTuple(newR, copiedR1, pickedAttrsR1, pickedTuplesR1, tupleOffset, attrOffset);

        tupleOffset = 0;
        attrOffset = pickedAttrsR1.getLength();
        copyAttr(newR, copiedR2, pickedAttrsR2, attrOffset);
        copyTuple(newR, copiedR2, pickedAttrsR2, pickedTuplesR2, tupleOffset, attrOffset);

        newR.setAttrIdxTable();
        newR.setTupleIdxTable();
        return newR;
    }

    private static void copyAttr(Relation newR, Relation copiedR,
            IntArray pickedAttrs, int attrOffset) {

        for (int attrIdx = 0; attrIdx < pickedAttrs.getLength(); attrIdx++) {
            int aIdx = pickedAttrs.getInt(attrIdx);
            newR.getAttributes()[attrOffset + attrIdx] = new Attribute(copiedR.getAttributes()[aIdx]);
        }
    }

    private static void copyTuple(Relation newR, Relation copiedR,
            IntArray pickedAttrs, IntArray pickedTuples, int tupleOffset, int attrOffset) {
        for (int tupleIdx = 0; tupleIdx < pickedTuples.getLength(); tupleIdx++) {
            int tIdx = pickedTuples.getInt(tupleIdx);
            for (int attrIdx = 0; attrIdx < pickedAttrs.getLength(); attrIdx++) {
                int aIdx = pickedAttrs.getInt(attrIdx);
                newR.getTuples()[tupleOffset + tupleIdx][attrOffset + attrIdx] = copiedR.getTuples()[tIdx][aIdx];
            }
        }
    }
}
