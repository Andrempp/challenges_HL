package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public final class BicWriter {
	
	public boolean log = true; // Debug logging is active: true, inactive: false
	public boolean logFileFlag = true; // File: true; System.out: false
	protected String logFile;
	protected Writer logWriter = null;
	protected String ident = new String();

	public BicWriter(String file){
		logFile = file;
	}

	public BicWriter(){
		logFileFlag = false;
	}

	public void print(String str) {
		try {
			if(logWriter == null) {
				if(logFileFlag) logWriter = new BufferedWriter(new FileWriter(logFile));
				else logWriter = new BufferedWriter(new OutputStreamWriter(System.out));
			}
			logWriter.append(str).flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void enter(String str){
		ident = ident + "\t";
		print(str+"\n"+ident);
	}
	
	public void back(String str){
		if(ident.length()>2) ident = ident.substring(2);		
		else ident = "";
		print(str+"\n"+ident);
	}
	
	public void println(String str){
		print(str+"\n"+ident);
	}
}
