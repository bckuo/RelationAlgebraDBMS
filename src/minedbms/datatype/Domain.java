package minedbms.datatype;

import java.util.regex.Pattern;

public abstract class Domain {

    public Domain(String defaultValue, String pattern) {
        this.defaultValue = defaultValue;
        this.pObj = Pattern.compile(pattern);
        this.pattern = pattern;
    }

    public Domain(String defaultValue, String pattern, int flag) {
        this.defaultValue = defaultValue;
        this.pObj = Pattern.compile(pattern, flag);
        this.pattern = pattern;
    }

    public boolean match(String value) {
        return this.pObj.matcher(value).matches();
    }

    public String getPattern() {
        return pattern;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    private String pattern;
    private final Pattern pObj;
    private String defaultValue;
    
    public boolean equal(Domain domain){
        return this.pattern.equals(domain.pattern);
    }

    public abstract int compareValue(String v1, String v2);
    public abstract String foramtValue(String value); 
    
    // Note: This function is heavily depends on the project requirements and description
    //
    // The concpet of this function is that integer domain is more strict than decimal domain,
    // and decimal domain is more strict than string domain.
    // Hence, it will determine the domain by the order of Integer, Decimal, and String.
    //
    // null value won't affect the result
    public static Domain[] determineDomain(int num_attribute, String[][] raw_values) {
        Domain[] domainsAssigned = {IntDomain.getInstance(), DecimalDomain.getInstance(), StringDomain.getInstance()};
        Domain[] domains = new Domain[num_attribute];
        for (int i = 0; i < num_attribute; i++) {
            int type = 0;
            for (int j = 0; j < raw_values.length && type < domainsAssigned.length - 1; j++) {
                // if faild, skip test
                // i is the index of attribute and the index of the tested value in tuples[j]
                String[] tuple = raw_values[j];
                if (i < tuple.length) {
                    if (!domainsAssigned[type].match(tuple[i])) {
                        type++;
                    }
                }
            }
            domains[i] = domainsAssigned[type];
        }
        return domains;
    }

    public static class IntDomain extends Domain {

        // Singleton code from https://www.geeksforgeeks.org/singleton-class-java/
        private static IntDomain single_instance = null;

        private IntDomain() {
            // default: "0"
            // pattern: any order of digits without dot.
            super("0", "^[1-9][0-9]*$|^0$");
        }

        public static IntDomain getInstance() {
            if (single_instance == null) {
                single_instance = new IntDomain();
            }
            return single_instance;
        }

        @Override
        public int compareValue(String v1, String v2) {
            int diiference = Integer.valueOf(v1) - Integer.valueOf(v2);
            if (diiference > 0) {
                return 1;
            } else if (diiference == 0) {
                return 0;
            } else {
                return -1;
            }
        }

        @Override
        public String foramtValue(String value) {
            return String.valueOf(Integer.valueOf(value));
        }
    }
    
    public static class DecimalDomain extends Domain {

        // Singleton code from https://www.geeksforgeeks.org/singleton-class-java/
        private static DecimalDomain single_instance = null;

        private DecimalDomain() {
            // default: "0"
            // pattern: any order of digits with 1 or 0 dot. Not allow leading zeros before and trailing zeros after decimal point.
            super("0", "^(?:[1-9][0-9]*|0)(?:\\.[0-9]*[1-9])?$");
        }

        public static DecimalDomain getInstance() {
            if (single_instance == null) {
                single_instance = new DecimalDomain();
            }
            return single_instance;
        }

        @Override
        public int compareValue(String v1, String v2) {
            double diiference = Double.valueOf(v1) - Double.valueOf(v2);
            if (diiference > 0) {
                return 1;
            } else if (diiference == 0) {
                return 0;
            } else {
                return -1;
            }
        }

        @Override
        public String foramtValue(String value) {
            return String.valueOf(Double.valueOf(value));
        }
    }

    public static class StringDomain extends Domain {

        // Singleton code from https://www.geeksforgeeks.org/singleton-class-java/
        private static StringDomain single_instance = null;

        private StringDomain() {
            // default: ""
            // pattern: any string.
            super("", "^.*$");
        }

        public static StringDomain getInstance() {
            if (single_instance == null) {
                single_instance = new StringDomain();
            }
            return single_instance;
        }

        @Override
        public int compareValue(String v1, String v2) {
            return v1.compareTo(v2);
        }

        @Override
        public String foramtValue(String value) {
            return value;
        }
    }

}
