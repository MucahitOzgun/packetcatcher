package com.company.sql.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class FileUtil {

	public static void writeToExcel(byte[] dataBin) throws Exception{

		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Bytes");
		for(int i = 0 ; i<16;i++) {
			sheet.setColumnWidth(i, 1500);
		}
		int byteCounter=0;
		int rowCounter =0;
		Row row = sheet.createRow(rowCounter);
		for(int i = 0 ; i<dataBin.length;i++) {

			if(byteCounter ==16) {
				byteCounter=0;
				rowCounter++;
				row = sheet.createRow(rowCounter);
			}
			Cell cell = row.createCell(byteCounter);
			cell.setCellValue(Integer.toString(ByteUtil.getUnsignedInt(dataBin[i])));
			byteCounter++;
		}

		Sheet sheetDecode = workbook.createSheet("Explain");
		sheetDecode.setColumnWidth(0, 40000);

		String[] decodedDataArray=ByteUtil.byteArrayToHexAndAsciiAndDecDump(dataBin).split("\n");
		for(int i=0;i<decodedDataArray.length;i++) {
			Row decodeRow = sheetDecode.createRow(i);
			Cell decodeCell = decodeRow.createCell(0);
			decodeCell.setCellValue(decodedDataArray[i].toString());
		}
		
		
		File currDir = new File(".");
		String path = currDir.getAbsolutePath();
		String fileLocation = path.substring(0, path.length() - 1) + "target/bytes.xlsx";

		FileOutputStream outputStream = new FileOutputStream(fileLocation);
		workbook.write(outputStream);
		workbook.close();
	}
	
	public static byte[] readFromExcel() throws Exception {
		FileInputStream file = new FileInputStream(new File("target/bytes.xlsx"));
		Workbook workbook = new XSSFWorkbook(file);
		Sheet sheet = workbook.getSheetAt(0);
		 
		String oneLinePacket ="";
		for (Row row : sheet) {
		    for (Cell cell : row) {
		    	oneLinePacket += cell.toString()+" ";
		    }
		}
		String[] stringBin = oneLinePacket.split(" ");
		byte[] dataBin = new byte[stringBin.length];
		for(int i=0;i<stringBin.length;i++) {
			if(stringBin[i].contains(".")) {
				stringBin[i]=stringBin[i].substring(0, stringBin[i].length()-2);
			}
			dataBin[i]=(byte)Integer.parseInt(stringBin[i]);
		}
		System.out.println(oneLinePacket); 
		return dataBin;
	}
	
	private static void writeToFile(byte[] dataBin) throws Exception{
		PrintWriter writer;
		writer = new PrintWriter("target/bytes.txt", "UTF-8");
		int counter =0;
		for(int i =0;i<dataBin.length;i++) {
			if(counter==16) {
				writer.println("\n");
				counter=0;
			}
			writer.print(ByteUtil.getUnsignedInt(dataBin[i])+" ");
			counter++;
		}

		writer.close();
	}
	private static byte[] readFromFile() throws Exception{
		File file = new File("target/bytes.txt"); 
		BufferedReader br = new BufferedReader(new FileReader(file)); 
		String oneLinePacket="";
		String st; 
		while ((st = br.readLine()) != null) {
			oneLinePacket+= st;
		}
		String[] stringBin = oneLinePacket.split(" ");
		byte[] dataBin = new byte[stringBin.length];
		for(int i=0;i<stringBin.length;i++) {
			dataBin[i]=(byte)Integer.parseInt(stringBin[i]);
		}
		System.out.println(oneLinePacket); 

		return dataBin;
	}

}
