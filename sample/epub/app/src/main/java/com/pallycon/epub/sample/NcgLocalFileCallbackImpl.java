package com.pallycon.epub.sample;

import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2Agent.LicenseValidation;
import com.inka.ncg2.Ncg2Agent.NcgFile;
import com.inka.ncg2.Ncg2Agent.NcgFile.SeekMethod;
import com.inka.ncg2.Ncg2Agent.LocalFileCallback;
import com.inka.ncg2.Ncg2Agent.NcgLocalFileException;
import com.inka.ncg2.Ncg2Exception;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NcgLocalFileCallbackImpl implements LocalFileCallback {

    private Ncg2Agent ncg2Agent;
    private NcgFile ncgFile;

    private ZipFile zipFile;
    private ZipEntry zipEntry;
    private Enumeration<? extends ZipEntry> zipEntries;

    private BufferedInputStream bufferedInputStream;

    private long currentPosition = 0;

    private String epubPath;

    public class FileEntry {
        public void open() throws Ncg2Exception {
            try {
                LicenseValidation validation = ncg2Agent.checkLicenseValid(getName());
                if (validation != LicenseValidation.ValidLicense) {
                    // download license
                    ncg2Agent.acquireLicenseByToken("eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ0ZXN0VXNlciIsImRybV90eXBlIjoiTkNHIiwic2l0ZV9pZCI6IkRFTU8iLCJoYXNoIjoiMzN3emxXZDJpSm5obHA0MkpOVFFsSFozSXZFRUl6UUY0d2NjWWU2WDZJMD0iLCJjaWQiOiJ6aXB0ZXN0IiwicG9saWN5IjoiOVdxSVdrZGhweFZHSzhQU0lZY25Kc2N2dUE5c3hndWJMc2QrYWp1XC9ib21RWlBicUkreGFlWWZRb2Nja3Z1RWZBYXFkVzVoWGdKTmdjU1MzZlM3bzhOc2p3c3pqTXV2dCswUXpMa1pWVk5teDBrZWZPZTJ3Q3MyVElUZ2RVNEJ2TjlhYmhkMHJRa01Jcm1vSWVvSkhxSWVIY1J2VmY2VDE0Um1UQURFcENZN1BIZlBmXC9WRllcL1ZiWHV4WFwvV1R0VlNGSlNIOXN4cHdRSVFYcjVCNlIrQWFhZlNmVlhTS2s0bVZGbGUyUFwvcHJqaDU4K2JPaGJ0VTQ0NGx4OW9yZUc1IiwidGltZXN0YW1wIjoiMjAyMy0wNS0xMVQwMTo1NTowOVoifQ==", true);
                }

                ncgFile = ncg2Agent.createNcgFile();
                ncgFile.open(getName());
            } catch (Ncg2Exception e) {
                throw e;
            }
        }

        public int read(byte[] buffer) throws Ncg2Exception {
            // Get the actual decrypted data.
            int bytesRead = (int)ncgFile.read(buffer, buffer.length);
            return bytesRead;
        }

        public void close() {
            ncgFile.close();
        }

        public void seek(long offset, SeekMethod seekMethod) throws Ncg2Exception {
            ncgFile.seek(offset, seekMethod);
        }

        public long getCurrentFilePointer() throws Ncg2Exception {
            return ncgFile.getCurrentFilePointer();
        }

        public Boolean isDirectory() {
            return zipEntry.isDirectory();
        }

        public String getName() {return zipEntry.getName();}
    }

    private class FileEntryIterator implements Enumeration<FileEntry>, Iterator<FileEntry> {
        public FileEntryIterator() {
            zipEntries = zipFile.entries();
        }

        public boolean hasMoreElements() {
            return hasNext();
        }

        public boolean hasNext() {
            synchronized (NcgLocalFileCallbackImpl.this) {
                return zipEntries.hasMoreElements();
            }
        }

        public FileEntry nextElement() {
            return next();
        }

        public FileEntry next() {
            synchronized (NcgLocalFileCallbackImpl.this) {
                zipEntry = zipEntries.nextElement();
                return new FileEntry();
            }
        }
    }

    NcgLocalFileCallbackImpl(Ncg2Agent ncg2Agent, String epubPath) {
        this.ncg2Agent = ncg2Agent;
        this.epubPath = epubPath;
    }

    public Enumeration<? extends FileEntry> entries() {
        try {
            zipFile = new ZipFile(this.epubPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new FileEntryIterator();
    }

    @Override
    public Boolean fileOpen(String filePath, String openMode, int cloneNum) throws NcgLocalFileException {
        if (!filePath.equals(zipEntry.getName())) {
            return false;
        }

        try {
            InputStream decodeInputStream = zipFile.getInputStream(zipEntry);
            bufferedInputStream = new BufferedInputStream(decodeInputStream);
            currentPosition = 0;
        } catch (IOException e) {
            throw new NcgLocalFileException(e.getMessage());
        }

        return true;
    }

    @Override
    public void fileClose(int cloneNum) throws NcgLocalFileException {
        try {
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
                bufferedInputStream = null;
            }
        } catch (IOException e) {
            throw new NcgLocalFileException(e.getMessage());
        }
    }

    @Override
    public byte[] fileRead(long numBytes, int cloneNum) throws NcgLocalFileException {
        try {
            if (bufferedInputStream == null) {
                throw new NcgLocalFileException("is not open");
            }

            byte[] buffer = new byte[(int)numBytes];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Get the bytes of a real file
            int bytesRead = bufferedInputStream.read(buffer);
            outputStream.write(buffer, 0, bytesRead);
            currentPosition += bytesRead;

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new NcgLocalFileException(e.getMessage());
        }
    }

    /// Not used for EPUB playback.
    @Override
    public long fileWrite(byte[] data, long numBytes, int cloneNum) throws NcgLocalFileException {
        return 0;
    }

    @Override
    public long getFileSize(int cloneNum) throws NcgLocalFileException {
        return zipEntry.getSize();
    }

    @Override
    public long setFilePointer(long distanceToMove, int moveMethod, int cloneNum) throws NcgLocalFileException {
        try {
            if (bufferedInputStream == null) {
                throw new NcgLocalFileException("is not open");
            }

            if (distanceToMove < currentPosition) {
                bufferedInputStream.close();
                InputStream decodeInputStream = zipFile.getInputStream(zipEntry);
                bufferedInputStream = new BufferedInputStream(decodeInputStream);
                bufferedInputStream.skip(distanceToMove);
                currentPosition = distanceToMove;
            } else {
                long distance = distanceToMove - currentPosition;
                bufferedInputStream.skip(distance);
                currentPosition += distance;
            }

        } catch (IOException e) {
            throw new NcgLocalFileException(e.getMessage());
        }

        return currentPosition;
    }
}
