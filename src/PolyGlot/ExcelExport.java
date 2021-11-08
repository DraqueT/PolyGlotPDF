/*
 * Copyright (c) 2014-2021, Draque Thompson, draquemail@gmail.com
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

import PolyGlot.ManagersCollections.ConjugationManager;
import PolyGlot.Nodes.ConWord;
import PolyGlot.Nodes.ConjugationNode;
import PolyGlot.Nodes.ConjugationPair;
import PolyGlot.Nodes.PronunciationNode;
import PolyGlot.Nodes.TypeNode;
import PolyGlot.Nodes.WordClassValue;
import PolyGlot.Nodes.WordClass;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;

/**
 * This class exports an existing dictionary to an excel spreadsheet
 *
 * @author Draque
 */
public class ExcelExport {
    
    private final DictCore core;
    private final ConjugationManager conMan;
    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet sheet;
    CellStyle localStyle = workbook.createCellStyle();
    CellStyle conStyle = workbook.createCellStyle();
    CellStyle boldHeader = workbook.createCellStyle();
    Font conFont = workbook.createFont();
    Font boldFont = workbook.createFont();
    
    private ExcelExport(DictCore _core) {
        core = _core;
        conMan = core.getConjugationManager();
        
        conFont.setFontName(core.getPropertiesManager().getFontCon().getFontName());
        boldFont.setBold(true);
        localStyle.setWrapText(true);
        conStyle.setWrapText(true);
        conStyle.setFont(conFont);
        boldHeader.setWrapText(true);
        boldHeader.setFont(boldFont);
    }
    
    /**
     * Exports a dictionary to an excel file (externally facing)
     *
     * @param fileName Filename to export to
     * @param core dictionary core
     * @param separateDeclensions whether to separate parts of speech into separate pages for declension values
     * @throws IOException on write error
     */
    public static void exportExcelDict(String fileName, DictCore core, boolean separateDeclensions) throws IOException {
        ExcelExport e = new ExcelExport(core);

        e.export(fileName, separateDeclensions);
    }
    
    /**
     * Returns a legal worksheet name from the string handed in (illegal characters removed, forced size
     * @param startName
     * @return 
     */
    private String legalWorksheetName(String startName) {
        // illegal characters: \ / * [ ] : ?
        String ret = startName.replaceAll("/|\\*|\\[|\\]|:|\\?", "");
        
        // max worksheet name length = 31 chars (why so short?!)
        if (ret.length() > 31) {
            ret = ret.substring(0, 30);
        }
        
        return ret;
    }

    private Object[] getWordFormOld(ConWord conWord, ConjugationPair[] conjList) {
        List<String> ret = new ArrayList<>();
        String declensionCell = "";

        ret.add(conWord.getValue());
        ret.add(conWord.getLocalWord());
        ret.add(conWord.getWordTypeDisplay());
        try {
            ret.add(conWord.getPronunciation());
        } catch (Exception e) {
            ret.add("<ERROR>");
        }

        String classes = "";
        for (Entry<Integer, Integer> curEntry : conWord.getClassValues()) {
            if (classes.length() != 0) {
                classes += ", ";
            }
            try {
                WordClass prop = (WordClass) core.getWordClassCollection().getNodeById(curEntry.getKey());
                WordClassValue value = prop.getValueById(curEntry.getValue());
                classes += value.getValue();
            } catch (Exception e) {
                classes = "ERROR: UNABLE TO PULL CLASS";
            }
        }
        ret.add(classes);
        
        for (ConjugationPair conjugation : conjList) {
            try {
                ConjugationNode existingValue = conMan.getConjugationByCombinedId(conWord.getId(), conjugation.combinedId);
                
                if (existingValue != null && conWord.isOverrideAutoConjugate()) {
                    declensionCell += existingValue.getValue() + ":";
                }
                else {
                    declensionCell += conMan.declineWord(conWord, conjugation.combinedId) + ":";
                }
            } catch (Exception e) {
                declensionCell += "DECLENSION ERROR";
            }
        }

        ret.add(declensionCell);
        ret.add(WebInterface.getTextFromHtml(conWord.getDefinition()));

        return ret.toArray();
    }
    
    /**
     * Returns list of all legal wordforms this word can take.
     * Accounts for overridden forms and properly filters forms marked as disabled
     * @param conWord
     * @param conjList
     * @return 
     */
    private List<String> getWordForm(ConWord conWord, ConjugationPair[] conjList) {
        List<String> ret = new ArrayList<>();

        ret.add(conWord.getValue());
        ret.add(conWord.getLocalWord());
        ret.add(conWord.getWordTypeDisplay());
        try {
            ret.add(conWord.getPronunciation());
        } catch (Exception e) {
            ret.add("<ERROR>");
        }

        String classes = "";
        for (Entry<Integer, Integer> curEntry : conWord.getClassValues()) {
            if (classes.length() != 0) {
                classes += ", ";
            }
            try {
                WordClass prop = (WordClass) core.getWordClassCollection().getNodeById(curEntry.getKey());
                WordClassValue value = prop.getValueById(curEntry.getValue());
                classes += value.getValue();
            } catch (Exception e) {
                classes = "ERROR: UNABLE TO PULL CLASS";
            }
        }
        
        for (Entry<Integer, String> curEntry : conWord.getClassTextValues()) {
            if (classes.length() != 0) {
                classes += ", ";
            }
            
            try {
                WordClass prop = (WordClass) core.getWordClassCollection().getNodeById(curEntry.getKey());
                classes += prop.getValue() + ":" + curEntry.getValue();
            } catch (Exception e) {
                classes = "ERROR: UNABLE TO PULL CLASS";
            }
        }
        
        ret.add(classes);

        for (ConjugationPair conjugation : conjList) {
            try {
                ConjugationNode existingValue = conMan.getConjugationByCombinedId(conWord.getId(), conjugation.combinedId);
                
                if (existingValue != null && conWord.isOverrideAutoConjugate()) {
                    ret.add(existingValue.getValue());
                }
                else {
                    ret.add(conMan.declineWord(conWord, conjugation.combinedId));
                }
            } catch (Exception e) {
                ret.add("DECLENSION ERROR");
            }
        }
        
        ret.add(WebInterface.getTextFromHtml(conWord.getDefinition()));

        return ret;
    }
    
    /**
     * Exports a dictionary to an excel file
     *
     * @param fileName Filename to export to
     * @param separateDeclensions whether to separate parts of speech into separate pages for declension values
     * @throws Exception on write error
     */
    private void export(String fileName, boolean separateDeclensions) throws IOException {
        this.recordWords(separateDeclensions);
        
        // record types on sheet
        sheet = workbook.createSheet("Parts of Speech");

        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("PoS");
        row.createCell(1).setCellValue("NOTES");

        int i = 0;
        for (TypeNode curNode : core.getTypes().getNodes()) {
            i++;
            row = sheet.createRow(i);

            Cell cell = row.createCell(0);
            cell.setCellValue(curNode.getValue());
            cell = row.createCell(1);
            cell.setCellValue(WebInterface.getTextFromHtml(curNode.getNotes()));
            cell.setCellStyle(localStyle);
        }

        // record word classes on sheet
        WordClass[] classes = core.getWordClassCollection().getAllWordClasses();
        
        if (classes.length != 0) {
            sheet = workbook.createSheet("Lexical Classes");
            int propertyColumn = 0;
            for (WordClass curProp : classes) {
                // get row, if not exist, create
                row = sheet.getRow(0);
                if (row == null) {
                    row = sheet.createRow(0);
                }

                Cell cell = row.createCell(propertyColumn);
                cell.setCellValue(curProp.getValue());
                cell.setCellStyle(boldHeader);

                int rowIndex = 1;
                for (WordClassValue curVal : curProp.getValues()) {
                    row = sheet.getRow(rowIndex);
                    if (row == null) {
                        row = sheet.createRow(rowIndex);
                    }

                    cell = row.createCell(propertyColumn);
                    cell.setCellStyle(localStyle);
                    cell.setCellValue(curVal.getValue());

                    rowIndex++;
                }

                propertyColumn++;
            }
        }

        // record pronunciations on sheet
        sheet = workbook.createSheet("Pronunciations");

        row = sheet.createRow(0);
        row.createCell(0).setCellValue("CHARACTER(S)");
        row.createCell(1).setCellValue("PRONUNCIATION");

        i = 0;
        for (PronunciationNode curNode : core.getPronunciationMgr().getPronunciations()) {
            i++;
            row = sheet.createRow(i);

            Cell cell = row.createCell(0);
            cell.setCellStyle(conStyle);
            cell.setCellValue(curNode.getValue());

            cell = row.createCell(1);
            cell.setCellStyle(localStyle);
            cell.setCellValue(curNode.getPronunciation());
        }

        try {
            try (FileOutputStream out = new FileOutputStream(new File(fileName))) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new IOException("Unable to write file: " + fileName);
        }
    }
    
    private void recordWords(boolean separateDeclensions) {
        // separate words by part of speech if requested so that each POS can have distinct declension columns
        if (separateDeclensions) {
            // create separate page for each part of speech
            
            for (TypeNode type : core.getTypes().getNodes()) {
                ConWord filter = new ConWord();
                filter.setWordTypeId(type.getId());
                
                try {
                    ConWord[] list = core.getWordCollection().filteredList(filter);

                    // don't make a sheet for types with no words
                    if (list.length == 0) {
                        continue;
                    }

                    sheet = workbook.createSheet(legalWorksheetName("Lex-" + type.getValue()));

                    Row row = sheet.createRow(0);
                    row.createCell(0).setCellValue(core.conLabel().toUpperCase() + " WORD");
                    row.createCell(1).setCellValue(core.localLabel().toUpperCase() + " WORD");
                    row.createCell(2).setCellValue("PoS");
                    row.createCell(3).setCellValue("PRONUNCIATION");
                    row.createCell(4).setCellValue("CLASS(ES)");

                    // create column for each declension
                    ConjugationPair[] conjList = core.getConjugationManager().getAllCombinedIds(type.getId());
                    int colNum = 4;
                    for (ConjugationPair curDec : conjList) {
                        colNum++;
                        row.createCell(colNum).setCellValue(curDec.label.toUpperCase());
                    }

                    row.createCell(colNum + 1).setCellValue("DEFINITION");
                
                    int rowCount = 1;
                    for (ConWord word : list) {
                        row = sheet.createRow(rowCount);
                        
                        Object[] wordArray = getWordForm(word, conjList).toArray();
                        for (int colCount = 0; colCount < wordArray.length; colCount++) {
                            Cell cell = row.createCell(colCount);
                            cell.setCellValue((String)wordArray[colCount]);

                            if (colCount == 0 || colCount > 4) {
                                cell.setCellStyle(conStyle);
                            } else {
                                cell.setCellStyle(localStyle);
                            }
                        }
                        rowCount++;
                    }
                } catch (Exception e) {
                    System.out.println( "Unable to export " + type.getValue() + " lexical values");
                }
            }
        } else {
            recordWordsOld();
        }
    }
    
    /**
     * Old style of printing words
     */
    private void recordWordsOld () {
        sheet = workbook.createSheet("Lexicon");
        Map<Integer, ConjugationPair[]> typeDecMap = new HashMap<>();
        
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue(core.conLabel().toUpperCase() + " WORD");
        row.createCell(1).setCellValue(core.localLabel().toUpperCase() + " WORD");
        row.createCell(2).setCellValue("PoS");
        row.createCell(3).setCellValue("PRONUNCIATION");
        row.createCell(4).setCellValue("CLASS(ES)");
        row.createCell(5).setCellValue("DECLENSIONS");
        row.createCell(6).setCellValue("DEFINITIONS");

        //Iterator<ConWord> wordIt = core.getWordCollection().getWordNodes().iterator();
        //for (Integer i = 1; wordIt.hasNext(); i++) {
        //    ConWord word = wordIt.next();
        int i = 0;
        for (ConWord word : core.getWordCollection().getWordNodes()) {
            i++;
            ConjugationPair[] decList;
            
            if (typeDecMap.containsKey(word.getWordTypeId())) {
                decList = typeDecMap.get(word.getWordTypeId());
            } else {
                decList = core.getConjugationManager().getAllCombinedIds(word.getWordTypeId());
                typeDecMap.put(word.getWordTypeId(), decList);
            }
            
            Object[] wordArray = getWordFormOld(word, decList);
            row = sheet.createRow(i);
            for (Integer j = 0; j < wordArray.length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue((String) wordArray[j]);

                if (j == 0) {
                    cell.setCellStyle(conStyle);
                } else {
                    cell.setCellStyle(localStyle);
                }
            }
        }
    }
}
