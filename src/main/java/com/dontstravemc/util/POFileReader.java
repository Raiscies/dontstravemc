package com.dontstravemc.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * PO File Parser - Manual parser
 * 
 * .po file format:
 *   msgctxt "context"
 *   msgid "original"
 *   msgstr "translated"
 * 
 * We ignore comments and entries without msgctxt.
 */

class POEntry {
    public final String msgid;
    public final String msgstr;

    public POEntry(String msgid, String msgstr) {
        this.msgid = msgid;
        this.msgstr = msgstr;
    }

    @Override
    public String toString() {
        // return String.format("POEntry(msgid=%s, msgstr=%s, recipe_desc=%s, recipe_desc_trans=%s)", msgid, msgstr, recipeDesc, recipeDescTranslated);
        return String.format("POEntry(msgid=%s, msgstr=%s)", msgid, msgstr);
    }
}

public class POFileReader {

    /**
     * Parse PO file from file path.
     * 
     * @param path Path to the PO file
     * @return Map with msgctxt as key, POEntry as value
     */
    public static HashMap<String, POEntry> parseFile(Path path) {
        try {
            String content = Files.readString(path);
            return parse(content);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read PO file: " + path, e);
        }
    }

    /**
     * Parse PO file content.
     * 
     * @param content The PO file content
     * @return Map with msgctxt as key, POEntry as value
     */
    public static HashMap<String, POEntry> parse(String content) {
        HashMap<String, POEntry> result = new HashMap<>();
        
        String[] lines = content.split("\n");
        String msgctxt = null;
        String msgid = null;
        String msgstr = null;

        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip comments
            if (trimmed.startsWith("#")) {
                continue;
            }
            
            // Check for each keyword
            if (trimmed.startsWith("msgctxt")) {
                // If we encounter a new msgctxt but haven't completed the previous entry, reset
                msgctxt = parseString(trimmed.substring(7));
                msgid = null;
                msgstr = null;
            } else if (trimmed.startsWith("msgid")) {
                msgid = parseString(trimmed.substring(5));
            } else if (trimmed.startsWith("msgstr")) {
                msgstr = parseString(trimmed.substring(6));
            }
            // If line doesn't start with any known keyword, skip it
            
            if (msgctxt != null && msgid != null && msgstr != null) {
                result.put(msgctxt, new POEntry(msgid, msgstr));
            }
        }
        
        return result;
    }

    /**
     * Parse a string after the keyword (e.g., after "msgctxt ")
     * Assumes format: keyword "content"
     */
    private static String parseString(String afterKeyword) {
        String trimmed = afterKeyword.trim();
        
        if (!trimmed.startsWith("\"") || !trimmed.endsWith("\"")) {
            return null;
        }
        
        // Remove surrounding quotes
        String s = trimmed.substring(1, trimmed.length() - 1);
        
        // Unescape
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
