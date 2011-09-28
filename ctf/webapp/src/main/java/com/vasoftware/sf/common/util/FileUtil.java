/*
 * CollabNet TeamForge
 * Copyright 2010 CollabNet, Inc.  All rights reserved.
 * http://www.collab.net
 */

package com.vasoftware.sf.common.util;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * The <code>FileUtil</code> class provides utility methods for working with files.
 */
public class FileUtil {
    /**
     * Close file or stream or whatever closeable; ignore all exceptions
     * 
     * @param closeable
     *            input stream
     */
    public static void close(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final Throwable t) {
            // ignore
        }
    }

    /**
     * Create a regular file on the file system
     * 
     * @param file
     *            the File object for the file to create
     * @param contents
     *            the contents to put into the file
     * @throws IOException
     *             thrown if there was a problem creating the file.
     */
    public static void createFile(final File file, final String contents) throws IOException {
        final FileWriter out = new FileWriter(file, false);
        try {
            out.write(contents);
        } finally {
            out.close();
        }
    }

    /**
     * Recursively deletes a directory.
     * 
     * @param directory The file corresponding to a directory
     * 
     * @return Whether or not the recursive delete succeeded
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();

            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));

                if (!success) {
                    return false;
                }
            }
        }

        boolean isDeleted = dir.delete();
        if (!isDeleted && dir.exists()) {
            // Directories mounted on NFS volumes may have lingering .nfsXXXX files 
            // pointing to deleted files which are still referenced by the JVM.  We don't
            // explicitly have any handles open, however it appears some are created during
            // the listing above, or in some other unknown way. It seems it is possible to
            // free them by cleaning up stale objects, and giving the OS time to cleanup as 
            // well.
            System.gc();
            try {
                Thread.sleep(100);                
            } catch (InterruptedException e) {
                // ignored
            }
            isDeleted = dir.delete();
        }
        
        return isDeleted;
    }

    /**                                                                                                                                                                                                  
     * For the given <code>fileSystemPath</code>, split the string by the identified                                                                                                                     
     * path separator, not using {@link java.io.File#separator}, but by a process of                                                                                                                     
     * elimination, and return the last segment.  If no path separator can be found,                                                                                                                     
     * the <code>fileSystemPath</code> is returned as passed in.                                                                                                                                         
     *                                                                                                                                                                                                   
     * @param fileSystemPath The filesystem path to analyze                                                                                                                                              
     *                                                                                                                                                                                                   
     * @return The last segment in the filesystem path or the file system path pass in                                                                                                                   
     */                                                                                                                                                                                                  
    public static String getLastFileSystemPathSegment(String fileSystemPath) {                                                                                                                           
        char separator = '/';                                                                                                                                                                            
                                                                                                                                                                                                         
        if (fileSystemPath.indexOf('\\') > -1) {                                                                                                                                                         
            separator = '\\';                                                                                                                                                                            
        } else if (fileSystemPath.indexOf('/') == -1) {                                                                                                                                                  
            return fileSystemPath;                                                                                                                                                                       
        }                                                                                                                                                                                                
                                                                                                                                                                                                         
        return fileSystemPath.substring(fileSystemPath.lastIndexOf(separator) + 1);                                                                                                                      
    }

    /**                                                                                                                                                                                                  
     * Normalizes the filesystem paths so that we only see '/' for the                                                                                                                                   
     * path separator character regardless of host operating system.                                                                                                                                     
     *                                                                                                                                                                                                   
     * @param fileSystemPath The file system path to normalize                                                                                                                                           
     *                                                                                                                                                                                                   
     * @return The normalized version of the filesystem path                                                                                                                                             
     */                                                                                                                                                                                                  
    public static String normalizePath(String fileSystemPath) {                                                                                                                                          
        return fileSystemPath.replaceAll("\\\\", "/").replace("//", "/");                                                                                                                                
    }

    /**
     * Creates a tar.gz file at the specified path with the contents of the specified directory.
     * 
     * @param directoryToArchive The directory to create an archive of
     * @param archivePath The path to the archive to create
     * 
     * @throws IOException If anything goes wrong
     */
    public static void createTarGzOfDirectory(String dirPath, String tarGzPath) throws IOException {
        FileOutputStream fOut = null;
        BufferedOutputStream bOut = null;
        GzipCompressorOutputStream gzOut = null;
        TarArchiveOutputStream tOut = null;

        try {
            fOut = new FileOutputStream(new File(tarGzPath));
            bOut = new BufferedOutputStream(fOut);
            gzOut = new GzipCompressorOutputStream(bOut);
            tOut = new TarArchiveOutputStream(gzOut);

            addFileToTarGz(tOut, dirPath, "");
        } finally {
            tOut.finish();

            tOut.close();
            gzOut.close();
            bOut.close();
            fOut.close();
        }
    }

    /**
     * Creates an tar entry for the path specified with a name built from the base passed in and the file/directory
     * name.  If the path is a directory, a recursive call is made such that the full directory is added to the tar.
     * 
     * @param tOut The tar file's output stream
     * @param path The filesystem path of the file/directory being added
     * @param base The base prefix to for the name of the tar file entry
     * 
     * @throws IOException If anything goes wrong
     */
    private static void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base) throws IOException {
        File f = new File(path);
        String entryName = base + f.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);

        tOut.putArchiveEntry(tarEntry);

        if (f.isFile()) {
            IOUtils.copy(new FileInputStream(f), tOut);

            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();

            File[] children = f.listFiles();

            if (children != null) {
                for (File child : children) {
                    addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/");
                }
            }
        }
    }
}
