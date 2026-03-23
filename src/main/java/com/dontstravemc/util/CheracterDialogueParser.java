package com.dontstravemc.util;

public class CheracterDialogueParser {
    
    public static final String[] CHARACTER_NAMES = {
        "WILSON", 
        "WILLOW", 
        "WENDY", 
        "WOLFGANG", 
        "WX78", 
        "WICKERBOTTOM", 
        "WES", 
        "WAXWELL", 
        "WOODIE", 
        "WATHGRITHR", 
        "WEBBER", 
        "WINONA", 
        "WORTOX", 
        "WORMWOOD", 
        "WARLY", 
        "WURT", 
        "WALTER", 
        "WANDA", 
        "WONKEY"
    };

    public static final String[] DIALOGUE_KEYWORDS = {
        // 物品描述
        "STRINGS.CHARACTERS.{character}.DESCRIBE.{item}",
        // 未知物品的fallback描述
        "STRINGS.CHARACTERS.{character}.DESCRIBE_GENERIC",
        
        // 冒烟
        "STRINGS.CHARACTERS.{character}.DESCRIBE_SMOLDERING",
        // 黑暗
        "STRINGS.CHARACTERS.{character}.DESCRIBE_TOODARK",

        // 关于植物
        // 开心
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTHAPPY",
        // 压力大
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTSTRESSED",
        // 家庭
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTSTRESSORFAMILY",
        // 需要交谈
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTSTRESSORHAPPINESS",
        // 杂草
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTSTRESSORKILLJOYS",
        // 干旱
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTSTRESSORMOISTURE",
        // 营养
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTSTRESSORNUTRIENTS",
        // 过度拥挤
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTSTRESSOROVERCROWDING",
        // 季节
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTSTRESSORSEASON",
        // 压力很大
        "STRINGS.CHARACTERS.{character}.DESCRIBE_PLANTVERYSTRESSED",
        
        // 还有很多，但有用到再补充吧
    };
    
    public static void main(String[] args) {

    }
}
