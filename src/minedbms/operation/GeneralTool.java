package minedbms.operation;

import java.util.HashMap;

/*
This class contains several class and function that is for general usage
 */
public class GeneralTool {

    public abstract static class IntArray {

        public abstract int getLength();

        public Types getType() {
            return type;
        }

        public abstract int getInt(int idx);

        @Override
        public String toString() {
            String s = "";
            for (int i = 0; i < this.getLength(); i++) {
                s += this.getInt(i) + ", ";
            }
            s += "\n";
            return s;
        }

        public static class NormalIntArray extends IntArray {

            private int[] array;

            public NormalIntArray(int[] array) {
                this.array = array;
                super.type = IntArray.Types.NORMAL;
            }

            @Override
            public int getLength() {
                return this.array.length;
            }

            @Override
            public int getInt(int idx) {
                return this.array[idx];
            }
        }

        public static class SimplifiedIntArray extends IntArray {

            public SimplifiedIntArray(int length) {
                super.length = length;
                super.type = IntArray.Types.SIMPLIFIED;
            }

            @Override
            public int getLength() {
                return super.length;
            }

            @Override
            public int getInt(int idx) {
                return idx;
            }
        }

        public static class RepeatedIntArray extends IntArray {

            public RepeatedIntArray(int oldLength, int repeatTime, boolean shortRepeat) {
                super.length = oldLength * repeatTime;
                this.oldLength = oldLength;
                this.repeatTime = repeatTime;
                if (shortRepeat) {
                    super.type = Types.SHORT_REPEAT;
                } else {
                    super.type = Types.LONG_REPEAT;
                }
            }

            @Override
            public int getLength() {
                return super.length;
            }

            public int getOldLength() {
                return oldLength;
            }

            public int getRepeatTime() {
                return repeatTime;
            }

            private int oldLength;
            private int repeatTime;

            @Override
            public int getInt(int idx) {
                if (super.type == Types.SHORT_REPEAT) {
                    return idx / repeatTime;
                } else if (super.type == Types.LONG_REPEAT) {
                    return idx % oldLength;
                } else {
                    return -1;
                }
            }

        }

        // Normal: 1, 5, 9, 3, 2
        // Simplified: 1, 2, ... n
        // Short Repeat: 1, 1, .. 2, 2, ... n, n
        // Long Repeat: 1, 2, ... n, 1, 2, ... n, ... 1, 2, ... n
        public static enum Types {
            NORMAL, SIMPLIFIED, SHORT_REPEAT, LONG_REPEAT
        }

        private Types type;
        private int length;
    }

    public static boolean noRepeat(String[] array) {
        HashMap<String, Boolean> hm = new HashMap();
        for (int i = 0; i < array.length; i++) {
            if (hm.containsKey(array[i])) {
                return false;
            }
            hm.put(array[i], true);
        }
        return true;
    }

    public static String[] removeElement(String[] array, int index) {
        String[] newArray = new String[array.length - 1];
        for (int i = 0; i < index; i++) {
            newArray[i] = array[i];
        }
        for (int i = index + 1; i < array.length; i++) {
            newArray[i - 1] = array[i];
        }
        return newArray;
    }

    public static String[][] removeElement(String[][] array, int index) {
        String[][] newArray = new String[array.length - 1][];
        for (int i = 0; i < index; i++) {
            newArray[i] = array[i];
        }
        for (int i = index + 1; i < array.length; i++) {
            newArray[i - 1] = array[i];
        }
        return newArray;
    }  

}
