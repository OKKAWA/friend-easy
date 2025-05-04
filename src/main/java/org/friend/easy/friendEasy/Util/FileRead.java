package org.friend.easy.friendEasy.Util;

import java.io.File;
import java.util.logging.Logger;

public class FileRead {
    public static Logger logger;
    public static void setLogger(Logger logger) {
        FileRead.logger = logger;
    }
    public static File readFile(File file){
        if(logger == null){
            logger = Logger.getLogger("FileRead");
        }
        logger.fine("Reading file: " + file.getAbsolutePath());
        return file;
    }
    public static File readFile(String filePath){
        if(logger == null){
            logger = Logger.getLogger("FileRead");
        }
        File file = new File(filePath);
        logger.fine("Reading file: " + file.getAbsolutePath());
        return file;
    }


}
