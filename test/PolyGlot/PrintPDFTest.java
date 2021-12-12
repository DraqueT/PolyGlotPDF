/*
 * Copyright (c) 2019-2021, Draque Thompson, draquemail@gmail.com
 * All rights reserved.
 *
 * Licensed under: Creative Commons Attribution-NonCommercial 4.0 International Public License
 * See LICENSE.TXT included with this code to read the full license agreement.

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

import java.io.File;
import org.junit.Test;

/**
 *
 * @author draque
 */
public class PrintPDFTest {
    
    OutputInterceptor interceptor;
    
    private final String testPdfPath = "test/TestResources/Lodenkur_TEST.pdf";

    public PrintPDFTest() {
        File f = new File(testPdfPath);
        cleanup();
        interceptor = new OutputInterceptor(System.out);
        System.setOut(interceptor);
        System.setErr(interceptor);
    }

    @Test
    public void testPrintGood() {
        try {
            String[] args = new String[]{
                "pdf-export",
                "test/TestResources/Lodenkur_TEST.pgd",
                testPdfPath,
                "Test Title Text",
                "Test Subtitle Text",
                "test/TestResources/EmptyImage.png",
                "Test Forward Text",
                "t",
                "t",
                "t",
                "t",
                "t",
                "t",
                "T",
                "T",
                "test/TestResources/Kukun.ttf",
                "X.XX",
                "t",
                "test/TestResources/Kukun.ttf",
                "0,1,2,3,4,5"
            };

            PolyGlot.main(args);

            String result = interceptor.getIntercepted();

            File f = new File(testPdfPath);
            assert(f.exists());
            assert(result.equals("SUCCESS"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    @Test
    public void testPrintTooManyArgs() throws Exception {
        cleanup();
        String[] args = new String[]{
            "pdf-export",
            "test/TestResources/Lodenkur_TEST.pgd",
            testPdfPath,
            "Test Title Text",
            "Test Subtitle Text",
            "test/TestResources/EmptyImage.png",
            "Test Forward Text",
            "true",
            "true",
            "true",
            "true",
            "true",
            "true",
            "TRUE",
            "True",
            "BLOO",
            "test/TestResources/Kukun.ttf",
            "X.XX",
            "ZIM ZAM MBAM!"
        };
        
        PolyGlot.main(args);
        
        OutputInterceptor errors = (OutputInterceptor)System.err;
        
        String result = errors.getIntercepted();

        File f = new File(testPdfPath);
        assert(!f.exists());
        assert(result.equals("ERROR: Wrong number of arguments given for comand.\nUsage: Consult internal documentation."));
        cleanup();
    }
    
    @Test
    public void testPrintTooFewArgs() throws Exception {
        cleanup();
        String[] args = new String[]{
            "pdf-export",
            "test/TestResources/Lodenkur_TEST.pgd",
            testPdfPath,
            "Test Title Text",
            "Test Subtitle Text",
            "test/TestResources/EmptyImage.png",
            "Test Forward Text"
        };
        
        PolyGlot.main(args);
        
        OutputInterceptor errors = (OutputInterceptor)System.err;
        
        String resultErr = errors.getIntercepted();

        File f = new File(testPdfPath);
        assert(!f.exists());
        assert(resultErr.equals("ERROR: Wrong number of arguments given for comand.\nUsage: Consult internal documentation."));
        cleanup();
    }
    
    @Test
    public void testPrintMissingFile() throws Exception {
        cleanup();
        String[] args = new String[]{
            "pdf-export",
            "test/TestResources/Lodenkur_TEST.pgdX",
            testPdfPath,
            "Test Title Text",
            "Test Subtitle Text",
            "test/TestResources/EmptyImage.png",
            "Test Forward Text",
            "true",
            "true",
            "true",
            "true",
            "true",
            "true",
            "TRUE",
            "True",
            "test/TestResources/Kukun.ttf",
            "X.XX",
            "true",
            "test/TestResources/Kukun.ttf",
            "0,1,2,3,4,5,6"
        };
        
        PolyGlot.main(args);
        
        OutputInterceptor errors = (OutputInterceptor)System.err;
        String result = errors.getIntercepted();

        File f = new File(testPdfPath);
        assert(!f.exists());
        assert(result.equals("ERROR: Unable to read PolyGlot file: test/TestResources/Lodenkur_TEST.pgdX"));
        cleanup();
    }
    
    private void cleanup() {
        File f = new File(testPdfPath);
        f.delete();
    }
}
