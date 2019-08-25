/*
 * Copyright (c) 2017-2019, Draque Thompson, draquemail@gmail.com
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
package PolyGlot.Screens;

import PolyGlot.CustomControls.InfoBox;
import PolyGlot.CustomControls.PFrame;
import PolyGlot.DictCore;
import PolyGlot.ExcelExport;
import PolyGlot.IOHandler;
import PolyGlot.PGTUtil;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Primary window for PolyGlot interface. Main running class that instantiates core and handles other windows/UI.
 * Depends on DictCore for all heavy logical lifting behind the scenes.
 *
 * @author draque.thompson
 */
public final class ScrMainMenu extends PFrame {

    private PFrame curWindow = null;
    private String curFileName = "";
    private List<Window> childWindows = new ArrayList<>();

    /**
     * Creates new form ScrMainMenu
     *
     * @param overridePath Path PolyGlot should treat as home directory (blank if default)
     */
    @SuppressWarnings("LeakingThisInConstructor") // only passing as later reference
    public ScrMainMenu(String overridePath) {
        super();
        core = new DictCore(); // needed for initialization
        core.setRootWindow(this);
        initComponents();

        newFile(false);
        core.getPropertiesManager().setOverrideProgramPath(overridePath);
        
        try {
            core.getOptionsManager().loadIni();
        } catch (Exception e) {
            IOHandler.writeErrorLog(e);
            InfoBox.error("Options Load Error", "Unable to load options file or file corrupted:\n"
                    + e.getLocalizedMessage(), core.getRootWindow());
            IOHandler.deleteIni(core);
        }
        
        populateRecentOpened();
        checkJavaVersion();
        super.setSize(super.getPreferredSize());
        addBindingsToPanelComponents(this.getRootPane());
    }
    
    /**
     * Warns user if they are using a beta version (based on beta warning file)
     */
    public void warnBeta() {
        if (IOHandler.fileExists("BETA_WARNING.txt")) {
            InfoBox.warning("BETA VERSION", "You are using a beta version of PolyGlot. Please proceed with caution!", this);
        }
    }


    @Override
    public void dispose() {
        // only exit if save/cancel test is passed and current window is legal to close
        if (!saveOrCancelTest() || (curWindow != null && !curWindow.canClose())) {
            return;
        }

        if (curWindow != null && !curWindow.isDisposed()) {
            // make certain that all actions necessary for saving information are complete
            curWindow.dispose();
        }
        
        killAllChildren();

        super.dispose();

        core.getOptionsManager().setScreenPosition(getClass().getName(), getLocation());
        core.getOptionsManager().setToDoBarPosition(pnlToDoSplit.getDividerLocation());
        
        try {
            core.getOptionsManager().saveIni();
        } catch (IOException e) {
            // save error likely due to inability to write to disk, disable logging
            // IOHandler.writeErrorLog(e);
            InfoBox.warning("INI Save Error", "Unable to save settings file on exit.", core.getRootWindow());
        }

        System.exit(0);
    }
    
    /**
     * Kills all children. Hiding under the covers won't save them.
     */
    private void killAllChildren() {
        for (Window child : childWindows) {
            child.dispose();
        }
        
        childWindows.clear();
    }

    /**
     * Checks to make certain Java is a high enough version. Informs user and quits otherwise.
     */
    private void checkJavaVersion() {
        String javaVersion = System.getProperty("java.version");

        if (javaVersion.startsWith("1.0")
                || javaVersion.startsWith("1.1")
                || javaVersion.startsWith("1.2")
                || javaVersion.startsWith("1.3")
                || javaVersion.startsWith("1.4")
                || javaVersion.startsWith("1.5")
                || javaVersion.startsWith("1.6")) {
            InfoBox.error("Please Upgrade Java", "Java " + javaVersion
                    + " must be upgraded to run PolyGlot. Version 1.7 or higher is required.\n\n"
                    + "Please upgrade at https://java.com/en/download/.", core.getRootWindow());
            System.exit(0);
        }
    }

    /**
     * Populates recently opened files menu
     */
    private void populateRecentOpened() {
        mnuRecents.removeAll();
        List<String> lastFiles = core.getOptionsManager().getLastFiles();

        for (int i = lastFiles.size() - 1; i >= 0; i--) {
            final String curFile = lastFiles.get(i);
            Path p = Paths.get(curFile);
            String fileName = p.getFileName().toString();
            JMenuItem lastFile = new JMenuItem();
            lastFile.setText(fileName);
            lastFile.setToolTipText(curFile);
            lastFile.addActionListener((java.awt.event.ActionEvent evt) -> {
                // only open if save/cancel test is passed
                if (!saveOrCancelTest()) {
                    return;
                }

                setFile(curFile);
                populateRecentOpened();
            });
            mnuRecents.add(lastFile);
        }
    }

    public void setFile(String fileName) {
        // some wrappers communicate empty files like this
        if (fileName.equals(PGTUtil.emptyFile)
                || fileName.isEmpty()) {
            return;
        }

        core = new DictCore(core);

        try {
            core.readFile(fileName);
            curFileName = fileName;
        } catch (IOException e) {
            IOHandler.writeErrorLog(e);
            core = new DictCore(core); // don't allow partial loads
            InfoBox.error("File Read Error", "Could not read file: " + fileName
                    + "\n\n " + e.getMessage(), core.getRootWindow());
        } catch (IllegalStateException e) {
            IOHandler.writeErrorLog(e);
            InfoBox.warning("File Read Problems", "Problems reading file:\n"
                    + e.getLocalizedMessage(), core.getRootWindow());
        }

        genTitle();
        updateAllValues(core);
    }

    /**
     * Gives user option to save file, returns continue/don't continue
     *
     * @return true to signal continue, false to signal stop
     */
    private boolean saveOrCancelTest() {
        // if there's a current dictionary loaded, prompt user to save before creating new
        if (core != null
                && !core.getWordCollection().getWordNodes().isEmpty()) {
            Integer saveFirst = InfoBox.yesNoCancel("Save First?",
                    "Save current dictionary before performing action?", core.getRootWindow());

            if (saveFirst == JOptionPane.YES_OPTION) {
                boolean saved = saveFile();

                // if the file didn't save (usually due to a last minute cancel) don't continue.
                if (!saved) {
                    return false;
                }
            } else if (saveFirst == JOptionPane.CANCEL_OPTION
                    || saveFirst == JOptionPane.DEFAULT_OPTION) {
                return false;
            }
        }

        return true;
    }

    /**
     * save file, open save as dialog if no file name already
     *
     * @return true if file saved, false otherwise
     */
    public boolean saveFile() {
        if (getCurFileName().length() == 0) {
            saveFileAs();
        }

        // if it still is blank, the user has hit cancel on the save as dialog
        if (getCurFileName().length() == 0) {
            return false;
        }
        
        core.getOptionsManager().pushRecentFile(getCurFileName());
        populateRecentOpened();
        saveAllValues();
        genTitle();
        return doWrite(getCurFileName());
    }

    /**
     * Writes the file by calling the core
     *
     * @param _fileName path to write to
     * @return returns success/failure
     */
    private boolean doWrite(final String _fileName) {
        boolean cleanSave = false;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        setCursor(Cursor.getDefaultCursor());

        if (cleanSave) {
            InfoBox.info("Success", "Dictionary saved to: "
                    + getCurFileName() + ".", core.getRootWindow());
        }

        return cleanSave;
    }

    /**
     * saves file as particular filename
     *
     * @return true if file saved, false otherwise
     */
    private boolean saveFileAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Dictionary");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PolyGlot Dictionaries", "pgd");
        chooser.setFileFilter(filter);
        chooser.setApproveButtonText("Save");
        if (curFileName.isEmpty()) {
            chooser.setCurrentDirectory(core.getPropertiesManager().getCannonicalDirectory());
        } else {
            chooser.setCurrentDirectory(IOHandler.getDirectoryFromPath(curFileName));
            chooser.setSelectedFile(IOHandler.getFileFromPath(curFileName));
        }

        String fileName;

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
        } else {
            return false;
        }

        // if user has not provided an extension, add one
        if (!fileName.contains(".pgd")) {
            fileName += ".pgd";
        }

        if (IOHandler.fileExists(fileName)) {
            Integer overWrite = InfoBox.yesNoCancel("Overwrite Dialog",
                    "Overwrite existing file? " + fileName, core.getRootWindow());

            if (overWrite == JOptionPane.NO_OPTION) {
                return saveFileAs();
            } else if (overWrite == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }

        curFileName = fileName;
        // TODO: refactor to single exit point
        return true;
    }

    /**
     * Creates new, blank language file
     *
     * @param performTest whether the UI ask for confirmation
     */
    final public void newFile(boolean performTest) {
        if (performTest && !saveOrCancelTest()) {
            return;
        }

        core = new DictCore(core);
        updateAllValues(core);
        curFileName = "";

        genTitle();
        updateAllValues(core);
    }

    public void genTitle() {
        String title = "PolyGlot";

        if (curWindow != null && curWindow.getTitle().length() != 0) {
            title += "-" + curWindow.getTitle();
            String langName = core.getPropertiesManager().getLangName();

            if (langName.length() != 0) {
                title += " : " + langName;
            } else if (getCurFileName().length() != 0) {
                title += " : " + getCurFileName();
            }
        }

        setTitle(title);
    }

    private void viewAbout() {
 
    }

    /**
     * opens dictionary file
     */
    public void open() {
        // only open if save/cancel test is passed
        if (!saveOrCancelTest()) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Dictionary");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PolyGlot Dictionaries", "pgd");
        chooser.setFileFilter(filter);
        String fileName;
        if (curFileName.isEmpty()) {
            chooser.setCurrentDirectory(core.getPropertiesManager().getCannonicalDirectory());
        } else {
            chooser.setCurrentDirectory(IOHandler.getDirectoryFromPath(curFileName));
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            core = new DictCore(core);
            setFile(fileName);
            core.getOptionsManager().pushRecentFile(fileName);
            populateRecentOpened();
        }

        genTitle();
    }

    /**
     * Export dictionary to excel file
     */
    private void exportToExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Dictionary to Excel");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel Files", "xls");
        chooser.setFileFilter(filter);
        chooser.setApproveButtonText("Save");
        chooser.setCurrentDirectory(core.getPropertiesManager().getCannonicalDirectory());

        String fileName;

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
        } else {
            return;
        }

        if (!fileName.contains(".xls")) {
            fileName += ".xls";
        }

        try {
            ExcelExport.exportExcelDict(fileName, core, 
                    InfoBox.actionConfirmation("Excel Export", 
                            "Export all declensions? (Separates parts of speech into individual tabs)", 
                            core.getRootWindow()));
            
            InfoBox.info("Export Status", "Dictionary exported to " + fileName + ".", core.getRootWindow());
        } catch (Exception e) {
            IOHandler.writeErrorLog(e);
            InfoBox.info("Export Problem", e.getLocalizedMessage(), core.getRootWindow());
        }
    }

    /**
     * Prompts user for a location and exports font within PGD to given path
     *
     * @param exportCharis set to true to export charis, false to export con font
     */
    public void exportFont(boolean exportCharis) {
        JFileChooser chooser = new JFileChooser();
        String fileName;
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Font Files", "ttf");

        chooser.setDialogTitle("Export Font");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File("."));
        chooser.setApproveButtonText("Save");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
        } else {
            return;
        }

        if (!fileName.contains(".")) {
            fileName += ".ttf";
        }
        
        if (IOHandler.fileExists(fileName) 
                && !InfoBox.actionConfirmation("Overwrite Confirmation", "File will be overwritten. Continue?", this)) {
            return;
        }

        try {
            if (exportCharis) {
                //IOHandler.exportCharisFont(fileName);
            } else {
                IOHandler.exportFont(fileName, getCurFileName());
            }
            InfoBox.info("Export Success", "Font exported to: " + fileName, core.getRootWindow());
        } catch (IOException e) {
            IOHandler.writeErrorLog(e);
            InfoBox.error("Export Error", "Unable to export font: " + e.getMessage(), core.getRootWindow());
        }
    }
    
    private void openHelp() {
        URI uri;
        try {
            String OS = System.getProperty("os.name");
            String overridePath = core.getPropertiesManager().getOverrideProgramPath();
            if (OS.startsWith("Windows")) {
                String relLocation = new File(".").getAbsolutePath();
                relLocation = relLocation.substring(0, relLocation.length() - 1);
                relLocation = "file:///" + relLocation + "readme.html";
                relLocation = relLocation.replaceAll(" ", "%20");
                relLocation = relLocation.replaceAll("\\\\", "/");
                uri = new URI(relLocation);
                uri.normalize();
                java.awt.Desktop.getDesktop().browse(uri);
            } else if (OS.startsWith("Mac")) {
                String relLocation;
                if (overridePath.length() == 0) {
                    relLocation = new File(".").getAbsolutePath();
                    relLocation = relLocation.substring(0, relLocation.length() - 1);
                    relLocation = "file://" + relLocation + "readme.html";
                } else {
                    relLocation = core.getPropertiesManager().getOverrideProgramPath();
                    relLocation = "file://" + relLocation + "/Contents/Resources/readme.html";
                }
                relLocation = relLocation.replaceAll(" ", "%20");
                uri = new URI(relLocation);
                uri.normalize();
                java.awt.Desktop.getDesktop().browse(uri);
            } else {
                // TODO: Implement this for Linux
                InfoBox.error("Help", "This is not yet implemented for OS: " + OS
                        + ". Please open readme.html in the application directory", core.getRootWindow());
            }
        } catch (URISyntaxException | IOException e) {
            // no need to log this.
            // IOHandler.writeErrorLog(e);
            InfoBox.error("Missing File", "Unable to open readme.html.", core.getRootWindow());
        }
    }
    
    @Override
    public void updateAllValues(DictCore _core) {
        if (curWindow != null) {
            curWindow.updateAllValues(_core);
        }
        core = _core;
    }

    @Override
    public void addBindingToComponent(JComponent c) {
        // none for this window
    }

    @Override
    public Component getWindow() {
        throw new UnsupportedOperationException("The main window never returns a value here. Do not call this.");
    }

    /**
     * For now, always returns true... shouldn't ever be any upstream window, regardless.
     *
     * @return
     */
    @Override
    public boolean canClose() {
        return true;
    }
    
    public String getCurFileName() {
        return curFileName;
    }
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem2 = new javax.swing.JMenuItem();
        pnlToDoSplit = new javax.swing.JSplitPane();
        pnlToDo = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        pnlSideButtons = new javax.swing.JPanel();
        pnlMain = new javax.swing.JPanel() {

        };
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        mnuNewLocal = new javax.swing.JMenuItem();
        mnuOpenLocal = new javax.swing.JMenuItem();
        mnuRecents = new javax.swing.JMenu();
        mnuPublish = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mnuExit = new javax.swing.JMenuItem();

        jMenuItem2.setText("jMenuItem2");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PolyGlot Language Construction Toolkit");
        setBackground(new java.awt.Color(255, 255, 255));
        setMaximumSize(new java.awt.Dimension(4000, 4000));

        pnlToDoSplit.setBackground(new java.awt.Color(255, 255, 255));
        pnlToDoSplit.setDividerLocation(675);
        pnlToDoSplit.setDividerSize(10);

        pnlToDo.setBackground(new java.awt.Color(255, 255, 255));
        pnlToDo.setToolTipText("");
        pnlToDo.setMinimumSize(new java.awt.Dimension(1, 1));

        javax.swing.GroupLayout pnlToDoLayout = new javax.swing.GroupLayout(pnlToDo);
        pnlToDo.setLayout(pnlToDoLayout);
        pnlToDoLayout.setHorizontalGroup(
            pnlToDoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 117, Short.MAX_VALUE)
        );
        pnlToDoLayout.setVerticalGroup(
            pnlToDoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 430, Short.MAX_VALUE)
        );

        pnlToDoSplit.setRightComponent(pnlToDo);

        pnlSideButtons.setBackground(new java.awt.Color(102, 204, 255));
        pnlSideButtons.setMaximumSize(new java.awt.Dimension(4000, 4000));

        javax.swing.GroupLayout pnlSideButtonsLayout = new javax.swing.GroupLayout(pnlSideButtons);
        pnlSideButtons.setLayout(pnlSideButtonsLayout);
        pnlSideButtonsLayout.setHorizontalGroup(
            pnlSideButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 173, Short.MAX_VALUE)
        );
        pnlSideButtonsLayout.setVerticalGroup(
            pnlSideButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 430, Short.MAX_VALUE)
        );

        pnlMain.setBackground(new java.awt.Color(255, 255, 255));
        pnlMain.setMaximumSize(new java.awt.Dimension(4000, 4000));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Welcome to PolyGlot");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("From here, you can open an existing language file, open the PolyGlot manual,");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("or simply begin work on the blank file currently loaded.");

        javax.swing.GroupLayout pnlMainLayout = new javax.swing.GroupLayout(pnlMain);
        pnlMain.setLayout(pnlMainLayout);
        pnlMainLayout.setHorizontalGroup(
            pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pnlMainLayout.setVerticalGroup(
            pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMainLayout.createSequentialGroup()
                .addContainerGap(129, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addGap(165, 165, 165))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(pnlSideButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlSideButtons, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnlMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pnlToDoSplit.setLeftComponent(jPanel3);

        jMenuBar1.setOpaque(false);

        jMenu1.setText("File");

        mnuNewLocal.setText("New");
        mnuNewLocal.setToolTipText("New PolyGlot Language File");
        mnuNewLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuNewLocalActionPerformed(evt);
            }
        });
        jMenu1.add(mnuNewLocal);

        mnuOpenLocal.setText("Open");
        mnuOpenLocal.setToolTipText("Open Existing Language File");
        mnuOpenLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuOpenLocalActionPerformed(evt);
            }
        });
        jMenu1.add(mnuOpenLocal);

        mnuRecents.setText("Recent");
        mnuRecents.setToolTipText("Recently Opened Language Files");
        jMenu1.add(mnuRecents);

        mnuPublish.setText("Publish Language to PDF");
        mnuPublish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuPublishActionPerformed(evt);
            }
        });
        jMenu1.add(mnuPublish);
        jMenu1.add(jSeparator2);

        mnuExit.setText("Exit");
        mnuExit.setToolTipText("Exit PolyGlot");
        mnuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuExitActionPerformed(evt);
            }
        });
        jMenu1.add(mnuExit);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlToDoSplit, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlToDoSplit)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void mnuNewLocalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuNewLocalActionPerformed
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        newFile(true);
        setCursor(Cursor.getDefaultCursor());
    }//GEN-LAST:event_mnuNewLocalActionPerformed

    private void mnuOpenLocalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuOpenLocalActionPerformed
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        open();
        setCursor(Cursor.getDefaultCursor());
        updateAllValues(core);
    }//GEN-LAST:event_mnuOpenLocalActionPerformed

    private void mnuPublishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuPublishActionPerformed
        ScrPrintToPDF.run(core);
    }//GEN-LAST:event_mnuPublishActionPerformed

    private void mnuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExitActionPerformed
        dispose();
    }//GEN-LAST:event_mnuExitActionPerformed

    public void showOptions() {

    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JMenuItem mnuExit;
    private javax.swing.JMenuItem mnuNewLocal;
    private javax.swing.JMenuItem mnuOpenLocal;
    private javax.swing.JMenuItem mnuPublish;
    private javax.swing.JMenu mnuRecents;
    private javax.swing.JPanel pnlMain;
    private javax.swing.JPanel pnlSideButtons;
    private javax.swing.JPanel pnlToDo;
    private javax.swing.JSplitPane pnlToDoSplit;
    // End of variables declaration//GEN-END:variables

    @Override
    public void saveAllValues() {
        
    }
}
