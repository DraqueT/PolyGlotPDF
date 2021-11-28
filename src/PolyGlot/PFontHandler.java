/*
 * Copyright (c) 2019-2021, Draque Thompson
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

import static PolyGlot.IOHandler.isFileZipArchive;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.swing.JLabel;
import org.apache.commons.io.FileUtils;
import org.mabb.fontverter.FVFont;
import org.mabb.fontverter.FontVerter;

/**
 * Split out from IOHandler due to large portion devoted to handling fonts
 *
 * @author Draque Thompson
 */
public class PFontHandler {

    /**
     * Sets the conlang and local lang fonts, if one exists and caches its file
     * for quicksaving
     *
     * @param _path The path of the PGD file
     * @param core the dictionary core
     * @throws java.io.IOException
     * @throws java.awt.FontFormatException
     */
    public static void setFontFrom(String _path, DictCore core) throws IOException, FontFormatException {
        setFontFrom(_path, core, true);
        setFontFrom(_path, core, false);
    }

    private static void setFontFrom(String _path, DictCore core, boolean isConFont) throws IOException, FontFormatException {
        if (!isConFont) { // PDF Printing does not currntly support local fonts, and it is causing trouble
            return;
        }

        if (isFileZipArchive(_path)) {
            try (ZipFile zipFile = new ZipFile(_path)) {
                ZipEntry fontEntry = isConFont
                        ? zipFile.getEntry(PGTUtil.CON_FONT_FILE_NAME)
                        : zipFile.getEntry(PGTUtil.LOCAL_FONT_FILE_NAME);

                if (fontEntry != null) {
                    final File tempFile = File.createTempFile("stream2file", ".tmp");

                    tempFile.deleteOnExit();

                    try (InputStream inputStream = zipFile.getInputStream(fontEntry)) {
                        Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);

                        if (font == null) {
                            return;
                        }

                        font = wrapFont(font);
                        if (isConFont) {
                            core.getPropertiesManager().setFontConRaw(font);
                        } else {
                            core.getPropertiesManager().setLocalFont(font);
                        }
                    } catch (FontFormatException e) {
                        throw new FontFormatException("Could not load language font: \""
                                + core.getPropertiesManager().getFontCon().getFontName() + "\".");
                    } catch (IOException e) {
                        throw new IOException("Could not load language font. I/O exception: " + e.getMessage());
                    }

                    try (InputStream inputStream = zipFile.getInputStream(fontEntry)) {
                        byte[] buffer = new byte[inputStream.available()];
                        inputStream.read(buffer);

                        if (isConFont) {
                            core.getPropertiesManager().setCachedFont(buffer);
                        } else {
                            core.getPropertiesManager().setCachedLocalFont(buffer);
                        }
                    } catch (IOException e) {
                        throw new IOException("Could not load language font. I/O exception: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Performs wrapping operations on fonts (such as enabling ligatures) and
     * returns the wrapped font
     *
     * @param font
     * @return
     */
    private static Font wrapFont(Font _font) {
        Font font = PGTUtil.addFontAttribute(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON, _font);
        return PGTUtil.addFontAttribute(TextAttribute.KERNING, TextAttribute.KERNING_ON, font);
    }

    public static byte[] getFontFileArray(Font font) throws Exception {
        return Files.readAllBytes(getFontFile(font).toPath());
    }

    /**
     * Attempts to load the given font from the OS's font folder (due to Java's
     * ligature problems)
     *
     * @param font
     * @return returns loaded font file on success, null otherwise
     */
    public static Font loadFontFromOSFileFolder(Font font) {
        Font ret = null;
        try {
            File fontFile = getFontFile(font);
            if (fontFile != null && fontFile.exists()) {
                ret = wrapFont(Font.createFont(Font.TRUETYPE_FONT, fontFile));
            }
        } catch (Exception e) {
            // do nothing here. Failure means returning null
        }

        return ret;
    }

    /**
     * gets the file of the current conlang font from the user's system
     *
     * @param font the font to find a file for
     * @return the font's file if found, null otherwise
     * @throws java.lang.Exception for unrecognized OS
     */
    public static File getFontFile(Font font) throws Exception {
        File ret = null;

        if (font == null) {
            return ret;
        }

        if (System.getProperty("os.name").startsWith("Mac")) {
            ret = PFontHandler.getFontFromLocation("/Library/Fonts/", font);

            if (ret == null) {
                ret = PFontHandler.getFontFromLocation("/System/Library/Fonts/", font);
            }

            if (ret == null) {
                ret = PFontHandler.getFontFromLocation(System.getProperty("user.home")
                        + "/Library/Fonts/", font);
            }
        } else if (System.getProperty("os.name").startsWith("Win")) {
            ret = getFontFromLocation(System.getenv("WINDIR") + "\\Fonts", font);
        } else if (System.getProperty("os.name").indexOf("nix") > 0
                || System.getProperty("os.name").indexOf("bunt") > 0
                || System.getProperty("os.name").indexOf("fed") > 0
                || System.getProperty("os.name").indexOf("nux") > 0) {
            ret = getFontFromLocation("/usr/share/fonts", font);
        } else {
            throw new Exception("Unknown OS; unable to retrieve font.");
        }

        return ret;
    }

    /**
     * Returns a font's file based on the font and a path Recurses on any
     * subdirectories found.
     *
     * In the case that multiple versions of the font are installed, the most
     * recently modified version will be defaulted to
     *
     * @param path path to check for a font
     * @param font font to check for
     * @return the font's file, null otherwise
     */
    private static File getFontFromLocation(String path, Font font) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        List<File> matches = new ArrayList<>();
        File ret = null;

        if (listOfFiles.length == 0) {
            return null;
        }

        // inspect all files and subdirectories to find matches
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                File fontFile = loadFont(listOfFile.getPath(), font);

                if (fontFile != null) {
                    matches.add(fontFile);
                }
            } else if (listOfFile.isDirectory()) {
                File fontFile = getFontFromLocation(path + "/"
                        + listOfFile.getName(), font);

                if (fontFile != null) {
                    matches.add(fontFile);
                }
            }
        }

        // return only most recently modified match
        for (File match : matches) {
            if (ret == null) {
                ret = match;
            } else {
                if (match.lastModified() > ret.lastModified()) {
                    ret = match;
                }
            }
        }

        return ret;
    }

    /**
     * compares testfont to loaded file. returns file if it represents the font
     *
     * @param path full path of file to test
     * @param testFont font to test against
     * @return file if path leads to passed font, null otherwise
     */
    private static File loadFont(String path, Font testFont) {
        File fontFile = new File(path);
        File ret = null;

        // unrecgnized types won't be loaded
        if (path.toLowerCase().endsWith(".ttf")
                || path.toLowerCase().endsWith(".otf")
                || path.toLowerCase().endsWith(".ttc")
                || path.toLowerCase().endsWith(".dfont")) {
            try {
                Font f = Font.createFont(Font.TRUETYPE_FONT, fontFile);

                // if names match, set ret to return file
                if (f.getFamily().equals(testFont.getFamily())
                        || f.getFamily().equals(testFont.getFamily())) {
                    ret = fontFile;
                }

            } catch (FontFormatException e) {
                // "Font name not found" errors due to Java bug (Java does not recognize some Mac style ttf fonts)
                // disabling logging until Java bug corrected.
                // IOHandler.writeErrorLog(e, path);
                // null detected and message bubbled to user elsewhere
                ret = null;
            } catch (IOException e) {
                // null detected and message bubbled to user elsewhere
                ret = null;
            }
        }

        return ret;
    }

    /**
     * Fetches and returns a font from a given location
     *
     * @param filePath
     * @return collected font
     * @throws java.awt.FontFormatException
     * @throws java.io.IOException
     */
    public static Font getFontFromFile(String filePath) throws FontFormatException, IOException {
        File fontFile = new File(filePath);
        return wrapFont(Font.createFont(Font.TRUETYPE_FONT, fontFile));
    }

    public static String writeFont(ZipOutputStream out, Font ouputFont, byte[] cachedFont, DictCore core, boolean isConFont) {
        String writeLog = "";
        try {
            // only search for font if the cached font is null
            if (cachedFont == null) {
                // embed font in PGD archive if applicable
                File fontFile = null;
                try {
                    fontFile = PFontHandler.getFontFile(ouputFont);
                } catch (Exception e) {
                    writeLog += "\nerror: " + e.getLocalizedMessage();
                }

                if (fontFile != null) {
                    try (FileInputStream fontInputStream = new FileInputStream(fontFile)) {
                        if (isConFont) {
                            core.getPropertiesManager().setCachedFont(IOHandler.inputStreamToByteArray(fontInputStream));
                        } else {
                            core.getPropertiesManager().setCachedLocalFont(IOHandler.inputStreamToByteArray(fontInputStream));
                        }
                    }
                    byte[] buffer = new byte[1024];
                    try (FileInputStream fis = new FileInputStream(fontFile)) {
                        if (isConFont) {
                            out.putNextEntry(new ZipEntry(PGTUtil.CON_FONT_FILE_NAME));
                        } else {
                            out.putNextEntry(new ZipEntry(PGTUtil.LOCAL_FONT_FILE_NAME));
                        }
                        int length;

                        while ((length = fis.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }

                        out.closeEntry();
                    }
                }
            } else {
                if (isConFont) {
                    out.putNextEntry(new ZipEntry(PGTUtil.CON_FONT_FILE_NAME));
                } else {
                    out.putNextEntry(new ZipEntry(PGTUtil.LOCAL_FONT_FILE_NAME));
                }
                out.write(cachedFont);
                out.closeEntry();
            }
        } catch (IOException e) {
            writeLog += "\nUnable to write font to archive: " + e.getMessage();
        }
        return writeLog;
    }

    /**
     * Fetches and returns default button font Should only be called a single
     * time (font then cached)
     *
     * @return Font to default buttons to
     * @throws java.io.IOException if unable to load font
     */
    public static Font getMenuFont() throws IOException {
        return new PFontHandler().getMenuFontInternal();
    }

    /**
     * Fetches and returns default button font nonstatic
     *
     * @return Font to default buttons to
     * @throws java.io.IOException if unable to load font
     */
    private Font getMenuFontInternal() throws IOException {
        return new JLabel().getFont();
    }
    
    public static void convertOtfToTtf(File source, File target) throws IOException {
        FVFont sourceFont = FontVerter.readFont(source);
        FVFont targetFont = FontVerter.convertFont(sourceFont, FontVerter.FontFormat.TTF);
        FileUtils.writeByteArrayToFile(target, targetFont.getData());
    }
}
