/*
 * Copyright (c) 2014 - 2019, Draque Thompson - draquemail@gmail.com
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

import PolyGlot.DictCore;
import PolyGlot.PGTUtil.WindowMode;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowStateListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * superclass for JFrame windows in PolyGlot. Includes setup instructions for
 * features like mac copy/paste in PolyGlot
 *
 * @author Draque
 */
public abstract class PFrame extends JFrame implements FocusListener, WindowFocusListener {

    protected DictCore core;
    protected WindowMode mode = WindowMode.STANDARD;
    protected int frameState = -1;
    private boolean curResizing;

    public PFrame() {
        this.addWindowStateListener(this::setWindowState);
    }

    @Override
    public final void addWindowStateListener(WindowStateListener listener) {
        super.addWindowStateListener(listener);
    }

    /**
     * Gets frame state of frame
     *
     * @return -1 for none set, otherwise Frame.ICONIFIED or
     * Frame.MAXIMIZED_BOTH
     */
    public Integer getFrameState() {
        return frameState;
    }

    /**
     * Used to set frame state of window
     *
     * @param e
     */
    private void setWindowState(WindowEvent e) {
        if ((e.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
            frameState = Frame.ICONIFIED;
        } else if ((e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            frameState = Frame.MAXIMIZED_BOTH;
        } else {
            frameState = -1;
        }
    }

    /**
     * Returns current running mode of window
     *
     * @return
     */
    public WindowMode getMode() {
        return mode;
    }

    /**
     * Returns whether target window can close. In implementation, all cases
     * where false is returned should also generate a pop-up explaining to the
     * user why it cannot close.
     *
     * @return true if can close. False otherwise.
     */
    public abstract boolean canClose();

    /**
     * Records all active/volatile values to core
     */
    public abstract void saveAllValues();

    /**
     * Forces window to update all relevant values from core
     *
     * @param _core current dictionary core
     */
    public abstract void updateAllValues(DictCore _core);

    /**
     * Get core from PFrame (used by custom PolyGlot elements)
     *
     * @return current dictionary core
     */
    public DictCore getCore() {
        return core;
    }

    public void setCore(DictCore _core) {
        core = _core;
    }

    public abstract void addBindingToComponent(JComponent c);

    @Override
    public void paint(Graphics g) {
        if (!curResizing) {
            super.paint(g);
        }
    }

    @Override
    public void paintComponents(Graphics g) {
        if (!curResizing) {
            super.paintComponents(g);
        }
    }

    @Override
    public void repaint() {
        if (!curResizing) {
            super.repaint();
        }
    }

    public void setCurResizing(boolean _resizing) {
        curResizing = _resizing;
    }

    /**
     * Smoothly resizes window with animation
     *
     * @param width new width of element
     * @param height new height of element
     * @param wait whether to wait on animation finishing before continuing
     * @throws java.lang.InterruptedException
     */
    public void setSizeSmooth(final int width, final int height, boolean wait) throws InterruptedException {
        final int numFrames = 20; // total number of frames to animate
        final int msDelay = 20; // ms delay between frames
        final int initialX = this.getWidth();
        final int initialY = this.getHeight();
        final float xDif = width - initialX;
        final float yDif = height - initialY;
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        setCurResizing(true);

        executorService.scheduleAtFixedRate(new Runnable() {
            int framesRun = 0;

            @Override
            public void run() {
                if (framesRun >= numFrames) {
                    PFrame.super.setSize(width, height);
                    setCurResizing(false);
                    repaint();
                    executorService.shutdown();
                    return;
                }

                float newX = initialX + (xDif / numFrames) * (framesRun + 1);
                float newY = initialY + (yDif / numFrames) * (framesRun + 1);
                PFrame.super.setSize((int) newX, (int) newY);

                framesRun++;
            }
        }, 0, msDelay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Forces fast dispose of window. Used primarily for testing.
     */
    public void hardDispose()  {
        super.dispose();
    }
            

    public abstract Component getWindow();
}
