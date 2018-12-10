import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

//https://mvnrepository.com/artifact/au.com.bytecode/opencsv/2.4
import au.com.bytecode.opencsv.CSVReader;

public class CsvReader {
	
	//Here you should write the file name of the CSV input file and the NT output file
	//In case CSV file is too large, you could divide the output file to 2 parts by specifying the middle of the file
	public static String OutputNtFileNamePart1= "summary-organization-part1.nt";
	public static String OutputNtFileNamePart2= "summary-organization-part2.nt";
	public static String inputCsvFileName= "organizations.csv";
	public static int midDataRow=300000;
	
	
	public static void main(String[] args) throws IOException {
		
		File fout1 = new File(OutputNtFileNamePart1);
		FileOutputStream fos1 = new FileOutputStream(fout1);
	 
		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(fos1));
		
		File fout2 = new File(OutputNtFileNamePart2);
		FileOutputStream fos2 = new FileOutputStream(fout2);
	 
		BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(fos2));
		
		//The library for CSV reader should be added from https://mvnrepository.com/artifact/au.com.bytecode/opencsv/2.4
		 CSVReader reader = new CSVReader(new FileReader(inputCsvFileName));
		 
		    String [] nextLine;
		    int i=0;
		    while ((nextLine = reader.readNext()) != null) {
		    	if(i<midDataRow) {
		    		
		        // nextLine[] is an array of the CSV columns per line
		    	//If you are creating CSV file for Organization or People, select Permalink column. If other entity types, the UUID column should be selected in nextLine[COLUMN NUMBER]
		    		bw1.write("<http://linked-crunchbase.org/api"+nextLine[1]+"#id> <http://linked-crunchbase.org/api-vocab#api_path> <http://linked-crunchbase.org/api"+nextLine[1]+"?items_per_page=250> .");
		    		bw1.newLine();
		    		i++;
		    	}
		    	else {
		    		bw2.write("<http://linked-crunchbase.org/api"+nextLine[1]+"#id> <http://linked-crunchbase.org/api-vocab#api_path> <http://linked-crunchbase.org/api"+nextLine[1]+"?items_per_page=250> .");
					bw2.newLine();
		    	}
		    	
		    }
		    
		    bw1.close();
		    bw2.close();
	}

}
