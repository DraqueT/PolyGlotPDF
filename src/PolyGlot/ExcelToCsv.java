/*
 * Copyright (c) 2019, Draque Thompson
 * All rights reserved.
 *
 * Licensed under: Creative Commons Attribution-NonCommercial 4.0 International Public License
 *  See LICENSE.TXT included with this code to read the full license agreement.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package PolyGlot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * This class exists solely to convert Excel files into CSV files, which can
 * then be read natively by PolyGlot
 *
 * @author Draque Thompson
 */
public class ExcelToCsv {

    /**
     * Creates csv file based on given excel.Uses double quotes for
 encapsulation.Supports multiline cells.Supports encapsulated single
 quotes and double quotes.
     *
     * @param excelFile file to read from
     * @param targetFile file to write to (typically temp file)
     * @param sheetNum sheet number to read from
     * @throws java.io.IOException if file read interrupted
     * @throws java.io.FileNotFoundException if file at path excelFile DNE
     * @throws org.apache.poi.openxml4j.exceptions.InvalidFormatException if excel file of an unrecognized format
     */
    public static void readExcel(String excelFile, String targetFile, int sheetNum) throws IOException, FileNotFoundException, InvalidFormatException {
        String csvString = read(new File(excelFile), sheetNum);
        
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            writer.write(csvString);
        }
    }
    
    private static String read(File excelFile, int sheetNum) throws FileNotFoundException, IOException, InvalidFormatException {
        String ret = "";
        Workbook wb;
        Sheet mySheet;
        
        try (InputStream myFile = new FileInputStream(excelFile)) {
            wb = WorkbookFactory.create(myFile);
            mySheet = wb.getSheetAt(sheetNum);
            Iterator<Row> rowIterator = mySheet.iterator();

            while (rowIterator.hasNext()) {
                ret += readRow(rowIterator.next());
            }
        }
        
        return ret;
    }
    
    private static String readRow(Row row) {
        String ret = "";
        int columnCount = row.getLastCellNum();
        
        for (int i = 0; i < columnCount; i++) {
            Cell cell = row.getCell(i);
            String cellContents = "";
            
            if (cell != null) {
                cellContents = cell.getStringCellValue();
                cellContents = cellContents.replace("\"", "\"\""); // double-quotes must be escaped as 2x double-quotes
                cellContents = "\"" + cellContents + "\""; // cells encapsulated in double quotes to allow for multilines
            }
            
            ret += cellContents + ","; // add comma to delimit cell contents
        }
        
        // if row not empty, eliminate trailing comma
        if (!ret.isEmpty()) {
            ret = ret.substring(0, ret.length() - 1);
        }
        
        return ret + "\n"; // ends row with newline
    }
}
