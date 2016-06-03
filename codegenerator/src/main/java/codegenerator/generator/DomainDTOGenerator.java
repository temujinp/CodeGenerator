package codegenerator.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Temujin Penlington
 */
public class DomainDTOGenerator {
    
    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/business";
    static final String DB = "business";
    
    //  Database credentials
    static final String USER = "root";
    static final String PASS = "root";
    
    // Generation parameters
    static final String NAMESPACE = "com.business.server.domain.dto";
    static final String PREFIX = "";
    static final String POSTFIX = "DTO";
    
    // Directory where resources will be created
    static final String DIRECTORY = "C:\\project\\dto\\";

    public static void main(String[] args) {
        
        //STEP 1: Declare db resources
        Connection conn = null;
        Statement stmt = null;
        Statement stmt2 = null;
        
        // Workload to be done
        try {
            //STEP 2: Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            //STEP 3: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            //STEP 4: Execute the select query
            stmt = conn.createStatement();
            String sql;
            sql = "SHOW TABLES IN " + DB;
            ResultSet rs = stmt.executeQuery(sql);

            String head = "package " + NAMESPACE +";\n\nimport java.io.Serializable;\nimport java.util.Date;\nimport com.google.gson.Gson;\n";
            
            //STEP 5: Extract data from result set
            while (rs.next()) {
                
                System.out.println("\n\n");
                
                String table = rs.getString("Tables_in_business");
                String name = table.replaceAll("_", " ");
                StringBuilder b = new StringBuilder(name);
                StringBuilder cls = new StringBuilder();
                
                int i = 0;
                do {
                    b.replace(i, i + 1, b.substring(i, i + 1).toUpperCase());
                    i = b.indexOf(" ", i) + 1;
                } while (i > 0 && i < b.length());

                String top = b.toString().replaceAll(" ", "");
                
                //STEP 6: Compile class name
                cls.append(head);
                cls.append("\npublic class ");
                cls.append(top);
                cls.append(POSTFIX);
                cls.append(" implements Serializable {\n\n");
                
                //STEP 7: Get the database table column details 
                String sqlcols = "SHOW COLUMNS FROM `" + table + "` FROM " + DB;
                stmt2 = conn.createStatement();
                ResultSet rscols = stmt2.executeQuery(sqlcols);

                StringBuilder getset = new StringBuilder();
                
                //STEP 8: Loop through the column details
                while (rscols.next()) {
                    String col = rscols.getString("Field");
                    StringBuilder colCamel = new StringBuilder(col.replaceAll("_", " "));

                    //STEP 9: Compile the getter and setter camel case method names
                    int j = 0;
                    do {
                        colCamel.replace(j, j + 1, colCamel.substring(j, j + 1).toUpperCase());
                        j = colCamel.indexOf(" ", j) + 1;
                    } while (j > 0 && j < colCamel.length());
                    
                    String camel = colCamel.toString().replaceAll(" ", "");
                    String dataType = "";
                    
                    //STEP 10: create Java datatype equivalents of the MySql column types
                    if(rscols.getString("Type").startsWith("int") || rscols.getString("Type").startsWith("mediumint")){
                        dataType = "Integer";
                    }else if(rscols.getString("Type").startsWith("decimal") || rscols.getString("Type").startsWith("bigint")){
                        dataType = "Long";
                    }else if(rscols.getString("Type").startsWith("double")){
                        dataType = "Double";
                    }else if(rscols.getString("Type").startsWith("float")){
                        dataType = "Float";
                    }else if(rscols.getString("Type").equals("tinyint(1)") || rscols.getString("Type").startsWith("binary(1)")){
                        dataType = "Boolean";
                    }else if(rscols.getString("Type").startsWith("smallint") || rscols.getString("Type").startsWith("tinyint")){
                        dataType = "Short";
                    }else if(rscols.getString("Type").startsWith("char(1)")){
                        dataType = "Character";
                    }else if(rscols.getString("Type").startsWith("varchar") || rscols.getString("Type").endsWith("text") ||rscols.getString("Type").startsWith("char")){
                        dataType = "String";
                    }else if(rscols.getString("Type").startsWith("datetime") || rscols.getString("Type").startsWith("date") || rscols.getString("Type").endsWith("timestamp") || rscols.getString("Type").endsWith("time")){
                        dataType = "Date"; 
                    }
                    
                    //STEP 11: Write the private fields and heir datatypes
                    cls.append("\t");
                    cls.append(dataType);
                    cls.append(" ");
                    cls.append(col);
                    cls.append(" = null;//");
                    cls.append(rscols.getString("Type"));
                    cls.append("\n");
                    
                    //STEP 12: Write the getters and setters of the fields
                    getset.append("\n\tpublic ");
                    getset.append(dataType);
                    getset.append(" get");
                    getset.append(camel);
                    getset.append("(){\n");
                    getset.append("\t\treturn ");
                    getset.append(col);
                    getset.append(";\n\t}\n");
                    getset.append("\n\tpublic void set");
                    getset.append(camel);
                    getset.append("(");
                    getset.append(dataType);
                    getset.append(" value){\n");
                    getset.append("\t\tthis.");
                    getset.append(col);
                    getset.append(" = value;\n\t}\n");                    
                }
                
                //STEP 13: Create the Gson convertion decoration method
                cls.append("\n\tpublic ");    
                cls.append(top);
                cls.append(POSTFIX);
                cls.append("(){}\n");
                cls.append("\n\tpublic String toJSONString() {\n");
                cls.append("\t\tGson gson = new Gson();\n");
                cls.append("\t\treturn gson.toJson(this);\n");
                cls.append("\t} \n");
                
                cls.append(getset.toString());
                
                cls.append("\n}");
                System.out.println(cls);
                
                //STEP 14: Write the actuals code to the generated file
                File file = new File(DIRECTORY + top + POSTFIX + ".java");
                file.createNewFile();
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);            
                bw.write(cls.toString());
                bw.close();
            }
            //STEP 15: Clean-up environment
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException se) {
        } catch (ClassNotFoundException | IOException e) {
        } finally {
            //finally block used to close resources
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se2) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
            }
        }
    }
}
