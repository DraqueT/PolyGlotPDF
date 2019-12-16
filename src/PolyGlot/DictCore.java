/*
 * Copyright (c) 2014-2019, Draque Thompson, draquemail@gmail.com
 * All rights reserved.
 *
 * Licensed under: Creative Commons Attribution-NonCommercial 4.0 International Public License
 * See LICENSE.TXT included with this code to read the full license agreement.
 *
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

import PolyGlot.CustomControls.PAlphaMap;
import PolyGlot.Nodes.DeclensionNode;
import PolyGlot.ManagersCollections.PropertiesManager;
import PolyGlot.ManagersCollections.GrammarManager;
import PolyGlot.ManagersCollections.PronunciationMgr;
import PolyGlot.ManagersCollections.FamilyManager;
import PolyGlot.ManagersCollections.DeclensionManager;
import PolyGlot.ManagersCollections.TypeCollection;
import PolyGlot.ManagersCollections.ConWordCollection;
import PolyGlot.ManagersCollections.EtymologyManager;
import PolyGlot.ManagersCollections.ImageCollection;
import PolyGlot.ManagersCollections.OptionsManager;
import PolyGlot.ManagersCollections.ReversionManager;
import PolyGlot.ManagersCollections.RomanizationManager;
import PolyGlot.ManagersCollections.ToDoManager;
import PolyGlot.ManagersCollections.VisualStyleManager;
import PolyGlot.ManagersCollections.WordClassCollection;
import java.awt.FontFormatException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class DictCore {
    
    private final String version = "2.5";
    private ConWordCollection wordCollection;
    private TypeCollection typeCollection;
    private DeclensionManager declensionMgr;
    private PropertiesManager propertiesManager;
    private PronunciationMgr pronuncMgr;
    private RomanizationManager romMgr;
    private FamilyManager famManager;
    // private LogoCollection logoCollection; //logographs not currently printed to PDF
    private GrammarManager grammarManager;
    private OptionsManager optionsManager;
    private WordClassCollection wordPropCollection;
    private ImageCollection imageCollection;
    private EtymologyManager etymologyManager;
    private VisualStyleManager visualStyleManager;
    private ReversionManager reversionManager;
    private ToDoManager toDoManager;
    private Object clipBoard;
    private boolean curLoading = false;
    private final Map<String, Integer> versionHierarchy = new HashMap<>();
    private Instant lastSaveTime = Instant.MIN;

    /**
     * Language core initialization
     *
     * @throws java.io.IOException
     */
    public DictCore() throws IOException {
        initializeDictCore();
    }
    
    private void initializeDictCore() throws IOException {
        wordCollection = new ConWordCollection(this);
        typeCollection = new TypeCollection(this);
        declensionMgr = new DeclensionManager(this);
        propertiesManager = new PropertiesManager(this);
        pronuncMgr = new PronunciationMgr(this);
        romMgr = new RomanizationManager(this);
        famManager = new FamilyManager(this);
        //logoCollection = new LogoCollection(this); //logographs not currently printed to PDF
        grammarManager = new GrammarManager();
        optionsManager = new OptionsManager(this);
        wordPropCollection = new WordClassCollection(this);
        imageCollection = new ImageCollection();
        etymologyManager = new EtymologyManager(this);
        visualStyleManager = new VisualStyleManager(this);
        reversionManager = new ReversionManager(this);
        toDoManager = new ToDoManager();

        PAlphaMap<String, Integer> alphaOrder = propertiesManager.getAlphaOrder();

        wordCollection.setAlphaOrder(alphaOrder);
        typeCollection.setAlphaOrder(alphaOrder);
        //logoCollection.setAlphaOrder(alphaOrder); //logographs not currently printed to PDF
        wordPropCollection.setAlphaOrder(alphaOrder);

        populateVersionHierarchy();
    }

    /**
     * Gets conlang name or CONLANG. Put on core because it's used a lot.
     *
     * @return either name of conlang or "Conlang"
     */
    public String conLabel() {
        return propertiesManager.getLangName().length() == 0
                ? "Conlang"
                : propertiesManager.getLangName();
    }

    /**
     * Gets local language name or Local Lang. Put on core because it's used a
     * lot.
     *
     * @return either name of local language or "Local Lang"
     */
    public String localLabel() {
        return propertiesManager.getLocalLangName().length() == 0
                ? "Local Lang"
                : propertiesManager.getLocalLangName();
    }

    public OptionsManager getOptionsManager() {
        return optionsManager;
    }

    public ImageCollection getImageCollection() {
        return imageCollection;
    }
    
    public VisualStyleManager getVisualStyleManager() {
        return visualStyleManager;
    }

    /**
     * Returns whether core is currently loading a file
     *
     * @return true if currently loading
     */
    public boolean isCurLoading() {
        return curLoading;
    }

    /**
     * Retrieves working directory of PolyGlot
     *
     * @return current working directory
     * @throws java.net.URISyntaxException
     */
    public String getWorkingDirectory() throws URISyntaxException {
        String ret = propertiesManager.getOverrideProgramPath();

        ret = ret.isEmpty() ? DictCore.class.getProtectionDomain().getCodeSource().getLocation().toURI().g‌​etPath() : ret;

        // in some circumstances (but not others) the name of the jar will be appended... remove
        if (ret.endsWith(PGTUtil.jarArchiveName)) {
            ret = ret.replace(PGTUtil.jarArchiveName, "");
        }

        return ret;
    }

    public WordClassCollection getWordPropertiesCollection() {
        return wordPropCollection;
    }

    /**
     * Clipboard can be used to hold any object
     *
     * @param c object to hold
     */
    public void setClipBoard(Object c) {
        clipBoard = c;
    }

    /**
     * Retrieves object held in clipboard, even if null, regardless of type
     *
     * @return contents of clipboard
     */
    public Object getClipBoard() {
        return clipBoard;
    }

    /**
     * gets family manager
     *
     * @return FamilyManager object from core
     */
    public FamilyManager getFamManager() {
        return famManager;
    }

//    public LogoCollection getLogoCollection() {
//        return logoCollection;
//    }

    public GrammarManager getGrammarManager() {
        return grammarManager;
    }

    /**
     * gets properties manager
     *
     * @return PropertiesManager object from core
     */
    public PropertiesManager getPropertiesManager() {
        return propertiesManager;
    }

    /**
     * gets version ID of PolyGlot
     *
     * @return String value of version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Used for getting the display version (potentially different than the internal version due to betas, etc.)
     * @return 
     */
    public String getDisplayVersion() {
        String ret = version;
        return ret;
    }

    /**
     * Gets lexicon manager
     *
     * @return ConWordCollection from core
     */
    public ConWordCollection getWordCollection() {
        return wordCollection;
    }

    /**
     * Reads from given file
     *
     * @param _fileName filename to read from
     * @throws java.io.IOException for unrecoverable errors
     * @throws IllegalStateException for recoverable errors
     * @throws java.awt.FontFormatException
     */
    public void readFile(String _fileName) throws IOException, IllegalStateException, FontFormatException {
        readFile(_fileName, null);
    }
    
    /**
     * Reads from given file
     *
     * @param _fileName filename to read from
     * @param overrideXML override to where the XML should be loaded from
     * @throws java.io.IOException for unrecoverable errors
     * @throws IllegalStateException for recoverable errors
     * @throws java.awt.FontFormatException
     */
    public void readFile(String _fileName, byte[] overrideXML) throws IOException, IllegalStateException, FontFormatException {
        curLoading = true;
        String errorLog = "";
        String warningLog = "";

        // test file exists
        if (!IOHandler.fileExists(_fileName)) {
            throw new IOException("File " + _fileName + " does not exist.");
        }
        
        // inform user if file is not an archive
        if (!IOHandler.isFileZipArchive(_fileName)) {
            throw new IOException("File " + _fileName + " is not a valid PolyGlot archive.");
        }

        // load image assets first to allow referencing as dictionary loads
        try {
            IOHandler.loadImageAssets(imageCollection, _fileName);
        } catch (Exception e) {
            throw new IOException("Image loading error: " + e.getLocalizedMessage());
        }

        PFontHandler.setFontFrom(_fileName, this);
        
        try {
            CustHandler handler;
            // if override XML value, load from that, otherwise pull from file
            if (overrideXML == null) {
                handler = IOHandler.getHandlerFromFile(_fileName, this);
                IOHandler.parseHandler(_fileName, handler);
            } else {
                handler = IOHandler.getHandlerFromByteArray(overrideXML, this);
                IOHandler.parseHandlerByteArray(overrideXML, handler);
            }
            
            errorLog += handler.getErrorLog();
            warningLog += handler.getWarningLog();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IOException(e.getMessage());
        }

        //logographs not currently printed to PDF
//        try {
//            logoCollection.loadRadicalRelations();
//        } catch (Exception e) {
//            IOHandler.writeErrorLog(e);
//            warningLog += e.getLocalizedMessage() + "\n";
//        }
//
//        try {
//            IOHandler.loadLogographs(logoCollection, _fileName);
//        } catch (Exception e) {
//            IOHandler.writeErrorLog(e);
//            warningLog += e.getLocalizedMessage() + "\n";
//        }
        
        IOHandler.loadReversionStates(reversionManager, _fileName);

        curLoading = false;

        if (errorLog.trim().length() != 0) {
            throw new IOException(errorLog);
        }

        if (warningLog.trim().length() != 0) {
            throw new IllegalStateException(warningLog);
        }
    }
    
    /**
     * loads revision XML from revision byte array (does not support media revisions)
     * @param revision 
     * @param fileName 
     * @throws java.io.IOException 
     */
    public void revertToState(byte[] revision, String fileName) throws IOException, Exception {
        DictCore revDict = new DictCore();
        revDict.readFile(fileName, revision);
    }
    
    /**
     * Used for test loading reversion XMLs. Cannot successfully load actual revision into functioning DictCore
     * @param reversion 
     * @return
     */
    public String testLoadReversion(byte[] reversion) {
        String errorLog;
        
        try {
            CustHandler handler = IOHandler.getHandlerFromByteArray(reversion, this);
            IOHandler.parseHandlerByteArray(reversion, handler);

            errorLog = handler.getErrorLog();
            // errorLog += handler.getWarningLog(); // warnings may be disregarded here
        } catch (IOException | ParserConfigurationException | SAXException e) {
            errorLog = e.getLocalizedMessage();
        }
        
        // if no save time present, simply timestamp for current time (only relevant for first time revision log added)
        if (lastSaveTime == Instant.MIN) {
            lastSaveTime = Instant.now();
        }
        
        return errorLog;
    }

    /**
     * Clears all declensions from word
     *
     * @param wordId ID of word to clear of all declensions
     */
    public void clearAllDeclensionsWord(Integer wordId) {
        declensionMgr.clearAllDeclensionsWord(wordId);
    }

    public DeclensionNode getDeclensionTemplate(Integer typeId, Integer templateId) {
        return declensionMgr.getDeclensionTemplate(typeId, templateId);
    }

    public DeclensionManager getDeclensionManager() {
        return declensionMgr;
    }

    public TypeCollection getTypes() {
        return typeCollection;
    }

    public PronunciationMgr getPronunciationMgr() {
        return pronuncMgr;
    }

    public RomanizationManager getRomManager() {
        return romMgr;
    }
    
    public EtymologyManager getEtymologyManager() {
        return etymologyManager;
    }
    
    public ReversionManager getReversionManager() {
        return reversionManager;
    }
    
    public ToDoManager getToDoManager() {
        return toDoManager;
    }
    
    public Instant getLastSaveTime() {
        return lastSaveTime;
    }

    public void setLastSaveTime(Instant _lastSaveTime) {
        lastSaveTime = _lastSaveTime;
    }
    
    /**
     * 
     * @param version
     * @return 
     */
    public int getVersionHierarchy(String version) {
        int ret = -1;
        
        if (versionHierarchy.containsKey(version)) {
            ret = versionHierarchy.get(version);
        }
        
        return ret;
    }
    
    private void validateVersion() throws Exception {
        if (!versionHierarchy.containsKey(this.getVersion())) {
            throw new Exception("ERROR: CURRENT VERSION NOT ACCOUNTED FOR IN VERSION HISTORY.");
        }
    }
    
    private void populateVersionHierarchy() {
        versionHierarchy.put("0", 0);
        versionHierarchy.put("0.5", 1);
        versionHierarchy.put("0.5.1", 2);
        versionHierarchy.put("0.6", 3);
        versionHierarchy.put("0.6.1", 4);
        versionHierarchy.put("0.6.5", 5);
        versionHierarchy.put("0.7", 6);
        versionHierarchy.put("0.7.5", 7);
        versionHierarchy.put("0.7.6", 8);
        versionHierarchy.put("0.7.6.1", 9);
        versionHierarchy.put("0.8", 10);
        versionHierarchy.put("0.8.1", 11);
        versionHierarchy.put("0.8.1.1", 12);
        versionHierarchy.put("0.8.1.2", 13);
        versionHierarchy.put("0.8.5", 14);
        versionHierarchy.put("0.9", 15);
        versionHierarchy.put("0.9.1", 16);
        versionHierarchy.put("0.9.2", 17);
        versionHierarchy.put("0.9.9", 18);
        versionHierarchy.put("0.9.9.1", 19);
        versionHierarchy.put("1.0", 20);
        versionHierarchy.put("1.0.1", 21);
        versionHierarchy.put("1.1", 22);
        versionHierarchy.put("1.2", 23);
        versionHierarchy.put("1.2.1", 24);
        versionHierarchy.put("1.2.2", 25);
        versionHierarchy.put("1.3", 26);
        versionHierarchy.put("1.4", 27);
        versionHierarchy.put("2.0", 28);
        versionHierarchy.put("2.1", 29);
        versionHierarchy.put("2.2", 30);
        versionHierarchy.put("2.3", 31);
        versionHierarchy.put("2.3.1", 32);
        versionHierarchy.put("2.3.2", 33);
        versionHierarchy.put("2.3.3", 34);
        versionHierarchy.put("2.4", 35);
        versionHierarchy.put("2.5", 36);
    }
}
