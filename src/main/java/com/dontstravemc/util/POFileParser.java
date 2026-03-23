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

    // from STRINGS.RECIPE_DESC.*
    public String recipeDesc; // msgid 
    public String recipeDescTranslated; // msgstr

    public POEntry(String msgid, String msgstr) {
        this.msgid = msgid;
        this.msgstr = msgstr;
    }

    public void setRecipeDesc(String recipe_desc, String recipe_desc_trans) {
        this.recipeDesc = recipe_desc;
        this.recipeDescTranslated = recipe_desc_trans;
    }

    @Override
    public String toString() {
        return String.format("POEntry(msgid=%s, msgstr=%s, recipe_desc=%s, recipe_desc_trans=%s)", msgid, msgstr, recipeDesc, recipeDescTranslated);
    }
}

public class POFileParser {

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
        
        int len_of_msgctxt_prefix = "STRINGS.NAMES.".length();
        int len_of_recipe_desc_prefix = "STRINGS.RECIPE_DESC.".length();
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip comments
            if (trimmed.startsWith("#")) {
                continue;
            }
            
            // Check for each keyword
            if (trimmed.startsWith("msgctxt")) {
                // If we encounter a new msgctxt but haven't completed the previous entry, reset
                msgid = null;
                msgstr = null;
                msgctxt = parseString(trimmed.substring(7));
            } else if (trimmed.startsWith("msgid")) {
                msgid = parseString(trimmed.substring(5));
            } else if (trimmed.startsWith("msgstr")) {
                msgstr = parseString(trimmed.substring(6));
            }
            // If line doesn't start with any known keyword, skip it
            
            // When we have all three, add to result
            if (msgctxt != null && msgid != null && msgstr != null) {
                // Only keep entries that start with STRINGS.NAMES. and trim the prefix
                if (msgctxt.startsWith("STRINGS.NAMES.")) {
                    String key = msgctxt.substring(len_of_msgctxt_prefix).toLowerCase();
                    result.put(key, new POEntry(msgid, msgstr));
                }else if (msgctxt.startsWith("STRINGS.RECIPE_DESC.")) {
                    String key = msgctxt.substring(len_of_recipe_desc_prefix).toLowerCase();
                    POEntry entry = result.get(key);
                    if (entry != null) {
                        entry.setRecipeDesc(msgid, msgstr);
                    }
                }
                msgctxt = null;
                msgid = null;
                msgstr = null;
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
