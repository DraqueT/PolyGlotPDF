/*
 * Copyright (c) 2016-2018 Draque Thompson
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
package PolyGlot.CustomControls;

import PolyGlot.ClipboardHandler;
import PolyGlot.DictCore;
import PolyGlot.ExternalCode.GlyphVectorEditorKit;
import PolyGlot.Nodes.ImageNode;
import PolyGlot.PGTUtil;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Similar to the PTextPane, but compatible with the HTML stylings performed in
 * the grammar window
 *
 * @author draque.thompson
 */
public class PGrammarPane extends JTextPane {

    private final DictCore core;

    public PGrammarPane(DictCore _core) {
        core = _core;
        setEditorKit(new GlyphVectorEditorKit()); // TODO: This is causing access violations that will make trouble in Java 13+. Deal with it at some point.
        setupCopyPaste();
    }

    /**
     * Due to setting editor kit, need to create input map manually
     */
    private void setupCopyPaste() {
        if (System.getProperty("os.name").startsWith("Mac")) {
            int mask = KeyEvent.META_DOWN_MASK;

            InputMap im = this.getInputMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | mask), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | mask), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | mask), DefaultEditorKit.cutAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | mask), DefaultEditorKit.selectAllAction);
        }
    }

    public void addImage(ImageNode image) throws IOException, BadLocationException {
        MutableAttributeSet inputAttributes = getInputAttributes();
        inputAttributes.removeAttributes(inputAttributes);
        StyleConstants.setIcon(inputAttributes, new ImageIcon(image.getImagePath()));
        inputAttributes.addAttribute(PGTUtil.ImageIdAttribute, image.getId());
        replaceSelection(" ", false);
        inputAttributes.removeAttributes(inputAttributes);
    }

    /**
     * Captures images in override, passes everything else on to super.
     */
    @Override
    public void paste() {
        // might handle more types in the future
        try {
            if (ClipboardHandler.isClipboardImage()) {
                Object imageObject = ClipboardHandler.getClipboardImage();
                BufferedImage image;
                if (imageObject instanceof BufferedImage) {
                    image = (BufferedImage) imageObject;
                } else if (imageObject instanceof Image) {
                    Image imageImage = (Image) imageObject;
                    image = new BufferedImage(imageImage.getWidth(null),
                            imageImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);

                    // Draw the image on to the buffered image
                    Graphics2D bGr = image.createGraphics();
                    bGr.drawImage(imageImage, 0, 0, null);
                    bGr.dispose();
                } else {
                    throw new Exception("Unrecognized image format.");
                }
                ImageNode imageNode = core.getImageCollection().getFromBufferedImage(image);
                addImage(imageNode);
            } else if (ClipboardHandler.isClipboardString()) {
                // sanitize contents to plain text
                ClipboardHandler board = new ClipboardHandler();
                board.setClipboardContents(board.getClipboardText());
                super.paste();
            } else {
                super.paste();
            }
        } catch (Exception e) {
            System.out.println("WARNING: PGrammarPane Paste Failed");
        }
    }

    private void replaceSelection(String content, boolean checkEditable) throws BadLocationException {
        if (checkEditable && !isEditable()) {
            UIManager.getLookAndFeel().provideErrorFeedback(PGrammarPane.this);
            return;
        }
        Document doc = getStyledDocument();
        if (doc != null) {
            Caret caret = getCaret();
            boolean composedTextSaved = saveComposedText(caret.getDot());
            int p0 = Math.min(caret.getDot(), caret.getMark());
            int p1 = Math.max(caret.getDot(), caret.getMark());
            AttributeSet attr = getInputAttributes().copyAttributes();
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).replace(p0, p1 - p0, content, attr);
            } else {
                if (p0 != p1) {
                    doc.remove(p0, p1 - p0);
                }
                if (content != null && content.length() > 0) {
                    doc.insertString(p0, content, attr);
                }
            }
            if (composedTextSaved) {
                restoreComposedText();
            }
        }
    }
}
