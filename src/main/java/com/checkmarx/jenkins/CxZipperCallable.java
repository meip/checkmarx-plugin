package com.checkmarx.jenkins;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.jenkinsci.remoting.RoleChecker;
import org.jetbrains.annotations.NotNull;

import com.checkmarx.components.zipper.ZipListener;
import com.checkmarx.components.zipper.Zipper;

/**
 * Creates zip file with source files.
 *
 * @author yevgenib
 * @since 11/03/14
 *
 */
public class CxZipperCallable implements FilePath.FileCallable<CxZipResult> {
    private static final long serialVersionUID = 1L;

    @NotNull
    private final String combinedFilterPattern;
    private int numOfZippedFiles;
    @NotNull
    private final StringBuffer logMessage = new StringBuffer();

    public CxZipperCallable(@NotNull String combinedFilterPattern){
        this.combinedFilterPattern = combinedFilterPattern;
        this.numOfZippedFiles = 0;
    }

    @Override
    public CxZipResult invoke(final File file, final VirtualChannel channel) throws IOException, InterruptedException {

        ZipListener zipListener = new ZipListener() {
            @Override
            public void updateProgress(String fileName, long size) {
                numOfZippedFiles++;
                logMessage.append("Zipping (" + FileUtils.byteCountToDisplaySize(size) + "): " + fileName + "\n");
            }
        };

        final File tempFile = File.createTempFile("base64ZippedSource", ".bin");
        final OutputStream fileOutputStream = new FileOutputStream(tempFile);
        final Base64OutputStream base64FileOutputStream = new Base64OutputStream(fileOutputStream,true,0,null);

        new Zipper().zip(file, combinedFilterPattern, base64FileOutputStream, CxConfig.maxZipSize(), zipListener);
        fileOutputStream.close();

        final FilePath remoteTempFile = new FilePath(tempFile);
        return new CxZipResult(remoteTempFile, numOfZippedFiles, logMessage.toString());
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        throw new NotImplementedException("");
    }
}
