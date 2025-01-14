package minedbms.datatype;

public class Attribute {

    public String name;
    private Domain domain;
    private boolean nullable;

    public Attribute(String name, Domain domain, boolean nullable) {
        this.name = name;
        this.domain = domain;
        this.nullable = nullable;
    }

    public Attribute(Attribute attr) {
        this.name = attr.name;
        this.domain = attr.domain;
        this.nullable = attr.nullable;
    }

    public boolean match(String value) {
        if (value == null) {
            return this.nullable;
        }
        return this.domain.match(value);
    }

    public int compare(String v1, String v2) {
        if (nullable) {
            if (v1 == null && v2 == null) {
                return 0;
            } else if (v1 == null) {
                return -1;
            } else if (v2 == null) {
                return 1;
            }
        }
        return this.domain.compareValue(v1, v2);
    }

    public String getDefaultValue() {
        return this.domain.getDefaultValue();
    }

    public String getPattern() {
        return this.domain.getPattern();
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public boolean isNullable() {
        return nullable;
    }
    
    public boolean sameDomain(Attribute attribute){
        return this.domain.equal(attribute.domain);
    }
    
    public String foramtValue(String value){
        return this.domain.foramtValue(value);
    }  
    
    // idea from here: https://stackoverflow.com/questions/8180430/
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Attribute == false) {
            return false;
        }        
        final Attribute other = (Attribute) obj;
        return this.sameDomain(other) && this.name.equals(other.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
