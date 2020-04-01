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
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
/**
 *
 * @author draque
 */
public class ExportToExcelTest {
    private final DictCore core;
    private final String sourceFile;
    private final String targetFile;
    private final String checkAgainst;
    
    public ExportToExcelTest() throws IOException, IllegalStateException, FontFormatException {
        sourceFile = "test" + File.separator + "TestResources" + File.separator + "excel_exp_test.pgd";
        checkAgainst = "test" + File.separator + "TestResources" + File.separator + "excel_export_check.xls";
        targetFile = "test" + File.separator + "tmp.xls";
        core = new DictCore();
        core.readFile(sourceFile);
    }
    
    @Test
    public void testOutputGood() throws IOException {
        cleanup();
        System.out.println("Testing correct output");
        
        String[] args = {"export-to-excel",
            sourceFile,
            targetFile,
            PGTUtil.True
        };
        
        OutputInterceptor output = new OutputInterceptor(System.out);
        OutputInterceptor errors = new OutputInterceptor(System.err);
        System.setOut(output);
        System.setErr(errors);
        
        PolyGlot.main(args);
        
        String resultOut = output.getIntercepted();
        String resultErr = errors.getIntercepted();
        
        File resultFile = new File(targetFile);
        File expectedFile = new File(checkAgainst);
        assert(resultErr.equals(""));
        assert(resultOut.equals("SUCCESS"));
        assert(resultFile.exists());
        assert(expectedFile.exists());
        assert(FileUtils.contentEquals(expectedFile, resultFile)); // this fails due to user change sometimes?
        cleanup();
    }
    
    @Test
    public void testFileNotFound() throws IOException {
        cleanup();
        System.out.println("Testing correct output");
        
        String[] args = {"export-to-excel",
            sourceFile + "BADFILE",
            targetFile,
            PGTUtil.True
        };
        
        OutputInterceptor interceptor = new OutputInterceptor(System.out);
        System.setOut(interceptor);
        System.setErr(interceptor);
        
        PolyGlot.main(args);
        
        String result = interceptor.getIntercepted();
        
        File resultFile = new File(targetFile);
        assert(result.equals("ERROR: Unable to read PolyGlot file: test/TestResources/excel_exp_test.pgdBADFILE"));
        assert(!resultFile.exists());
        cleanup();
    }
    
    @Test
    public void testTooManyArguments() throws IOException {
        cleanup();
        System.out.println("Testing correct output");
        
        String[] args = {"export-to-excel",
            sourceFile,
            targetFile,
            PGTUtil.True,
            "ZOT"
        };
        
        OutputInterceptor output = new OutputInterceptor(System.out);
//        OutputInterceptor error = new OutputInterceptor(System.err);
        System.setOut(output);
//        System.setErr(error);
        
        PolyGlot.main(args);
        
        //String resultErr = error.getIntercepted();
        String resultErr = output.getIntercepted();
        
        File resultFile = new File(targetFile);
        assert(resultErr.startsWith("ERROR: Wrong number of arguments given for command."));
        assert(!resultFile.exists());
        cleanup();
    }
    
    @Test
    public void testTooFewArguments() throws IOException {
        cleanup();
        System.out.println("Testing correct output");
        
        String[] args = {"export-to-excel",
            sourceFile
        };
        
        OutputInterceptor interceptor = new OutputInterceptor(System.out);
        System.setOut(interceptor);
        System.setErr(interceptor);
        
        PolyGlot.main(args);
        
        String result = interceptor.getIntercepted();
        
        File resultFile = new File(targetFile);
        assert(result.startsWith("ERROR: Wrong number of arguments given for command."));
        assert(!resultFile.exists());
        cleanup();
    }
    
    private void cleanup() {
        File file = new File(targetFile);
        file.delete();
    }
}
