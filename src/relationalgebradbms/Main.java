package relationalgebradbms;

import minedbms.operation.RelationOperation;
import minedbms.MineDBMS;
import minedbms.datatype.Relation;
import minedbms.datatype.Condition;
import minedbms.datatype.Condition.Inequality;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException, InterruptedException, IOException {
        testParsings();
    }
    
    // this is for project, for getting 
    public static void outputQueriesFromFile() throws FileNotFoundException, IOException {              
        MineDBMS dbms = new MineDBMS("data/");
        MineDBMS.readQueries(dbms, "data/RAqueries.txt", "data/RAoutput.csv");
    }

    // this is for project, for getting 
    public static void testReadQueries() throws FileNotFoundException, IOException {      
        MineDBMS dbms = new MineDBMS("data/");
        dbms.readQueries(dbms, "data/RAqueries.txt");
        Relation r = dbms.getQuery("SELE_{Payment > 80} Play");
        System.out.println(r);
    }

    public static void testParsingAndOutput() throws FileNotFoundException, IOException {
        MineDBMS dbms = new MineDBMS("data/");
        // folowing queries are from "data/RAqueries.txt"
        MineDBMS.parse(dbms, "SELE_{Payment > 80} Play", "data/RAoutput.csv", false);
        MineDBMS.parse(dbms, "PROJ_{ANO, MNO} (Play)", "data/RAoutput.csv", true);
        MineDBMS.parse(dbms, "ACTORS * Play", "data/RAoutput.csv", true);
        MineDBMS.parse(dbms, "PROJ_{ANO} (ACTORS * Play)", "data/RAoutput.csv", true);
        MineDBMS.parse(dbms, "(PROJ_{ANO} (SELE_{Payment > 80} Play)) - (PROJ_{ANO} (SELE_{Payment < 70} Play))", "data/RAoutput.csv", true);
        MineDBMS.parse(dbms, "(PROJ_{ANO} (SELE_{Payment > 80} Play)) U (PROJ_{ANO} (SELE_{Payment < 70} Play))", "data/RAoutput.csv", true);
    }
    
    public static void testParsings() throws FileNotFoundException{
        MineDBMS dbms = new MineDBMS("data/");       
        
        parse(dbms, "SELE_{Payment > 80} Play");
        parse(dbms, "PROJ_{ANO, MNO} (Play)");
        parse(dbms, "ACTORS * Play");
        parse(dbms, "ACTORS*Play");
        parse(dbms, "(ACTORS)*Play");
        parse(dbms, "ACTORS*(Play)");
        parse(dbms, "PROJ_{ANO} (ACTORS * Play)");
        parse(dbms, "(PROJ_{ANO} (SELE_{Payment > 80} Play)) - (PROJ_{ANO} (SELE_{Payment < 70} Play))");
        parse(dbms, "(PROJ_{ANO} (SELE_{Payment > 80} Play)) U (PROJ_{ANO} (SELE_{Payment < 70} Play))");
        parse(dbms, "(PROJ_{ANO} (SELE_{Payment > 80} Play)) U PROJ_{ANO} SELE_{Payment < 70} Play");
        parse(dbms, "(PROJ_{ANO} (SELE_{Payment > 80} Play)) UPROJ_{ANO} (SELE_{Payment < 70} Play)");
        parse(dbms, "PlayUPlayUPlayUPlay");
    }
       
    public static void parse(MineDBMS dbms, String query) throws FileNotFoundException{        
        System.out.println(query);
        System.out.println(dbms.parse(query));
    }

    // a test without useing parsing
    public static void testWithoutParse() throws FileNotFoundException {

        Relation actors = MineDBMS.readTable("data/ACTORS.txt");
        Relation actors2 = MineDBMS.readTable("data/ACTORS2.txt");
        Relation movies = MineDBMS.readTable("data/MOVIES.txt");
        Relation play = MineDBMS.readTable("data/Play.txt");

        System.out.println("Print all tables:");
        System.out.println(actors.toString() + "\n" + actors2.toString() + "\n" + movies.toString() + "\n" + play.toString());

        System.out.println("project(actors, \"ANAME\"):");
        System.out.println(RelationOperation.project(actors, "ANAME"));

        System.out.println("union(actors, actors2):");
        System.out.println(RelationOperation.union(actors, actors2));

        System.out.println("intersect(actors, actors2)");
        System.out.println(RelationOperation.intersect(actors, actors2));

        System.out.println("difference(actors, actors2)");
        System.out.println(RelationOperation.difference(actors, actors2));

        Condition cond = new Inequality.GreaterThan("Payment", "80");
        System.out.println("select(play, cond)");
        System.out.println(RelationOperation.select(play, cond));

        System.out.println("crossProduct(actors, movies)");
        System.out.println(RelationOperation.crossProduct(actors, movies));

        System.out.println("crossProduct(actors, play)");
        System.out.println(RelationOperation.crossProduct(actors, play));

        System.out.println("Print all related tables: (make sure name is back)");
        System.out.println(actors);
        System.out.println(play);

        System.out.println("naturalJoin(actors, play)");
        System.out.println(RelationOperation.naturalJoin(actors, play));
        System.out.println("naturalJoin(play, actors)");
        System.out.println(RelationOperation.naturalJoin(play, actors));

        System.out.println("Print all related tables: (make sure name is back)");
        System.out.println(actors);
        System.out.println(play);

        System.out.println("naturalJoin(actors, actors)");
        System.out.println(RelationOperation.naturalJoin(actors, actors));
        System.out.println(actors);
    }
}