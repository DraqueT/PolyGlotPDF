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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author draque
 */
public class ExcelToCsvTest {
 
    private final String targetFile = "test" + File.separator + "testFile.csv";
    private final String excelFile = "test" + File.separator + "TestResources" + File.separator + "excelImport.xlsx";
    private final String sheet1Expected;
    private final String sheet2Expected;
    
    public ExcelToCsvTest() {
        sheet1Expected = "\"COL 1\",\"COL 2\",\"COL 3\"\n" +
            "\"A\",\"AA\",\"AAA\"\n" +
            "\"B\"\n" +
            "\"C\",\"CC\",\"CCC\",\"CCCC\"\n" +
            "\"E\",,\"EEE\"\n" +
            "\"F\",\"F\n" +
            "F\",\"F\n" +
            "F\n" +
            "F\"\n" +
            "\"\"\"G\"\"\",\"G'\",\",G\"";
        
        sheet2Expected = "\"SHEET 2\"";
    }

    @Test
    public void testReadExcelSheet1() throws Exception {
        System.out.println("readExcel Sheet 1");
        
        String sheetNum = "0";
        String[] args = {
            "excel-to-cvs",
            excelFile,
            targetFile,
            sheetNum
        };
        
        OutputInterceptor interceptor = new OutputInterceptor(System.out);
        System.setOut(interceptor);
        
        PolyGlot.main(args);
        
        String result = interceptor.getIntercepted();
        String outputFile = readFile(targetFile);
        
        assertEquals(sheet1Expected, outputFile);
        assertEquals(result, "SUCCESS");
        
        new File (targetFile).delete();
    }
    
    @Test
    public void testReadExcelSheet2() throws Exception {
        System.out.println("readExcel Sheet 1");
        String sheetNum = "1";
        String[] args = {
            "excel-to-cvs",
            excelFile,
            targetFile,
            sheetNum
        };
        
        OutputInterceptor interceptor = new OutputInterceptor(System.out);
        System.setOut(interceptor);
        
        PolyGlot.main(args);
        
        String result = interceptor.getIntercepted();
        String outputFile = readFile(targetFile);
        
        assertEquals(sheet2Expected, outputFile);
        assertEquals(result, "SUCCESS");
        
        new File (targetFile).delete();
    }
    
    @Test
    public void testReadExcelSheetNonintegerInput() throws Exception {
        System.out.println("readExcel Sheet 1");
        String sheetNum = "X";
        String[] args = {
            "excel-to-cvs",
            excelFile,
            targetFile,
            sheetNum
        };
        
        OutputInterceptor interceptor = new OutputInterceptor(System.out);
        System.setOut(interceptor);
        
        PolyGlot.main(args);
        
        String result = interceptor.getIntercepted();
        String outputFile = readFile(targetFile);
        
        assertEquals(null, outputFile);
        assertEquals(result, "ERROR: Argument 3 must be an integer value.\n" +
            "Usage: PolyGlot_J8_Bridge excel-to-cvs <EXCEL-FILE> <TARGET-WRITE> <SHEET-NUMBER>");
        
        new File (targetFile).delete();
    }
    
    @Test
    public void testReadExcelSheetTooManyArgs() throws Exception {
        System.out.println("readExcel Sheet 1");
        String sheetNum = "X";
        String[] args = {
            "excel-to-cvs",
            excelFile,
            targetFile,
            sheetNum,
            "BLOOP"
        };
        
        OutputInterceptor interceptor = new OutputInterceptor(System.out);
        System.setOut(interceptor);
        
        PolyGlot.main(args);
        
        String result = interceptor.getIntercepted();
        String outputFile = readFile(targetFile);
        
        assertEquals(null, outputFile);
        assertEquals(result, "ERROR: Wrong number of args for cvs conversion.\nUsage: PolyGlot_J8_Bridge excel-to-cvs <EXCEL-FILE> <TARGET-WRITE> <SHEET-NUMBER>");
        
        new File (targetFile).delete();
    }
    
    @Test
    public void testReadExcelSheetTooFewArgs() throws Exception {
        System.out.println("readExcel Sheet 1");
        String sheetNum = "X";
        String[] args = {
            "excel-to-cvs",
            excelFile
        };
        
        OutputInterceptor interceptor = new OutputInterceptor(System.out);
        System.setOut(interceptor);
        
        PolyGlot.main(args);
        
        String result = interceptor.getIntercepted();
        String outputFile = readFile(targetFile);
        
        assertEquals(null, outputFile);
        assertEquals(result, "ERROR: Wrong number of args for cvs conversion.\nUsage: PolyGlot_J8_Bridge excel-to-cvs <EXCEL-FILE> <TARGET-WRITE> <SHEET-NUMBER>");
        
        new File (targetFile).delete();
    }
    
    private String readFile(String fileIn) throws FileNotFoundException, IOException {
        String ret = "";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(fileIn))) {
            String line = reader.readLine();
            
            while (line != null) {
                ret += line + "\n";
                line = reader.readLine();
            }
            
            if (!ret.isEmpty()) {
                ret = ret.substring(0, ret.length() - 1);
            }
        } catch (FileNotFoundException e) {
            ret = null;
        }
        
        return ret;
    }
    //TODO: TEST CALLS TO MAIN
}
