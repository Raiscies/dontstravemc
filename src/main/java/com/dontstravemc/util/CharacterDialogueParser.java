package com.dontstravemc.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// ==================== 树结构节点类 ====================

/**
 * 对话树节点，用于递归解析点分割的字符串key
 * - 如果是叶子节点（有值），存储 originalValue 和 translatedValue
 * - 如果不是叶子节点（有子节点），children 不为空
 */
class DialogueTreeNode {
    // 叶子节点的值（非叶子节点时为 null）
    public String originalValue;
    public String translatedValue;
    
    // 子节点 map（非叶子节点时有值）
    public HashMap<String, DialogueTreeNode> children;

    public DialogueTreeNode() {
        this.children = new HashMap<>();
    }

    public DialogueTreeNode(String originalValue, String translatedValue) {
        this.originalValue = originalValue;
        this.translatedValue = translatedValue;
        this.children = new HashMap<>();
    }

    /**
     * 判断是否为叶子节点（无子节点）
     */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * 递归插入一个值到树中
     * @param pathParts 按点分割的路径部分数组
     * @param index 当前处理的路径部分索引
     * @param originalValue 原始值
     * @param translatedValue 翻译值
     */
    public void put(String[] pathParts, int index, String originalValue, String translatedValue) {
        if (index >= pathParts.length) {
            return;
        }

        String part = pathParts[index];
        
        // 如果是最后一个部分，设置叶子节点的值
        if (index == pathParts.length - 1) {
            // 如果当前节点还没有 children，先创建一个空的 HashMap
            if (this.children == null) {
                this.children = new HashMap<>();
            }
            // 为最后一个部分创建子节点并设置值
            DialogueTreeNode leafNode = children.computeIfAbsent(part, k -> new DialogueTreeNode());
            leafNode.originalValue = originalValue;
            leafNode.translatedValue = translatedValue;
            return;
        }

        // 否则继续向下递归，创建子节点
        if (this.children == null) {
            this.children = new HashMap<>();
        }
        DialogueTreeNode child = children.computeIfAbsent(part, k -> new DialogueTreeNode());
        child.put(pathParts, index + 1, originalValue, translatedValue);
    }

    /**
     * 获取所有叶子节点的值（原始版）
     * @param prefix 当前路径前缀
     * @return key为完整路径，value为原始对话的map
     */
    public HashMap<String, String> getAllLeavesOriginal(String prefix) {
        HashMap<String, String> result = new HashMap<>();
        collectLeaves(this, prefix, result, false);
        return result;
    }

    /**
     * 获取所有叶子节点的值（翻译版）
     * @param prefix 当前路径前缀
     * @return key为完整路径，value为翻译对话的map
     */
    public HashMap<String, String> getAllLeavesTranslated(String prefix) {
        HashMap<String, String> result = new HashMap<>();
        collectLeaves(this, prefix, result, true);
        return result;
    }

    /**
     * 递归收集所有叶子节点
     */
    private void collectLeaves(DialogueTreeNode node, String prefix, HashMap<String, String> result, boolean translated) {
        if (node == null) {
            return;
        }
        
        if (node.isLeaf()) {
            // 叶子节点
            String value = translated ? node.translatedValue : node.originalValue;
            if (value != null && prefix.length() > 0) {
                result.put(prefix, value);
            }
        } else if (node.children != null) {
            // 遍历子节点
            for (java.util.Map.Entry<String, DialogueTreeNode> entry : node.children.entrySet()) {
                String newPrefix = prefix.length() > 0 ? prefix + "." + entry.getKey() : entry.getKey();
                collectLeaves(entry.getValue(), newPrefix, result, translated);
            }
        }
    }
}

class CharacterDialogue {
    public String characterName;
    // 对话树结构
    public HashMap<String, DialogueTreeNode> dialogueTree;

    public CharacterDialogue(String characterName) {
        this.characterName = characterName;
        this.dialogueTree = new HashMap<>();
    }
}

// ==================== JSON POJO Class ====================

class JsonCharacterDialogue {
    // 直接把 character 和所有对话类型放在顶级
    public String character;
    // 对话内容：直接是键值对，不再有 dialogue_tree 包装
    public HashMap<String, Object> dialogues;

    public JsonCharacterDialogue() {}

    public JsonCharacterDialogue(CharacterDialogue dialogue) {
        this.character = dialogue.characterName;
        if (dialogue.dialogueTree != null) {
            this.dialogues = new HashMap<>();
            for (java.util.Map.Entry<String, DialogueTreeNode> entry : dialogue.dialogueTree.entrySet()) {
                this.dialogues.put(entry.getKey(), convertNodeToMap(entry.getValue()));
            }
        }
    }

    /**
     * 递归将 DialogueTreeNode 转换为 Map
     * - 叶子节点：返回包含 original 和 translated 的 Map
     * - 非叶子节点：返回一个 Map，键为子节点名，值为递归转换结果
     */
    private Object convertNodeToMap(DialogueTreeNode node) {
        if (node.isLeaf()) {
            // 叶子节点：返回包含 original 和 translated 的 Map
            HashMap<String, String> leafMap = new HashMap<>();
            leafMap.put("en", node.originalValue);
            leafMap.put("trans", node.translatedValue);
            return leafMap;
        } else if (node.children != null) {
            // 非叶子节点：返回子节点扁平化的 Map
            HashMap<String, Object> nodeMap = new HashMap<>();
            for (java.util.Map.Entry<String, DialogueTreeNode> childEntry : node.children.entrySet()) {
                nodeMap.put(childEntry.getKey(), convertNodeToMap(childEntry.getValue()));
            }
            return nodeMap;
        }
        return new HashMap<>();
    }
}

public class CharacterDialogueParser {
    
    public static final String[] CHARACTER_NAMES = {
        "generic", //wilson 
        "willow", 
        "wendy", 
        "wolfgang", 
        "wx78", 
        "wickerbottom", 
        "wes", 
        "waxwell", 
        "woodie", 
        "wathgrithr", 
        "webber", 
        "winona", 
        "wortox", 
        "wormwood", 
        "warly", 
        "wurt", 
        "walter", 
        "wanda", 
        "wonkey"
    };

    private static Path resolvePath(String path) {
        Path p = java.nio.file.Paths.get(path);
        if (p.isAbsolute()) {
            return p;
        }
        // Resolve relative to current working directory
        return java.nio.file.Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
    }

    /**
     * 递归解析所有以角色前缀开头的Entries，建立树结构
     * @param dialogue 角色对话对象
     * @param Entries 所有PO条目
     */
    public static void scanCharacterDialogues(CharacterDialogue dialogue, HashMap<String, POEntry> Entries) {
        String characterPrefix = "STRINGS.CHARACTERS." + dialogue.characterName.toUpperCase() + ".";
        
        // 遍历所有 Entries，按点分割递归插入到树中
        for (java.util.Map.Entry<String, POEntry> entry : Entries.entrySet()) {
            String key = entry.getKey();
            
            // 只处理以角色前缀开头的 key
            if (!key.startsWith(characterPrefix)) {
                continue;
            }
            
            // 获取 key 中前缀后面的部分（如 DESCRIBE.LOG 等）
            String pathAfterPrefix = key.substring(characterPrefix.length()).toLowerCase();
            
            // 按点分割路径
            String[] pathParts = pathAfterPrefix.split("\\.");
            
            // 根节点为第一个部分（如 DESCRIBE、TALK 等）
            String rootKey = pathParts[0];
            
            // 获取或创建该根节点的树
            DialogueTreeNode rootNode = dialogue.dialogueTree.computeIfAbsent(rootKey, k -> new DialogueTreeNode());
            
            // 递归插入（从索引0开始，传入完整路径）
            if (pathParts.length > 1) {
                // 传入完整 pathParts，让 rootNode 递归创建子节点
                rootNode.put(pathParts, 1, entry.getValue().msgid, entry.getValue().msgstr);
            } else {
                // 只有一个部分的情况（如 DESCRIBE 本身），设置叶子节点的值
                rootNode.originalValue = entry.getValue().msgid;
                rootNode.translatedValue = entry.getValue().msgstr;
                rootNode.children = null;
            }
        }
    }

    public static List<CharacterDialogue> scanAllCharactersDialogues(HashMap<String, POEntry> Entries) {
        List<CharacterDialogue> dialogues = new ArrayList<>();
        for (String character : CHARACTER_NAMES) {
            CharacterDialogue dialogue = new CharacterDialogue(character);
            scanCharacterDialogues(dialogue, Entries);
            dialogues.add(dialogue);
        }
        return dialogues;
    }

    public static String toJson(CharacterDialogue dialogue) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonCharacterDialogue jsonDialogue = new JsonCharacterDialogue(dialogue);
        return gson.toJson(jsonDialogue);
    }

    public static String toJsonList(java.util.List<CharacterDialogue> dialogues) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        java.util.List<JsonCharacterDialogue> jsonDialogues = new java.util.ArrayList<>();
        for (CharacterDialogue dialogue : dialogues) {
            jsonDialogues.add(new JsonCharacterDialogue(dialogue));
        }
        return gson.toJson(jsonDialogues);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java CharacterDialogueParser <input_dialogue_pofile> <output_file_prefix>");
            System.err.println("  <input_dialogue_pofile> - Path to the dialogue file to parse");
            System.err.println("  <output_file_prefix> - Prefix for the output JSON files");
            return;
        }
        
        String inputPath = args[0];
        String outputPath = args[1];

        try {
            Path path = resolvePath(inputPath);
            HashMap<String, POEntry> poEntries = POFileReader.parseFile(path);

            List<CharacterDialogue> dialogues = scanAllCharactersDialogues(poEntries);

            // 将每个角色的对话写入单独的 JSON 文件
            Path outputPathResolved = resolvePath(outputPath);
            
            Path outputDir;
            String filePrefix;
            
            if (Files.isDirectory(outputPathResolved)) {
                // 如果是目录，则在该目录下生成文件
                outputDir = outputPathResolved;
                filePrefix = "";
            } else {
                // 如果是文件，则将其作为前缀
                outputDir = outputPathResolved.getParent();
                filePrefix = outputPathResolved.getFileName().toString();
            }
            
            for (CharacterDialogue dialogue : dialogues) {
                String json = toJson(dialogue);
                String fileName = filePrefix + dialogue.characterName + ".json";
                Path filePath = outputDir.resolve(fileName);
                Files.write(filePath, json.getBytes(StandardCharsets.UTF_8));
                System.out.println("Wrote: " + filePath);
            }

            System.out.println("Successfully wrote " + dialogues.size() + " character dialogue files.");

        }catch (Exception e) {
            System.err.println("Error parsing dialogue file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}