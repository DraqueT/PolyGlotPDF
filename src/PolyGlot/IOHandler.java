/*
 * Copyright (c) 2014-2020, Draque Thompson, draquemail@gmail.com
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

import PolyGlot.ManagersCollections.ImageCollection;
import PolyGlot.ManagersCollections.ReversionManager;
import PolyGlot.Nodes.ImageNode;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * This class handles file IO for PolyGlot
 *
 * @author draque
 */
public class IOHandler {

    /**
     * Opens and returns image from URL given (can be file path)
     *
     * @param filePath path of image
     * @return BufferedImage
     * @throws IOException in IO
     */
    public static BufferedImage getImage(String filePath) throws IOException {
        return ImageIO.read(new File(filePath));
    }
    
    public static File createTmpFileFromImageBytes(byte[] imageBytes, String fileName) throws IOException {
        File tmpFile = File.createTempFile(fileName, ".png");
        ByteArrayInputStream stream = new ByteArrayInputStream(imageBytes);
        BufferedImage img = ImageIO.read(stream);
        ImageIO.write(
                img, 
                "PNG", 
                new FileOutputStream(tmpFile)
        );
        return tmpFile;
    }

    /**
     * Queries user for image file, and returns it
     *
     * @param parent the parent window from which this is called
     * @return the image chosen by the user, null if canceled
     * @throws IOException If the image cannot be opened for some reason
     */
    public static BufferedImage openImageFile(Component parent) throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Images");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "gif", "jpg", "jpeg", "bmp", "png", "wbmp");
        chooser.setFileFilter(filter);
        String fileName;
        chooser.setCurrentDirectory(new File("."));

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
        } else {
            return null;
        }

        return getImage(fileName);
    }

    /**
     * Creates a temporary file with the contents handed in the arguments and
     * returns its URL location.
     *
     * @param contents Contents to put in file.
     * @return
     * @throws java.io.IOException
     * @throws java.net.URISyntaxException
     */
    public static URI createTmpURL(String contents) throws IOException, URISyntaxException {
        File tmpFile = File.createTempFile("POLYGLOT", ".html");
        Files.write(tmpFile.toPath(), contents.getBytes());
        tmpFile.deleteOnExit();
        URI ret = createURIFromFullPath(tmpFile.getAbsolutePath());

        return ret;
    }

    public static URI createURIFromFullPath(String path) throws URISyntaxException, IOException {
        String OS = System.getProperty("os.name");
        URI uri;

        if (OS.startsWith("Windows")) {
            String relLocation = path;
            relLocation = "file:///" + relLocation;
            relLocation = relLocation.replaceAll(" ", "%20");
            relLocation = relLocation.replaceAll("\\\\", "/");
            uri = new URI(relLocation);
        } else if (OS.startsWith("Mac")) {
            String relLocation;
            relLocation = path;
            relLocation = "file://" + relLocation;
            relLocation = relLocation.replaceAll(" ", "%20");
            uri = new URI(relLocation);
        } else {
            // TODO: Implement this for Linux
            throw new IOException("This is not yet implemented for OS: " + OS
                    + ". Please open readme.html in the application directory");
        }
        
        return uri;
    }

    public static byte[] getByteArrayFromFile(File file) throws FileNotFoundException, IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return inputStreamToByteArray(inputStream);
        }
    }
    
    /**
     * Used for snagging cachable versions of files
     * @param filePath path of file to fetch as byte array
     * @return byte array of file at given path
     * @throws java.io.FileNotFoundException
     */
    public static byte[] getFileByteArray(String filePath) throws FileNotFoundException, IOException {
        byte[] ret;
        final File toByteArrayFile = new File(filePath);
        
        try (InputStream inputStream = new FileInputStream(toByteArrayFile)) {
            ret = inputStreamToByteArray(inputStream);
        }
        
        return ret;
    }
    
    /**
     * Given file name, returns appropriate cust handler
     *
     * @param _fileName full path of target file to read
     * @param _core dictionary core
     * @return cushandler class
     * @throws java.io.IOException on read problem
     */
    public static CustHandler getHandlerFromFile(String _fileName, DictCore _core) throws IOException {
        CustHandler ret = null;

        if (IOHandler.isFileZipArchive(_fileName)) {
            try (ZipFile zipFile = new ZipFile(_fileName)) {
                ZipEntry xmlEntry = zipFile.getEntry(PGTUtil.LANG_FILE_NAME);
                try (InputStream ioStream = zipFile.getInputStream(xmlEntry)) {
                    ret = CustHandlerFactory.getCustHandler(ioStream, _core);
                } catch (Exception e) {
                    throw new IOException(e.getLocalizedMessage());
                }
            }
        } else {
            try (InputStream ioStream = new FileInputStream(_fileName)) {
                ret = CustHandlerFactory.getCustHandler(ioStream, _core);
            } catch (Exception e) {
                throw new IOException(e.getLocalizedMessage());
            }
        }

        return ret;
    }
    
    /**
     * Creates a custhandler object from a reversion byte array of a language state
     * @param byteArray byte array containing XML of language state
     * @param _core dictionary core
     * @return new custhandler class
     * @throws IOException on parse error
     */
    public static CustHandler getHandlerFromByteArray(byte[] byteArray, DictCore _core) throws IOException {
        try {
            return CustHandlerFactory.getCustHandler(new ByteArrayInputStream(byteArray), _core);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }

    /**
     * Opens an image via GUI and returns as buffered image Returns null if user
     * cancels.
     *
     * @param parent parent window of operation
     * @return buffered image selected by user
     * @throws IOException on file read error
     */
    public static BufferedImage openImage(Window parent) throws IOException {
        BufferedImage ret = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Image");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "tiff", "bmp", "png");
        chooser.setFileFilter(filter);
        String fileName;
        chooser.setCurrentDirectory(new File("."));

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            ret = ImageIO.read(new File(fileName));
        }

        return ret;
    }

    /**
     * returns name of file sans path
     *
     * @param fullPath full path to file
     * @return string of filename
     */
    public static String getFilenameFromPath(String fullPath) {
        File file = new File(fullPath);
        return file.getName();
    }
    
    /**
     * Deletes options file
     * @param core 
     * @throws java.net.URISyntaxException 
     */
    public static void deleteIni(DictCore core) throws URISyntaxException {
        // DUMMY
    }

    /**
     * Loads all option data from ini file, if none, ignore. One will be created
     * on exit.
     *
     * @param core dictionary core
     * @throws IOException on failure to open existing file
     */
    public static void loadOptionsIni(DictCore core) throws Exception {
        // DUMMY
    }

    /**
     * Given handler class, parses XML document within file (archive or not)
     *
     * @param _fileName full path of target file
     * @param _handler custom handler to consume XML document
     * @throws IOException on read error
     * @throws ParserConfigurationException on parser factory config error
     * @throws SAXException on XML interpretation error
     */
    public static void parseHandler(String _fileName, CustHandler _handler)
            throws IOException, ParserConfigurationException, SAXException {
        try (ZipFile zipFile = new ZipFile(_fileName)) {
            ZipEntry xmlEntry = zipFile.getEntry(PGTUtil.LANG_FILE_NAME);
            try (InputStream ioStream = zipFile.getInputStream(xmlEntry)) {
                parseHandlerInternal(ioStream, _handler);
            }
        }
    }
    
    public static void parseHandlerByteArray(byte[] reversion, CustHandler _handler) 
            throws ParserConfigurationException, IOException, SAXException {
        parseHandlerInternal(new ByteArrayInputStream(reversion), _handler);
    }
    
    private static void parseHandlerInternal(InputStream stream, CustHandler _handler) 
            throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(stream, _handler);
    }

    /**
     * Tests whether or not a file is a zip archive
     *
     * @param _fileName the file to test
     * @return true is passed file is a zip archive
     * @throws java.io.FileNotFoundException
     */
    public static boolean isFileZipArchive(String _fileName) throws FileNotFoundException, IOException {
        File file = new File(_fileName);

        // ignore directories and files too small to possibly be archives
        if (file.isDirectory()
                || file.length() < 4) {
            return false;
        }

        int test;
        try (FileInputStream fileStream = new FileInputStream(file)) {
            try (BufferedInputStream buffer = new BufferedInputStream(fileStream)) {
                try (DataInputStream in = new DataInputStream(buffer)) {
                    test = in.readInt();
                }
            }
        }
        return test == 0x504b0304;
    }

    /**
     * Returns the default path of PolyGlot's running directory NOTE: If the
     * path is overridden, in the properties manager, use that. This returns
     * only what the OS tells PolyGlot it is running under (not always
     * trustworthy)
     *
     * @return default path
     */
    public static File getBaseProgramPath() {
        return new File(".");
    }

    /**
     * Tests whether a file at a particular location exists. Wrapped to avoid IO
     * code outside this file
     *
     * @param fullPath path of file to test
     * @return true if file exists, false otherwise
     */
    public static boolean fileExists(String fullPath) {
        File f = new File(fullPath);
        return f.exists();
    }

    /**
     * Loads image assets from file. Does not load logographs due to legacy
     * coding/logic
     *
     * @param imageCollection from dictCore to populate
     * @param fileName of file containing assets
     * @throws java.io.IOException
     */
    public static void loadImageAssets(ImageCollection imageCollection,
            String fileName) throws IOException, Exception {
        try (ZipFile zipFile = new ZipFile(fileName)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            ZipEntry entry;
            while (entries.hasMoreElements()) { // find images directory (zip paths are linear, only simulating tree structure)
                entry = entries.nextElement();
                if (!entry.getName().equals(PGTUtil.IMAGES_SAVE_PATH)) {
                    continue;
                }
                break;
            }

            while (entries.hasMoreElements()) {
                entry = entries.nextElement();

                if (entry.isDirectory()) { // kills process after last image found
                    break;
                }

                BufferedImage img;
                try (InputStream imageStream = zipFile.getInputStream(entry)) {
                    String name = entry.getName().replace(".png", "")
                            .replace(PGTUtil.IMAGES_SAVE_PATH, "");
                    int imageId = Integer.parseInt(name);
                    img = ImageIO.read(imageStream);
                    ImageNode imageNode = new ImageNode(imageCollection.getCore());
                    imageNode.setId(imageId);
                    imageNode.setImageBytes(loadImageBytesFromImage(img));
                    imageCollection.getBuffer().setEqual(imageNode);
                    imageCollection.insert(imageId);
                }
            }
        }
    }
    
    public static byte[] loadImageBytesFromImage(Image img) throws IOException {
        BufferedImage image = PGTUtil.toBufferedImage(img);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write( image, "png", baos );
        baos.flush();
        return baos.toByteArray();
    }

    /**
     * Encapsulates image loading to keep IO within IOHandler class
     *
     * @param fileName path of image
     * @return buffered image loaded
     * @throws IOException on file read error
     */
    public BufferedImage fetchImageFromLocation(String fileName) throws IOException {
        try (InputStream imageStream = new FileInputStream(fileName)) {
            return ImageIO.read(imageStream);
        }
    }

    public static byte[] getBufferedImageByteArray(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }

    /**
     * loads all images into their logographs from archive and images into the
     * generalized image collection
     *
     * @param logoCollection logocollection from dictionary core
     * @param fileName name/path of archive
     * @throws java.lang.Exception
     */
//    public static void loadLogographs(LogoCollection logoCollection,
//            String fileName) throws Exception {
//        Iterator<LogoNode> it = logoCollection.getAllLogos().iterator();
//        try (ZipFile zipFile = new ZipFile(fileName)) {
//            while (it.hasNext()) {
//                LogoNode curNode = it.next();
//                ZipEntry imgEntry = zipFile.getEntry(PGTUtil.logoGraphSavePath
//                        + curNode.getId().toString() + ".png");
//
//                BufferedImage img;
//                try (InputStream imageStream = zipFile.getInputStream(imgEntry)) {
//                    img = ImageIO.read(imageStream);
//                }
//                curNode.setLogoGraph(img);
//            }
//        }
//    }
    
    /**
     * Loads all reversion XML files from polyglot archive
     * @param reversionManager reversion manager to load to
     * @param fileName full path of polyglot archive
     * @throws IOException on read error
     */
    public static void loadReversionStates(ReversionManager reversionManager,
            String fileName) throws IOException {
        try (ZipFile zipFile = new ZipFile(fileName)) {
            Integer i = 0;
            
            ZipEntry reversion = zipFile.getEntry(PGTUtil.REVERSION_SAVE_PATH
                    + PGTUtil.REVERSION_BASE_FILE_NAME + i.toString());
            
            while (reversion != null && i < reversionManager.getMaxReversionsCount()) {
                reversionManager.addVersionToEnd(inputStreamToByteArray(zipFile.getInputStream(reversion)));
                i++;
                reversion = zipFile.getEntry(PGTUtil.REVERSION_SAVE_PATH
                        + PGTUtil.REVERSION_BASE_FILE_NAME + i.toString());
            }
            
            // remember to load latest state in addition to all prior ones
            reversion = zipFile.getEntry(PGTUtil.LANG_FILE_NAME);
            reversionManager.addVersionToEnd(inputStreamToByteArray(zipFile.getInputStream(reversion)));
        }
    }

    /**
     * Exports font in PGD to external file
     *
     * @param exportPath path to export to
     * @param dictionaryPath path of PGT dictionary
     * @throws IOException
     */
    public static void exportFont(String exportPath, String dictionaryPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(dictionaryPath)) {
            if (zipFile == null) {
                throw new IOException("Dictionary must be saved before font can be exported.");
            }

            // ensure export file has the proper extension
            if (!exportPath.toLowerCase().endsWith(".ttf")) {
                exportPath += ".ttf";
            }

            ZipEntry fontEntry = zipFile.getEntry(PGTUtil.CON_FONT_FILE_NAME);

            if (fontEntry != null) {
                Path path = Paths.get(exportPath);
                try (InputStream copyStream = zipFile.getInputStream(fontEntry)) {
                    Files.copy(copyStream, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                throw new IOException("Custom font not found in PGD dictionary file.");
            }
        }
    }

    /**
     * Tests whether the path can be written to
     *
     * @param path
     * @return
     */
    private static boolean testCanWrite(String path) {
        return new File(path).canWrite();
    }

    /**
     * Saves ini file with polyglot options
     *
     * @param core
     * @throws IOException on failure or lack of permission to write
     * @throws java.net.URISyntaxException
     */
    public static void saveOptionsIni(DictCore core) throws IOException, URISyntaxException {
        // DUMMY
    }

    /**
     * Opens an arbitrary file via the local OS's default. If unable to open for
     * any reason, returns false.
     *
     * @param path
     * @return
     */
    public static boolean openFileNativeOS(String path) {
        boolean ret = true;

        try {
            File file = new File(path);
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            // internal logic based on thrown exception due to specific use case. No logging required.
            // IOHandler.writeErrorLog(e);
            ret = false;
        }

        return ret;
    }
    
    /**
     * Returns deepest directory from given path (truncating non-directory files from the end)
     * @param path path to fetch directory from
     * @return File representing directory, null if unable to capture directory path for any reason
     */
    public static File getDirectoryFromPath(String path) {
        File ret = new File(path);
        
        if (ret.exists()) {
            while (ret != null && ret.exists() && !ret.isDirectory()) {
                ret = ret.getParentFile();
            }
        }
        
        if (!ret.exists()) {
            ret = null;
        }
        
        return ret;
    }
    
    /**
     * Wraps File so that I can avoid importing it elsewhere in code
     * @param path path to file
     * @return file
     */
    public static File getFileFromPath(String path) {
        return new File(path);
    }
    
    /**
     * Gets system information in human readable format
     * @return system information
     */
    public static String getSystemInformation() {
        List<String> attributes = Arrays.asList("java.version",
            "java.vendor",
            "java.vendor.url",
            "java.vm.specification.version",
            "java.vm.specification.name",
            "java.vm.version",
            "java.vm.vendor",
            "java.vm.name",
            "java.specification.version",
            "java.specification.vendor",
            "java.specification.name",
            "java.class.version",
            "java.ext.dirs",
            "os.name",
            "os.arch",
            "os.version");
        String ret = "";
        
        for (String attribute : attributes) {
            ret += attribute + " : " + System.getProperty(attribute) + "\n";
        }
        
        return ret;
    }
    
    public static String getErrorLog() throws FileNotFoundException {
        return "DUMMY";
    }
    
    /**
     * Gets Unicode compatible font as byte array
     *
     * @return byte array of font's file
     * @throws FileNotFoundException if this throws, something is wrong
     * internally
     * @throws IOException if this throws, something is wrong internally
     */
    public byte[] getUnicodeFontByteArray() throws FileNotFoundException, IOException {
        try (InputStream localStream = this.getClass().getResourceAsStream(PGTUtil.UNICODE_FONT_LOCATION)) {
            return inputStreamToByteArray(localStream);
        }
    }

    /**
     * Gets Unicode compatible font as byte array
     *
     * @return byte array of font's file
     * @throws FileNotFoundException if this throws, something is wrong
     * internally
     * @throws IOException if this throws, something is wrong internally
     */
    public byte[] getUnicodeFontItalicByteArray() throws FileNotFoundException, IOException {
        try (InputStream localStream = this.getClass().getResourceAsStream(PGTUtil.UNICODE_FONT_ITALIC_LOCATION)) {
            return inputStreamToByteArray(localStream);
        }
    }
    
    public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }
}
