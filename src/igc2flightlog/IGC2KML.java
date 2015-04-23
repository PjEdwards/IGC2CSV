package igc2flightlog;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Convert an array of IGC formatted flight data files into a simple, comma delimited
 * text file with a single line for each flight.
 * @author pedwards
 */
public class IGC2KML {
    
    public static final double FEET_PER_METER = 3.28084;

    /**
     * @param args the list of files to convert... either a directory name in which
     * case all .IGC files within the directory will be processed, or a list of 
     * full paths to .IGC files to be processed
     */
    public static void main(String[] args) {
        
        if(args.length < 1){
            Logger.getLogger(IGC2KML.class.getName()).log(Level.SEVERE, "Please pass in the input file path as the first argument and the output file path as the second");
            System.exit(1);
        }

        //Get the file name(s)
        String filename = args[0];
        
        //Initialize the converter instance
        IGC2KML converter = new IGC2KML();
        
        //Load the file(s) into an iterable array
        ArrayList<File> files = converter.getIGCFilesFromPath(filename);
        if(files.size() < 1){
            Logger.getLogger(IGC2KML.class.getName()).log(Level.SEVERE, "No IGC files on the path specified");
            System.exit(0);
        }

        //Create the output file and write the header line
        File kml = null; 
        BufferedWriter bfo = null;
        try {
            String kmlFileName = files.get(0).getAbsolutePath() + ".kml";
            System.out.println(kmlFileName);
            kml = new File(kmlFileName);
            //write the output file as a .csv          
            bfo = new BufferedWriter(new FileWriter(kml));
            bfo.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            bfo.newLine();
            bfo.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
            bfo.newLine();
        }catch(IOException ioex){
            Logger.getLogger(IGC2KML.class.getName()).log(Level.SEVERE, "Error creating output file", ioex);
            System.exit(1);
        }
            
        //Iterate over all the igc files and convert them to a log entry. Write it to the output file
        String kmlEntry;
        for(File igcFile : files){
            try {
                bfo.write("<Document><name>Awesome Flight!</name>");
                bfo.newLine();
                bfo.write("<open>1</open>\n");
                bfo.write("<Style id=\"green-dot\">\n" +
                "      <IconStyle>\n" +
                "        <scale>.2</scale>\n" +        
                "        <Icon>\n" +
                "          <href>http://maps.google.com/mapfiles/kml/paddle/grn-blank-lv.png</href>\n" +
                "        </Icon>\n" +
                "      </IconStyle>\n" +
                "    </Style>");
                bfo.newLine();
                converter.generateKmlFromIGC(igcFile, bfo);
                bfo.write("</Document>");
                bfo.newLine();
            } catch (Exception ex) {
                Logger.getLogger(IGC2KML.class.getName()).log(Level.SEVERE, "The file: " + igcFile.getName() + " could not be converted into a log entry.", ex); 
            }
        }
        
        //Finalize the logfile
        try {
            bfo.write("</kml>");
            bfo.close();
        } catch (IOException ex) {
            Logger.getLogger(IGC2KML.class.getName()).log(Level.SEVERE, "Failed to close the output file", ex);
            System.exit(1);
        }
        
        System.out.println("Log File Written!");
        //exit
        System.exit(0);
    }
    
    /**
     * Get a list of IGC files from a directory or file path
     * @param path
     * @return 
     */
    public ArrayList<File> getIGCFilesFromPath(String path){
        ArrayList<File> files = new ArrayList<File>();
        File file = new File(path);
        if(file.isDirectory()){
            //get all the IGC files from within this directory
            String[] fileNames = file.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    //if the file extension is .igc return true, else false
                    return name.toLowerCase().endsWith(".igc");
                }
            });
            for(String f : fileNames){
                files.add(new File(path + "/" + f));
            }
        }else{
            //just the one file
            if(file.getName().toLowerCase().endsWith(".igc")){
                files.add(file);
            }
        }
        return files;
    }
    
    /**
     * Parse the file into a log entry (comma delimited string)
     * @param file
     * @return
     * @throws ParseException
     * @throws IOException 
     */
    public void generateKmlFromIGC(File file, BufferedWriter bfo) throws ParseException, IOException{
        
        //prepare the variables
        String launchDate = "";
        ArrayList<ArrayList> dataPoints = new ArrayList<ArrayList>();
        ArrayList<String> point;        
        //Parse the file into populated objects
        BufferedReader bfr = new BufferedReader(new FileReader(file));           
        String line = bfr.readLine();
        
        while(line != null){
            //Is the line the date?
            if(line.indexOf("HFDTE") != -1){
                launchDate = line.substring(5,11);
            }else{
                //is the line a B line?
                if(line.indexOf("B")==0){
                    String time = line.substring(1, 7);
                    String lat = line.substring(7, 15);
                    String lon = line.substring(15,24);
                    String validity = line.substring(24,25);
                    String pressureAlt = line.substring(25,30);
                    String gnssAlt = line.substring(30,35);
                    String tas = line.substring(35,38);                    
                    //Only use the line item if it has GPS altitude
                    if(validity.equals("A")){
                        point = new ArrayList<String>();
                        point.add(time);
                        point.add(lat);
                        point.add(lon);
                        point.add(pressureAlt);
                        point.add(gnssAlt);
                        point.add(tas);
                        dataPoints.add(point);
                    }
                }
            }
            line = bfr.readLine();
        }
        bfr.close();

        //variables for calculating stuff
        int previousTAS = 0;
        int maxTAS = 0;
        int maxAlt = 0;
        int pointAlt = 0;
        String duration = "";
        int beginAlt = 0;
        int endAlt = 0;
        int airspeed = 0;
        int fixNum = 0;
        String launchPoint = "";
        String landPoint = "";
        Date launchTime = null;
        Date landTime = null;
        SimpleDateFormat sdfIn = new SimpleDateFormat("ddMMyykkmmss");
        SimpleDateFormat sdfOut = new SimpleDateFormat("MM/dd/yyyy kk:mm");
        String lineKml = "";
        String lineCoords = "";
        boolean flightBegun = false;
        boolean flightEnded = false;
        for (ArrayList<String> dp : dataPoints) {          
            airspeed = (int)(Math.round(Integer.parseInt(dp.get(5)) * 0.621371));
            if(airspeed > maxTAS) {
                maxTAS = airspeed;
            }
            double tempAlt = Double.parseDouble(dp.get(4));
            tempAlt = tempAlt * IGC2KML.FEET_PER_METER; //convert meters to feet
            pointAlt = (int)Math.round(tempAlt);
            double lat = getCoordFromString(dp.get(1));
            double lon = getCoordFromString(dp.get(2));
            
            //The linestring
            lineCoords += lon + "," + lat + "," + dp.get(4) + " ";
            
            if(flightEnded) {
                continue;
            }
            
            if(!flightBegun){
                if(airspeed>0){
                    flightBegun = true;
                    launchTime = sdfIn.parse(launchDate+dp.get(0));
                    beginAlt = pointAlt;
                    maxAlt = pointAlt;
                    launchPoint = lat + " " + lon;
                    bfo.write("<Folder>\n<name>Fixes - " + sdfOut.format(launchTime) + " UTC</name>\n");
                    lineKml += "<Folder>\n<name>Flightpath - " + sdfOut.format(launchTime) + " UTC</name>\n";
                    lineKml += "<Placemark>\n<LineString>\n<altitudeMode>absolute</altitudeMode>\n";
                    lineKml += "<coordinates>\n";
                    bfo.newLine();
                } else {
                    continue;
                }
            }
            
            bfo.write("<Placemark>");
            bfo.newLine();
            //bfo.write("<name>" + ++fixNum + "</name>");
            //bfo.newLine();
            bfo.write("<styleUrl>#green-dot</styleUrl>");
            bfo.newLine();
            bfo.write("<ExtendedData>");
            bfo.newLine();
            bfo.write("<Data name=\"tas\"><displayName>True Air Speed</displayName><value>" + airspeed + " mph</value></Data>");
            bfo.newLine();
            bfo.write("<Data name=\"alt\"><displayName>Altitude</displayName><value>" + pointAlt + " ft</value></Data>");
            bfo.newLine();
            bfo.write("</ExtendedData>");
            bfo.write("<Point><altitudeMode>absolute</altitudeMode>");
            bfo.newLine();
            bfo.write("<coordinates>" + lon + "," + lat + "," + dp.get(4) + "</coordinates>");
            bfo.newLine();
            bfo.write("</Point>");
            bfo.newLine();
            bfo.write("</Placemark>");
            bfo.newLine();
            
            if(pointAlt > maxAlt) maxAlt = pointAlt;
            if(flightBegun && airspeed==0 && previousTAS==0){
                //flight ended
                flightEnded = true;
                landTime = sdfIn.parse(launchDate+dp.get(0));
                landPoint = dp.get(1) + " " + dp.get(2);
                endAlt = pointAlt;
                long minutes = (landTime.getTime() - launchTime.getTime())/1000/60;
                long hours = minutes/60;
                minutes = minutes % 60;
                minutes++; //I'd rather round up
                duration = Long.toString(hours) + ":" + Long.toString(minutes) + ":" + "00";
            }
            previousTAS = airspeed;
        }
        
        bfo.write("</Folder>\n");
        lineKml += lineCoords.trim() + "\n</coordinates>\n</LineString>\n</Placemark>\n</Folder>";
        bfo.write(lineKml);
        System.out.println( "The max airspeed is " + maxTAS + " kph");
        /*
        sdf.applyPattern("MM/dd/yyyy kk:mm:ss");
        StringBuilder newLine = new StringBuilder(",");
        newLine.append(sdf.format(launchTime));
        newLine.append(",");
        newLine.append(duration);
        newLine.append(",");
        newLine.append(Integer.toString(beginAlt));
        newLine.append(",");
        newLine.append(Integer.toString(maxAlt));
        newLine.append(",");
        newLine.append(Integer.toString(endAlt));
        newLine.append(",");
        newLine.append(this.getAverageClimbRate(dataPoints, 2)).append("-").append(this.getAverageClimbRate(dataPoints, 15)).append("-").append(this.getAverageClimbRate(dataPoints, 30));
        newLine.append(",");
        newLine.append(launchPoint);
        newLine.append(",");
        newLine.append(landPoint);
        newLine.append(",,,,");
        newLine.append(file.getName());
        return newLine.toString();
        */
    }
    
    /**
     * Coordinates are input as WGS84 e.g. 3453787N 08526761W and 
     * need to be output in decimal format
     * @param coordString
     * @return double decimal value of coordinate
     */
    private double getCoordFromString(String coordString) {
        String quadrant = coordString.substring(coordString.length()-1);
        String coord = coordString.substring(0, coordString.length()-1);
        double degrees;
        double decimalMinutes;
        if(quadrant.equals("W") || quadrant.equals("E")) {
            degrees = Double.parseDouble(coord.substring(0,3));
            decimalMinutes = Double.parseDouble(coord.substring(3))/1000/60;
        } else{
            degrees = Double.parseDouble(coord.substring(0,2));
            decimalMinutes = Double.parseDouble(coord.substring(2))/1000/60;
        }        
        double retval = degrees + decimalMinutes;
        if(quadrant.equals("W") || quadrant.equals("S")) {
            retval = retval * -1;
        }
        //System.out.println("The incoming string was " + coordString + " and the outgoing coord is " + retval);
        return retval;
    }
    
    private int getAverageClimbRate(ArrayList<ArrayList> dataPoints, int interval){
        int maxAvg = 0;
        int avg = 0;
        Date firstPoint = null;
        Date secondPoint = null;
        SimpleDateFormat sdf = new SimpleDateFormat("kkmmss");
        for(ArrayList<String> dp : dataPoints) {
            //Ignore any measurements when not flying
            if(Integer.parseInt(dp.get(5)) == 0){
                continue;
            }
            try {
                firstPoint = sdf.parse(dp.get(0));
            } catch (ParseException ex) {
                Logger.getLogger(IGC2KML.class.getName()).log(Level.SEVERE, "Failed to parse date while calculating 12 second average. Skipping point and moving on.", ex);
                continue;
            }
            for(ArrayList<String> dp2 : dataPoints){
                try {
                    secondPoint = sdf.parse(dp2.get(0));
                } catch (ParseException ex) {
                    Logger.getLogger(IGC2KML.class.getName()).log(Level.SEVERE, "Failed to parse date while calculating 12 second average. Skipping point and moving on.", ex);
                    continue;
                }
                double secs = (double)((secondPoint.getTime() - firstPoint.getTime())/1000);
                if(secs >= interval){ //greater than x seconds between points
                    double alt1 = Integer.parseInt(dp.get(3));
                    double alt2 = Integer.parseInt(dp2.get(3));
                    if(alt2 > alt1){ //higher altitude on the second point means we're climbing
                        double tmp = (alt2-alt1)/secs; //average climb in meters per second
                        tmp = tmp*60; //average climb in meters per minute                        
                        tmp = tmp*IGC2KML.FEET_PER_METER; //average climb in feet per minute
                        avg = (int)Math.round(tmp);
                        if(avg > maxAvg){
                            maxAvg = avg;
                        }
                    }                    
                    break;
                }
            }
        }
        return maxAvg;
    }
}
