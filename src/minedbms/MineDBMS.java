package minedbms;

import minedbms.datatype.Relation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

/*
It's not allowed to have following symbols or pattern in the name of relation or attribute:
symbols: '<', '=', '>', '{', '}', '(', ')' 
pattern: "PROJ_.*{", "SELE_.*{", "U", "INTE", "-", "X", "*"

Note: this section should be revised after futher development
 */
/*
The next implemetations should be 
1. nested condition for select
2. condition the compare two relation for join
    2.1 the nested version of it
3. file name, relation name, attribute name constraint and filter methods of them
4. rename
5. aggegration function
6. grouping

*/

public class MineDBMS {

    private String defaultPath = "data/";
    private HashMap<String, Relation> relations = new HashMap<String, Relation>();
    private HashMap<String, Relation> queries = new HashMap<String, Relation>();
    private ParseRAQurey parseQ = new ParseRAQurey(this);

    public MineDBMS(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public Relation parse(String query) throws FileNotFoundException {
        Relation r = parseQ.parse(query);
        queries.put(query, r);
        return r;
    }

    // this is for project
    public static void parse(MineDBMS dbms, String query, String outputPath, boolean append)
            throws FileNotFoundException, IOException {
        System.out.println(query);
        Relation r = dbms.parse(query);
        System.out.println(r);
        MineDBMS.writeTable(r, query, outputPath, append);
    }

    public String getDefaultPath() {
        return this.defaultPath;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public void addRelation(String name, Relation r) {
        if (!this.relations.containsKey(name)) {
            this.relations.put(name, r);
        }
    }

    public void removeRelation(String name) {
        if (this.relations.containsKey(name)) {
            this.relations.remove(name);
        }
    }

    public Relation getRelation(String name) throws FileNotFoundException {
        if (this.relations.containsKey(name)) {
            return this.relations.get(name);
        } else {
            Relation r = readTable(this.defaultPath + name + ".txt");
            this.relations.put(name, r);
            return r;
        }
    }

    public void resetRelations() {
        this.relations.clear();
    }

    public void addQuery(String query, Relation r) {
        if (!this.queries.containsKey(query)) {
            this.queries.put(query, r);
        }
    }

    public void removeQuery(String query) {
        if (this.queries.containsKey(query)) {
            this.queries.remove(query);
        }
    }

    public Relation getQuery(String query) throws FileNotFoundException {
        if (this.queries.containsKey(query)) {
            return this.queries.get(query);
        } else {
            Relation r = parse(query);
            this.queries.put(query, r);
            return r;
        }
    }

    public void resetQueries() {
        this.queries.clear();
    }

    public void readQueries(MineDBMS dbms, String inputPath) throws FileNotFoundException, IOException {
        String name = getName(inputPath);
        if (name == null) {
            return;
        }

        File inputFile = new File(inputPath);
        Scanner input = new Scanner(inputFile);

        String line;
        while (input.hasNextLine()) {
            line = input.nextLine();
            if (validQuery(line)) {
                Relation r = parse(line);
                this.queries.put(line, r);
            }
        }
        input.close();
    }

    // this is for project
    // normaly please use another readQueries, and retrieve data from getQuery
    public static void readQueries(MineDBMS dbms, String inputPath, String outputPath)
            throws FileNotFoundException, IOException {
        String name = getName(inputPath);
        if (name == null) {
            return;
        }

        File inputFile = new File(inputPath);
        Scanner input = new Scanner(inputFile);

        String line;
        if (input.hasNextLine()) {
            line = input.nextLine();
            if (validQuery(line)) {
                parse(dbms, line, outputPath, false);
            }
        }
        while (input.hasNextLine()) {
            line = input.nextLine();
            if (validQuery(line)) {
                parse(dbms, line, outputPath, true);
            }
        }
        input.close();
    }

    public static Relation readTable(String filePath) throws FileNotFoundException {
        String name = getName(filePath);
        if (name == null) {
            return null;
        }
        File inputFile = new File(filePath);
        Scanner input = new Scanner(inputFile);
        String inputString = "";
        while (input.hasNextLine()) {
            inputString += input.nextLine() + "\n";
        }
        input.close();

        final String delimiter = ", ";
        String[] data = inputString.split("\n");
        String[] attrs = data[0].split(delimiter);
        String[][] rawValues = new String[data.length - 1][attrs.length];

        for (int i = 0; i < rawValues.length; i++) {
            String[] tuple = data[i + 1].split(delimiter);
            for (int j = 0; j < attrs.length; j++) {
                if (j < attrs.length) {
                    rawValues[i][j] = tuple[j];
                }
            }
        }
        Relation r = new Relation(name, attrs, rawValues);
        return r;
    }

    private static String getName(String filePath) {

        final char SLASH_CHAR = '/';
        final char DOT_CHAR = '.';
        final String FILE_FORMAT = "txt";
        int slashIdx = -1; // index of slash, should be the last one after the for loop
        int dotIdx = -1; // index of dot, should be the last one after the for loop
        for (int i = 0; i < filePath.length(); i++) {
            if (filePath.charAt(i) == SLASH_CHAR) {
                slashIdx = i;
            }
            if (filePath.charAt(i) == DOT_CHAR) {
                dotIdx = i;
            }
        }

        if (slashIdx == -1 || dotIdx == -1 || slashIdx >= dotIdx
                || filePath.substring(dotIdx + 1).compareTo(FILE_FORMAT) != 0) {
            return null;
        }

        return filePath.substring(slashIdx + 1, dotIdx);
    }

    // https://www.baeldung.com/java-write-to-file
    public static void writeTable(Relation r, String preceeding, String filename, boolean append) throws IOException {
        if (filename == null) {
            filename = r.name;
        }
        FileOutputStream fos = new FileOutputStream(new File(filename), append);
        PrintWriter printWriter = new PrintWriter(fos);
        if (append) {
            printWriter.print("\n");
        }
        printWriter.print(preceeding + "\n");
        for (int i = 0; i < r.getNum_attribute() - 1; i++) {
            printWriter.print(r.getAttribute(i) + ", ");
        }
        printWriter.print(r.getAttribute(r.getNum_attribute() - 1) + "\n");
        for (int i = 0; i < r.getNum_tuple() - 1; i++) {
            for (int j = 0; j < r.getNum_attribute() - 1; j++) {
                printWriter.print(r.getTuples()[i][j] + ", ");
            }
            printWriter.print(r.getTuples()[i][r.getNum_attribute() - 1] + "\n");
        }
        for (int j = 0; j < r.getNum_attribute() - 1; j++) {
            printWriter.print(r.getTuples()[r.getNum_tuple() - 1][j] + ", ");
        }
        printWriter.print(r.getTuples()[r.getNum_tuple() - 1][r.getNum_attribute() - 1]);

        printWriter.close();
    }

    // only do a basic test now
    // test if the query is empty or only spaces
    private static boolean validQuery(String query) {
        for (int i = 0; i < query.length(); i++) {
            if (query.charAt(i) != ' ') {
                return true;
            }
        }
        return false;
    }

}
