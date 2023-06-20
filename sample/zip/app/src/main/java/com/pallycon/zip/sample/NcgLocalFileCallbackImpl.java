package com.pallycon.zip.sample;

import android.util.Log;

import com.inka.ncg2.Ncg2Agent;
import com.inka.ncg2.Ncg2Agent.LocalFileCallback;

public class NcgLocalFileCallbackImpl implements LocalFileCallback {

    @Override
    public Boolean fileOpen(String filePath, String openMode, int cloneNum) throws Ncg2Agent.NcgLocalFileException {
        return true;
    }

    @Override
    public void fileClose(int cloneNum) throws Ncg2Agent.NcgLocalFileException {
        Log.d("Ncg2Agent", "cloneNum");
    }

    @Override
    public byte[] fileRead(long numBytes, int cloneNum) throws Ncg2Agent.NcgLocalFileException {
        return new byte[0];
    }

    @Override
    public long fileWrite(byte[] data, long numBytes, int cloneNum) throws Ncg2Agent.NcgLocalFileException {
        return 0;
    }

    @Override
    public long getFileSize(int cloneNum) throws Ncg2Agent.NcgLocalFileException {
        return 0;
    }

    @Override
    public long setFilePointer(long distanceToMove, int moveMethod, int cloneNum) throws Ncg2Agent.NcgLocalFileException {
        return 0;
    }
}
