/*
 * This file is part of the OdinMS MapleStory Private Server
 * Copyright (C) 2012 Patrick Huy and Matthias Butz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import server.Randomizer;
import tools.HexTool;
import tools.Pair;

import java.io.*;
import java.net.URL;

/**
 * @author AuroX
 */
public class LieDetectorScript {

    private static final String IMG_DIRECTORY = "lieDetector";
    private static final String CAPTCHA_VERIFIER = "3ec48eab7c5f53e299fb9c2be9cb9038ac2784e5";
    private static final String CAPTCHA_SERVER = "http://dokeos.dothome.co.kr/captcha.php?verify=" + CAPTCHA_VERIFIER;

    public static final Pair<String, String> getImageBytes() {
        final File directory = new File(IMG_DIRECTORY);
        if (!directory.exists()) {
            System.err.println("lieDetector folder does not exist!");
            return null;
        }
        final String filename[] = directory.list();
        String answer = filename[Randomizer.nextInt(filename.length)];
        answer = answer.substring(0, answer.length() - 4); // .jpg 
        try {
            return new Pair<>(HexTool.toString(getBytesFromFile(new File(IMG_DIRECTORY + "/" + answer + ".jpg"))), answer);
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] getBytesFromFile(final File file) throws IOException {
        byte[] bytes = null;
        try ( InputStream is = new FileInputStream(file)) {
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                return null;
            }
            bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                System.err.println("[Lie Detector Script] Could not completely read file " + file.getName());
                return null;
            }
        }
        return bytes;
    }
}
