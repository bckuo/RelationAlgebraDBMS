package minedbms.datatype;

import static minedbms.operation.GeneralTool.removeElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
/*
It might be confusion that the first index of tuples is row of it, second is column
However, the first index of attributes is the column
Also, the constructor take number of attributes before number of tuples, which are column and row of Relation table
*/
public class Relation {
    
    public String name;
    private int num_attributes;
    private int num_tuples;
    private Attribute[] attributes;
    private final HashMap<String, Integer> attrIdxTable;
    private String[][] tuples;
    private final HashMap<List<String>, Integer> tuplesIdxTable;
    
    private boolean attrSet;

    public Relation(String name, String[] attributes, String[][] raw_values) {
        this.attrSet = false;
        this.name = name;
        this.num_attributes = attributes.length;
        this.num_tuples = raw_values.length;
        this.attributes = new Attribute[num_attributes];
        this.attrIdxTable = new HashMap();
        this.tuples = new String[num_tuples][num_attributes];
        this.tuplesIdxTable = new HashMap();

        setAttributes(attributes, raw_values);
        setTuples(this.num_attributes, raw_values);        
        setAttrIdxTable();
    }

    public Relation(String name, int num_attributes, int num_tuples) {
        this.name = name;
        this.num_attributes = num_attributes;
        this.num_tuples = num_tuples;
        this.attributes = new Attribute[num_attributes];
        this.attrIdxTable = new HashMap();
        this.tuples = new String[num_tuples][num_attributes];
        this.tuplesIdxTable = new HashMap();
    }
    

    // getter and setter
    public int getNum_attribute() { return num_attributes; }
    public int getNum_tuple() { return num_tuples; }
    public Attribute[] getAttributes() { return attributes; }
    public Attribute getAttribute(int idx) { return attributes[idx]; }  
    public Attribute getAttribute(String attr_name) { return attributes[getAttrIdx(attr_name)]; }  
    public int getAttrIdx(String attrName) {
        if (this.attrIdxTable.isEmpty()) {
            try {
                this.setAttrIdxTable();                
            } catch (IllegalArgumentException e) {      
                System.out.println(e.getMessage());
                return -1;
            }
        } 
        if (!this.attrIdxTable.containsKey(attrName)) {
            return -1;
        }
        return this.attrIdxTable.get(attrName);
    } 
    public String[][] getTuples() { return tuples; }
    public String[] getTuple(int idx) { return tuples[idx]; }
    public int getTupleIdx(String[] tuple) {
        if (this.tuplesIdxTable.isEmpty()) {
            this.setTupleIdxTable();    
        } 
        if (!this.tuplesIdxTable.containsKey(Arrays.asList(tuple))) {
            return -1;
        }
        return this.tuplesIdxTable.get(Arrays.asList(tuple));
    } 
   
    // No null value allowed
    public boolean setTuples(int num_attributes, String[][] raw_values) {
        this.tuplesIdxTable.clear();
        // attribute need to be set first 
        if (!this.attrSet) {            
            throw new RuntimeException("Error: The Attributes need to be set before setting Tuples.");
        }
        int repeatCount = 0;
        for (int i = 0; i < raw_values.length; i++) {
            String[] tuple = raw_values[i];
            for (int j = 0; j < tuple.length; j++) {
                this.tuples[i-repeatCount][j] = this.getAttribute(j).foramtValue(tuple[j]);
            }
            for (int j = tuple.length; j < num_attributes; j++) {
                Attribute attr = this.attributes[i];
                if (attr.isNullable()) {
                    this.tuples[i-repeatCount][j] = null;
                } else {
                    this.tuples[i-repeatCount][j] = this.attributes[j].getDefaultValue();
                }
            }
            // idea from this: https://stackoverflow.com/questions/16839182/
            if (tuplesIdxTable.containsKey(Collections.unmodifiableList(Arrays.asList(this.tuples[i])))){
                this.tuples = removeElement(tuples, i);
                this.num_tuples--;
                repeatCount++;
            } else {
                this.tuplesIdxTable.put(Collections.unmodifiableList(Arrays.asList(this.tuples[i])),  i);                
            }
        }
        return true;
    }

    public void setAttributes(String[] attrs, String[][] raw_values) {
        Domain[] domains = Domain.determineDomain(attrs.length, raw_values);
        for (int i = 0; i < attrs.length; i++) {
            // use string domain as default
            this.attributes[i] = new Attribute(attrs[i], domains[i], false);
        }
        this.attrSet = true;
    }
    
    // this will only need to be call once for every Relation object, since the attribute array is final array and won't be modified
    public void setAttrIdxTable() {
        this.attrIdxTable.clear();
        for (int i = 0; i < this.num_attributes; i++) {
            if (this.attrIdxTable.containsKey(this.attributes[i].name)) {
                this.attrIdxTable.clear();                
                throw new IllegalArgumentException("Error: The names of Attributes are repeated.");
            }
            this.attrIdxTable.put(this.attributes[i].name, i);
        }
    }
    // this will only need to be call once for every Relation object, since the attribute array is final array and won't be modified
    public void setTupleIdxTable() {
        this.tuplesIdxTable.clear();
        for (int i = 0; i < this.num_tuples; i++) {
            if (tuplesIdxTable.containsKey(Collections.unmodifiableList(Arrays.asList(this.tuples[i])))){
                this.num_tuples--;
                i--;
                this.tuples = removeElement(tuples, i);
            } else {
                this.tuplesIdxTable.put(Collections.unmodifiableList(Arrays.asList(this.tuples[i])),  i);                
            }
        }
    }


    @Override
    public String toString() {
        String table = "" + name + ":\n";
        for (int i = 0; i < num_attributes - 1; i++) {
            table += attributes[i] + ", ";
        }
        table += attributes[num_attributes - 1] + "\n";
        for (int i = 0; i < num_tuples; i++) {
            for (int j = 0; j < tuples[i].length - 1; j++) {
                table += tuples[i][j] + ", ";
            }
            table += tuples[i][tuples[i].length - 1] + "\n";
        }
        return table;
    }
}
