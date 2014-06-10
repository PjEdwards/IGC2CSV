package random;

import java.io.*;

/**
 *
 * @author pedwards
 */
public class CSV2JSON {
    
    public static void main(String[] args) {
        
        File file = new File("/Users/pedwards/Downloads/cub_tables_jan_2013/lookup_region.csv");
        File logout = new File("/Users/pedwards/Downloads/cub_tables_jan_2013/lookup_region.json");
        BufferedWriter bfo = null;
        BufferedReader bfr = null;
        
        try {
            bfr = new BufferedReader(new FileReader(file));           
            bfo = new BufferedWriter(new FileWriter(logout));
            bfo.write("[");
            bfo.newLine();
            
            String line = bfr.readLine();
            while(line != null){
                String[] cols = line.split(";");
                if(cols[0].equals("id")) {
                    //move on
                } else {
                    bfo.write( "{'id':" + cols[0] + ", 'description':'" + cols[1] + "'},");
                    bfo.newLine();
                }
                
                line = bfr.readLine();
            }
            bfo.write("]");
            bfo.close();
            bfr.close();
        }catch(IOException ioex){
            System.err.println(ioex.getMessage());
        }
    }
}
