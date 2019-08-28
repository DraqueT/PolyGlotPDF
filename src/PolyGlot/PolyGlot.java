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

import java.awt.FontFormatException;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

/**
 * Starts up PolyGlot and does testing for OS/platform that would be
 * inappropriate elsewhere
 *
 * @author Draque Thompson
 */
public class PolyGlot {

    private static final String PDFCOMMAND = "pdf-export";
    private static final String EXCELTOCVSCOMMAND = "excel-to-cvs";
    private static final String EXPORTTOEXCELCOMMAND = "export-to-excel";
    private static final String EXCELTOCVSUSAGE = "PolyGlot_J8_Bridge " + EXCELTOCVSCOMMAND + " <EXCEL-FILE> <TARGET-WRITE> <SHEET-NUMBER>";
    private static final String TRUESTRING = "true";
    private static final String EXPORTTOEXCELUSAGE = "PolyGlot_J8_Bridge " + EXPORTTOEXCELCOMMAND + "<POLYGLOT-ARCHIVE> <TARGET-WRITE> <TRUE/FALSE SEPARATE DECLENSIONS>";
    private static final String PDFEXPORTUSAGE = "Consult internal documentation.";

    public static void main(final String[] args) {
        switch (args[0]) {
            case PDFCOMMAND:
                System.out.print(pdfExport(args));
                break;
            case EXCELTOCVSCOMMAND:
                System.out.print(excelToCvs(args));
                break;
            case EXPORTTOEXCELCOMMAND:
                System.out.print(exportToExcel(args));
                break;
            default:
                System.out.print("ERROR: Unrecognized command: " + args[0]);
        }
    }

    private static String pdfExport(String[] args) {
        String ret;

        if (args.length == 15) {
            String readFrom = args[1];
            String writeTo = args[2];
            
            try {
                DictCore core = new DictCore();
                core.readFile(readFrom);

                try {
                    PExportToPDF pdf = new PExportToPDF(core, writeTo);
                    pdf.setTitleText(args[3]);
                    pdf.setSubTitleText(args[4]);
                    pdf.setCoverImagePath(args[5]);
                    pdf.setForewardText(args[6]);
                    pdf.setPrintAllConjugations(args[7].toLowerCase().equals(TRUESTRING));
                    pdf.setPrintConLocal(args[8].toLowerCase().equals(TRUESTRING));
                    pdf.setPrintGlossKey(args[9].toLowerCase().equals(TRUESTRING));
                    pdf.setPrintGrammar(args[10].toLowerCase().equals(TRUESTRING));
                    pdf.setPrintLocalCon(args[11].toLowerCase().equals(TRUESTRING));
                    pdf.setPrintOrtho(args[12].toLowerCase().equals(TRUESTRING));
                    pdf.setPrintPageNumber(args[13].toLowerCase().equals(TRUESTRING));
                    pdf.setPrintWordEtymologies(args[14].toLowerCase().equals(TRUESTRING));

                    pdf.print();

                    ret = "SUCCESS";
                } catch (IOException e) {
                    ret = "ERROR: Unable to write to file: " + e.getLocalizedMessage();
                }
            } catch (IOException | IllegalStateException e) {
                ret = "ERROR: Unable to read PolyGlot file: " + readFrom;
            } catch (FontFormatException e) {
                ret = "ERROR: Unable to load font from PolyGlot archive: " + e.getLocalizedMessage();
            }
        } else {
            ret = "ERROR: Wrong number of arguments given for comand.\nUsage: " + PDFEXPORTUSAGE;
        }

        return ret;
    }

    private static String exportToExcel(String[] args) {
        String ret;

        if (args.length == 4) {
            String exportFrom = args[1];
            String exportTo = args[2];
            boolean separateDeclensions = args[3].toLowerCase().equals(TRUESTRING);

            try {
                DictCore core = new DictCore();
                core.readFile(exportFrom);

                try {
                    ExcelExport.exportExcelDict(exportTo, core, separateDeclensions);
                    ret = "SUCCESS";
                } catch (IOException e) {
                    ret = "ERROR: Unable to export to: " + exportTo;
                }
            } catch (IOException | IllegalStateException ex) {
                ret = "ERROR: Unable to read PolyGlot file: " + exportFrom;
            } catch (FontFormatException e) {
                ret = "ERROR: Unable to load font from PolyGlot archive: " + e.getLocalizedMessage();
            }

        } else {
            ret = "ERROR: Wring number of arguments given for command.\nUsage: " + EXPORTTOEXCELUSAGE;
        }

        return ret;
    }

    private static String excelToCvs(String[] args) {
        String ret;

        if (args.length == 4) {
            String excelFile = args[1];
            String targetWrite = args[2];

            try {
                int sheet = Integer.parseInt(args[3]);
                ExcelToCsv.readExcel(excelFile, targetWrite, sheet);
                ret = "SUCCESS";
            } catch (NumberFormatException e) {
                ret = "ERROR: Argument 3 must be an integer value.\nUsage: " + EXCELTOCVSUSAGE;
            } catch (FileNotFoundException e) {
                ret = "ERROR: File nout found: " + excelFile;
            } catch (IOException e) {
                ret = "ERROR: Unable to write to file: " + targetWrite + " due to: " + e.getLocalizedMessage();
            } catch (InvalidFormatException e) {
                ret = "ERROR: Unrecognized Excel format.";
            } catch (Exception e) {
                ret = "Error: Unable to convert Excel file: " + e.getLocalizedMessage();
            }
        } else {
            ret = "ERROR: Wrong number of args for cvs conversion.\nUsage: " + EXCELTOCVSUSAGE;
        }

        return ret;
    }
}
