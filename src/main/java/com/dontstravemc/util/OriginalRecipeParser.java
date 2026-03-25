package com.dontstravemc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;


/*
 * This file is expected to parse all of the recipes from the origin game *Don't Starve Together*, 
 * origin recipes are recorded in origin game's scripts file, under "./steamapps/common/Don't Starve Together/data/databundles/scripts.zip/recipes.lua". 
 * The parsed data can then be used to register recipes in the mod or for other purposes.
*/

/**
 * Grammar:
 * 
 * IngredientType       -> string
 * ItemName             -> string
 * Amount               -> number
 * Function             -> 'function' '(' ... ')' ... 'end'
 *  where ... represents all of the things we don't care about. 
 * 
 * List                 -> '{' ListItem [',' ListItem]* '}'
 * ListItem             -> List | Term 
 * Term                 -> string | number | (identifier ['=' (string | number | identifier | Function)])
 * 
 * Recipe               -> 'Recipe2'
 * DeconstructRecipe    -> 'DeconstructRecipe'
 * 
 * IngredientList       -> '{' IngredientDef [',' IngredientDef]* '}'
 * 
 * TechnologyConstraint -> 'TECH\.[A-Z_]+'
 * 
 * IngredientDef        -> 'Ingredient' '(' IngredientType ',' Amount  [',' some other params] ')'
 * RecipeDef            -> Recipe '(' ItemName ',' IngredientList ',' TechnologyConstraint [',' List] ')'
 * DeconstructRecipeDef -> DeconstructRecipe '(' ItemName ',' IngredientList [',' List] ')'
 * 
 * S -> RecipeDef | DeconstructRecipeDef
 * 
 */


// ==================== JSON POJO Classes ====================

class JsonIngredient {
    @SerializedName("type")
    public String type;
    
    @SerializedName("amount")
    public int amount;
    
    @SerializedName("extra")
    public List<String> extra;

    public JsonIngredient(String type, int amount, List<String> extra) {
        this.type = type;
        this.amount = amount;
        this.extra = extra;
    }
}

class JsonConfigValue {
    // Can be String, Number, String (identifier), or List
    public Object value;

    public JsonConfigValue(Object value) {
        this.value = value;
    }
}

class JsonConfig {
    public String key;
    public Object value;

    public JsonConfig(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}

class JsonRecipe {
    @SerializedName("type")
    public String type;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("text")
    public String text;
    
    @SerializedName("text_trans")
    public String textTrans;
    
    @SerializedName("recipe_desc")
    public String recipeDesc;
    
    @SerializedName("recipe_desc_trans")
    public String recipeDescTrans;
    
    @SerializedName("ingredients")
    public Map<String, Integer> ingredients;
    
    @SerializedName("tech")
    public String tech;
    
    // something wrong with it for now
    // and it does not important
    // @SerializedName("configs")
    // public List<JsonConfig> configs;

    public JsonRecipe(String type, String name, String text, String textTrans, 
                      String recipeDesc, String recipeDescTrans, 
                      Map<String, Integer> ingredients, String tech/* , List<JsonConfig> configs = null*/) {
        this.type = type;
        this.name = name;
        this.text = text;
        this.textTrans = textTrans;
        this.recipeDesc = recipeDesc;
        this.recipeDescTrans = recipeDescTrans;
        this.ingredients = ingredients;
        this.tech = tech;
        // this.configs = configs;
    }
}


// ==================== Token Definitions ====================

enum TokenType {
    RECIPE,           // Recipe2
    DECONSTRUCT,      // DeconstructRecipe
    INGREDIENT,       // Ingredient
    TECH,             // TECH.xxx
    FUNCTION,         // function
    STRING,           // "item_name" or 'item_name'
    NUMBER,           // numbers
    IDENTIFIER,       // identifiers (non-keyword)
    LBRACE,           // {
    RBRACE,           // }
    LPAREN,           // (
    RPAREN,           // )
    COMMA,            // ,
    EQUAL,            // =
    END,              // end
    EOF               // end of file
}

class Token {
    public final TokenType type;
    public final String value;
    public final int line;
    public final int column;

    public Token(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("Token(%s, '%s', %d:%d)", type, value, line, column);
    }
}

// ==================== Lexer ====================

class Lexer {
    public final String input;
    public int pos = 0;
    public int line = 1;
    public int column = 1;
    private Token currentToken;

    public Lexer(String input) {
        this.input = input;
    }

    public Token peek() {
        if (currentToken == null) {
            currentToken = nextToken();
        }
        return currentToken;
    }

    public Token consume() {
        if (currentToken != null) {
            Token t = currentToken;
            currentToken = null;
            return t;
        }
        return nextToken();
    }

    /**
     * Skip input until the target string is found
     */
    public void skipUntil(String target) {
        int index = input.indexOf(target, pos);
        if (index != -1) {
            // Update line and column to approximately the end position
            for (int i = pos; i < index; i++) {
                if (input.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            pos = index + target.length();
            column += target.length();
            // Invalidate current token since position changed
            currentToken = null;
        }
    }

    /**
     * Get the substring from current position to the target string
     */
    public String getUntil(String target) {
        int index = input.indexOf(target, pos);
        if (index != -1) {
            return input.substring(pos, index);
        }
        return input.substring(pos);
    }

    /**
     * Skip to the beginning of the next line
     */
    public void skipToNextLine() {
        // Skip until we find a newline or end of input
        while (pos < input.length() && input.charAt(pos) != '\n') {
            pos++;
        }
        if (pos < input.length() && input.charAt(pos) == '\n') {
            pos++;
            line++;
            column = 1;
        }
        // Invalidate current token
        currentToken = null;
    }

    private Token nextToken() {
        skipWhitespace();
        
        if (pos >= input.length()) {
            return new Token(TokenType.EOF, "", line, column);
        }

        char c = input.charAt(pos);

        // String literals
        if (c == '"' || c == '\'') {
            return readString(c);
        }

        // Numbers
        if (Character.isDigit(c) || (c == '-' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1)))) {
            return readNumber();
        }

        // Identifiers and keywords
        if (Character.isLetter(c) || c == '_') {
            return readIdentifier();
        }

        // Single character tokens
        pos++;
        column++;
        switch (c) {
            case '{': return new Token(TokenType.LBRACE, "{", line, column - 1);
            case '}': return new Token(TokenType.RBRACE, "}", line, column - 1);
            case '(': return new Token(TokenType.LPAREN, "(", line, column - 1);
            case ')': return new Token(TokenType.RPAREN, ")", line, column - 1);
            case ',': return new Token(TokenType.COMMA, ",", line, column - 1);
            case '=': return new Token(TokenType.EQUAL, "=", line, column - 1);
        }

        // Skip unknown characters
        return nextToken();
    }

    private void skipWhitespace() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isWhitespace(c)) {
                if (c == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                pos++;
            } else if (c == '-' && pos + 1 < input.length() && input.charAt(pos + 1) == '-') {
                // Skip comments
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    pos++;
                }
            } else {
                break;
            }
        }
    }

    private Token readString(char quote) {
        int startColumn = column;
        pos++; // skip opening quote
        column++;
        StringBuilder sb = new StringBuilder();
        
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == quote) {
                pos++;
                column++;
                return new Token(TokenType.STRING, sb.toString(), line, startColumn);
            }
            if (c == '\\' && pos + 1 < input.length()) {
                pos++;
                column++;
                char escaped = input.charAt(pos);
                switch (escaped) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    case 'r': sb.append('\r'); break;
                    case '\'': sb.append('\''); break;
                    case '"': sb.append('"'); break;
                    default: sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
            pos++;
            column++;
        }
        
        return new Token(TokenType.STRING, sb.toString(), line, startColumn);
    }

    private Token readNumber() {
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        
        // Handle negative numbers
        if (pos < input.length() && input.charAt(pos) == '-') {
            sb.append('-');
            pos++;
            column++;
        }
        
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            sb.append(input.charAt(pos));
            pos++;
            column++;
        }
        
        return new Token(TokenType.NUMBER, sb.toString(), line, startColumn);
    }

    private Token readIdentifier() {
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_' || input.charAt(pos) == '.')) {
            sb.append(input.charAt(pos));
            pos++;
            column++;
        }
        
        String value = sb.toString();
        
        // Check for keywords
        if (value.equals("Recipe2")) {
            return new Token(TokenType.RECIPE, value, line, startColumn);
        } else if (value.equals("DeconstructRecipe")) {
            return new Token(TokenType.DECONSTRUCT, value, line, startColumn);
        } else if (value.equals("Ingredient")) {
            return new Token(TokenType.INGREDIENT, value, line, startColumn);
        } else if (value.equals("function")) {
            return new Token(TokenType.FUNCTION, value, line, startColumn);
        } else if (value.equals("end")) {
            return new Token(TokenType.END, value, line, startColumn);
        } else if (value.startsWith("TECH.")) {
            return new Token(TokenType.TECH, value, line, startColumn);
        }
        
        return new Token(TokenType.IDENTIFIER, value, line, startColumn);
    }
}

// ==================== AST Node Classes ====================

abstract class ASTNode {
    public abstract void accept(ASTVisitor visitor);
}

class IngredientDef extends ASTNode {
    public final String ingredientType;
    public final int amount;
    public final List<String> extraParams;

    public IngredientDef(String ingredientType, int amount, List<String> extraParams) {
        this.ingredientType = ingredientType;
        this.amount = amount;
        this.extraParams = extraParams;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

/**
 * Represents a function in Configs: function(...) ... end
 */
class FunctionConfig extends ASTNode {
    public final String content; // Raw content between function(...) and end

    public FunctionConfig(String content) {
        this.content = content;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

/**
 * Represents a list value that can contain nested lists or key-value pairs
 */
class ListValue extends ASTNode {
    public final List<ListItem> items;

    public ListValue(List<ListItem> items) {
        this.items = items;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

/**
 * Represents a Term: string | number | identifier | KVPair
 * Term can be:
 *   - A string literal
 *   - A number
 *   - An identifier (standalone)
 *   - A KVPair (identifier = value)
 */
class Term extends ASTNode {
    public final Object value; // Can be String, Number, Identifier (String), or ConfigEntry

    public Term(Object value) {
        this.value = value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

/**
 * Represents an item in a List: either a nested List or a Term
 */
class ListItem extends ASTNode {
    public final Object value; // Can be ListValue or Term

    public ListItem(Object value) {
        this.value = value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

/**
 * Represents a key-value pair in Configs
 */
class ConfigEntry extends ASTNode {
    public final String key;
    public final Object value; // Can be String, Number, Identifier (String), FunctionConfig, or ListValue

    public ConfigEntry(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

abstract class AbstractRecipeDef extends ASTNode {
    public final String itemName;
    public final List<IngredientDef> ingredients;
    public final List<ListItem> configs;

    // From PO file, can be null if not found
    public final String itemTextName;             
    public final String itemTextNameTranslated;

    public final String recipeDesc;
    public final String recipeDescTranslated;

    public AbstractRecipeDef(String itemName, List<IngredientDef> ingredients, List<ListItem> configs, POEntry itemTexts, POEntry recipeDescTexts) {
        this.itemName = itemName;
        this.ingredients = ingredients;
        this.configs = configs;


        if (itemTexts != null) {
            this.itemTextName = itemTexts.msgid;
            this.itemTextNameTranslated = itemTexts.msgstr;
        }else {
            this.itemTextName = null;
            this.itemTextNameTranslated = null;
        }
        if(recipeDescTexts != null) {
            this.recipeDesc = recipeDescTexts.msgid;
            this.recipeDescTranslated = recipeDescTexts.msgstr;
        } else {
            this.recipeDesc = null;
            this.recipeDescTranslated = null;
        }
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
class RecipeDef extends AbstractRecipeDef {
    public final String technologyConstraint;

    public RecipeDef(String itemName, List<IngredientDef> ingredients, String technologyConstraint, List<ListItem> configs, POEntry itemTexts, POEntry recipeDescTexts) {
        super(itemName, ingredients, configs, itemTexts, recipeDescTexts);

        this.technologyConstraint = technologyConstraint;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

}

class DeconstructRecipeDef extends AbstractRecipeDef {

    public DeconstructRecipeDef(String itemName, List<IngredientDef> ingredients, List<ListItem> configs, POEntry itemTexts, POEntry recipeDescTexts) {
        super(itemName, ingredients, configs, itemTexts, recipeDescTexts);

    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

// ==================== AST Visitor ====================

interface ASTVisitor {
    void visit(IngredientDef node);
    void visit(FunctionConfig node);
    void visit(ListValue node);
    void visit(Term node);
    void visit(ListItem node);
    void visit(ConfigEntry node);
    void visit(AbstractRecipeDef node);
    void visit(RecipeDef node);
    void visit(DeconstructRecipeDef node);
}

// ==================== LL Parser ====================

class Parser {
    private final Lexer lexer;
    private Token currentToken;
    
    private final HashMap<String, POEntry> poEntries; // For looking up translations

    public Parser(String input, HashMap<String, POEntry> poEntries) {
        this.lexer = new Lexer(input);
        this.poEntries = poEntries;
    }

    private void advance() {
        currentToken = lexer.consume();
    }

    private void expect(TokenType... types) {
        for (TokenType type : types) {
            if (currentToken.type == type) {
                return;
            }
        }
        throw new ParseException("Expected one of " + java.util.Arrays.toString(types) + " but got " + currentToken.type + 
            " at " + currentToken.line + ":" + currentToken.column);
    }

    private boolean check(TokenType... types) {
        if (currentToken == null) return false;
        for (TokenType type : types) {
            if (currentToken.type == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recover from parse error by skipping to the next statement or end of input
     * Uses panic mode: simply skip to next line and try parsing again
     */
    private void recoverFromError() {
        // Skip to next line and advance to get next token
        lexer.skipToNextLine();
        advance();
    }

    public List<ASTNode> parse() {
        List<ASTNode> statements = new ArrayList<>();
        advance();
        
        while (!check(TokenType.EOF)) {
            try {
                ASTNode statement = parseStatement();
                if (statement != null) {
                    statements.add(statement);
                }
            } catch (ParseException e) {
                // Panic mode: print error and recover
                System.err.println("Parse error at " + e.getMessage() + ", recovering...");
                recoverFromError();
            }
        }
        
        return statements;
    }

    /**
     * Check if current token starts a valid S statement
     * First(S) = {Recipe2, DeconstructRecipe}
     */
    private boolean isStartOfStatement() {
        return check(TokenType.RECIPE, TokenType.DECONSTRUCT);
    }

    private ASTNode parseStatement() {
        if (isStartOfStatement()) {
            if (check(TokenType.RECIPE)) {
                return parseRecipeDef();
            } else {
                return parseDeconstructRecipeDef();
            }
        } else {
            // Not a valid statement start - skip this line without saving
            lexer.skipToNextLine();
            // After skipping to next line, advance to get the next token
            advance();
            return null;
        }
    }

    /**
     * RecipeDef -> RECIPE '(' ItemName ',' IngredientList ',' TechnologyConstraint [',' Configs] ')'
     */
    private RecipeDef parseRecipeDef() {
        expect(TokenType.RECIPE);
        advance();
        
        expect(TokenType.LPAREN);
        advance();
        
        expect(TokenType.STRING);
        String itemName = currentToken.value;
        advance();
        
        expect(TokenType.COMMA);
        advance();
        
        List<IngredientDef> ingredients = parseIngredientList();
        
        expect(TokenType.COMMA);
        advance();
        
        expect(TokenType.TECH);
        String techConstraint = currentToken.value;
        advance();
        
        List<ListItem> configs = new ArrayList<>();
        if (check(TokenType.COMMA)) {
            advance();
            configs = parseConfigList();
        }
        
        expect(TokenType.RPAREN);
        advance();
        
        return new RecipeDef(itemName, ingredients, techConstraint, configs, getItemTexts(itemName), getRecipeDescTexts(itemName));
    }

    /**
     * DeconstructRecipeDef -> DECONSTRUCT '(' ItemName ',' IngredientList [',' Configs] ')'
     */
    private DeconstructRecipeDef parseDeconstructRecipeDef() {
        expect(TokenType.DECONSTRUCT);
        advance();
        
        expect(TokenType.LPAREN);
        advance();
        
        expect(TokenType.STRING);
        String itemName = currentToken.value;
        advance();
        
        expect(TokenType.COMMA);
        advance();
        
        List<IngredientDef> ingredients = parseIngredientList();
        
        List<ListItem> configs = new ArrayList<>();
        if (check(TokenType.COMMA)) {
            advance();
            configs = parseConfigList();
        }
        
        expect(TokenType.RPAREN);
        advance();
        
        return new DeconstructRecipeDef(itemName, ingredients, configs, getItemTexts(itemName), getRecipeDescTexts(itemName));
    }

    /**
     * IngredientList -> '{' IngredientDef [',' IngredientDef]* '}'
     */
    private List<IngredientDef> parseIngredientList() {
        List<IngredientDef> ingredients = new ArrayList<>();
        
        expect(TokenType.LBRACE);
        advance();
        
        while (!check(TokenType.RBRACE)) {
            IngredientDef ingredient = parseIngredientDef();
            ingredients.add(ingredient);
            
            if (check(TokenType.COMMA)) {
                advance();
            } else {
                break;
            }
        }
        
        expect(TokenType.RBRACE);
        advance();
        
        return ingredients;
    }

    /**
     * IngredientDef -> INGREDIENT '(' IngredientType ',' Amount [',' Params] ')'
     */
    private IngredientDef parseIngredientDef() {
        expect(TokenType.INGREDIENT);
        advance();
        
        expect(TokenType.LPAREN);
        advance();
        
        expect(TokenType.STRING, TokenType.IDENTIFIER);
        String ingredientType = currentToken.value;
        advance();
        
        expect(TokenType.COMMA);
        advance();
        
        // TODO: detect identifiers from TUNING.lua, and parse them to correct numbers  
        expect(TokenType.NUMBER, TokenType.IDENTIFIER);
        int amount = check(TokenType.NUMBER) ? Integer.parseInt(currentToken.value) : 0;
        advance();
        
        List<String> extraParams = new ArrayList<>();
        if (check(TokenType.COMMA)) {
            advance();
            while (!check(TokenType.RPAREN)) {
                if (check(TokenType.STRING, TokenType.NUMBER, TokenType.IDENTIFIER)) {
                    extraParams.add(currentToken.value);
                    advance();
                }
                
                if (check(TokenType.COMMA)) {
                    advance();
                } else {
                    break;
                }
            }
        }
        
        expect(TokenType.RPAREN);
        advance();
        
        return new IngredientDef(ingredientType, amount, extraParams);
    }

    /**
     * List -> '{' ListItem [',' ListItem]* '}'
     * ListItem -> List | Term 
     * Term -> string | number | (identifier ['=' (string | number | identifier | Function)])
     */
    private List<ListItem> parseList() {
        List<ListItem> items = new ArrayList<>();
        
        // Check if it's a brace-enclosed list or a single item
        if (check(TokenType.LBRACE)) {
            expect(TokenType.LBRACE);
            advance();
            
            while (!check(TokenType.RBRACE)) {
                ListItem item = parseListItem();
                items.add(item);
                
                if (check(TokenType.COMMA)) {
                    advance();
                } else {
                    break;
                }
            }
            
            expect(TokenType.RBRACE);
            advance();
        } else {
            // Single item (Term)
            ListItem item = parseListItem();
            items.add(item);
        }
        
        return items;
    }

    /**
     * ListItem -> List | Term
     */
    private ListItem parseListItem() {
        // If we see a left brace, it's a nested list
        if (check(TokenType.LBRACE)) {
            List<ListItem> nestedList = parseList();
            return new ListItem(new ListValue(nestedList));
        }
        // Otherwise, it's a Term
        Term term = parseTerm();
        return new ListItem(term);
    }

    /**
     * Term -> string | number | (identifier ['=' (string | number | identifier | Function)])
     */
    private Term parseTerm() {
        Object value;
        
        switch (currentToken.type) {
            case STRING -> {
                value = currentToken.value;
                advance();
            }
            case NUMBER -> {
                String numStr = currentToken.value;
                if (numStr.contains(".")) {
                    value = Double.valueOf(numStr);
                } else {
                    value = Integer.valueOf(numStr);
                }
                advance();
            }
            case IDENTIFIER -> {
                // Check if next token is '=' (KVPair case)
                // Use peek to look ahead without consuming
                Token next = lexer.peek();
                if (next != null && next.type == TokenType.EQUAL) {
                    // This is a KVPair: identifier = value
                    value = parseKVPair();
                } else {
                    // Standalone identifier
                    value = currentToken.value;
                    advance();
                }
            }
            default -> throw new ParseException("Expected string, number, or identifier but got " + currentToken.type + 
                " at " + currentToken.line + ":" + currentToken.column);
        }
        
        return new Term(value);
    }

    /**
     * Configs -> '{' KVPair [',' KVPair]* '}' (legacy, now uses List)
     * KVPair -> identifier '=' (string | number | identifier | Function | List)
     */
    private List<ListItem> parseConfigList() {
        // Now uses parseList() to support nested lists
        return parseList();
    }

    /**
     * KVPair -> identifier '=' (string | number | identifier | Function | List)
     */
    private ConfigEntry parseKVPair() {
        expect(TokenType.IDENTIFIER);
        String key = currentToken.value;
        advance();
        
        expect(TokenType.EQUAL);
        advance();
        
        Object value = switch (currentToken.type) {
            case STRING -> {
                String result = currentToken.value;
                advance();
                yield result;
            }
            case NUMBER -> {
                String numStr = currentToken.value;
                advance();
                if (numStr.contains(".")) {
                    yield Double.valueOf(numStr);
                } else {
                    yield Integer.valueOf(numStr);
                }
            }
            case IDENTIFIER -> {
                String result = currentToken.value;
                advance();
                yield result;
            }
            case FUNCTION -> parseFunction();
            case LBRACE -> {
                // Nested list
                List<ListItem> nestedList = parseList();
                yield new ListValue(nestedList);
            }
            default -> throw new ParseException("Expected string, number, identifier, function, or list but got " + currentToken.type + 
                " at " + currentToken.line + ":" + currentToken.column);
        };
        
        return new ConfigEntry(key, value);
    }

    /**
     * Parse a function: function(...) ... end
     * Note: Skip text directly, don't care about token rules inside
     */
    private FunctionConfig parseFunction() {
        // 'function' keyword already consumed by caller
        expect(TokenType.FUNCTION);
        
        // Get the raw content from current position until 'end'
        String funcContent = "function " + lexer.getUntil("end");
        
        // Skip past 'end'
        lexer.skipUntil("end");
        
        advance();
        
        return new FunctionConfig(funcContent.trim());
    }

    private POEntry getItemTexts(String itemName) {
        String key = "STRINGS.NAMES." + itemName.toUpperCase();
        return poEntries.get(key);
    }
    private POEntry getRecipeDescTexts(String itemName) {
        String key = "STRINGS.RECIPE_DESC." + itemName.toUpperCase();
        return poEntries.get(key);
    }
}

// ==================== Parse Exception ====================

class ParseException extends RuntimeException {
    public ParseException(String message) {
        super(message);
    }
}

// ==================== Main Parser Class ====================

public class OriginalRecipeParser {

    /**
     * Read all lines from a resource file in the mod's resources.
     *
     * @param path The path to the resource file (e.g., "data/mymod/recipes/myrecipe.txt")
     * @return A list of lines from the file
     */
    public static List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        InputStream stream = OriginalRecipeParser.class.getClassLoader().getResourceAsStream(path);
        
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
        
        return lines;
    }

    /**
     * Read all lines and process them with a callback.
     *
     * @param path    The path to the resource file
     * @param callback A consumer to process each line
     */
    public static void processLines(String path, Consumer<String> callback) {
        InputStream stream = OriginalRecipeParser.class.getClassLoader().getResourceAsStream(path);
        
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                callback.accept(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    /**
     * Read entire file content as a single string.
     *
     * @param path The path to the resource file
     * @return The entire file content as a string
     */
    public static String readContent(String path) {
        StringBuilder content = new StringBuilder();
        
        processLines(path, line -> {
            content.append(line).append("\n");
        });
        
        return content.toString();
    }

    /**
     * Parse recipes from a string input.
     * 
     * @param input The recipe text to parse
     * @return A list of AST nodes representing the parsed recipes
     */
    public static List<ASTNode> parse(String input, HashMap<String, POEntry> poEntries) {
        Parser parser = new Parser(input, poEntries);
        return parser.parse();
    }

    /**
     * Parse recipes from a resource file.
     * 
     * @param path The path to the resource file
     * @return A list of AST nodes representing the parsed recipes
     */
    public static List<ASTNode> parseFromResource(String path, HashMap<String, POEntry> poEntries) {
        String content = readContent(path);
        return parse(content, poEntries);
    }

    /**
     * Convert AST nodes to JSON using Gson
     */
    private static String nodesToJson(List<ASTNode> nodes, Gson gson) {
        List<JsonRecipe> recipes = new ArrayList<>();
        
        for (ASTNode node : nodes) {
            switch (node) {
                case RecipeDef recipeDef -> {
                    // Convert ingredients to flat map (type -> amount)
                    java.util.Map<String, Integer> ingredients = new HashMap<>();
                    for (IngredientDef ing : recipeDef.ingredients) {
                        ingredients.put(ing.ingredientType, ing.amount);
                    }
                    
                    // // Convert configs
                    // List<JsonConfig> configs = convertConfigs(recipeDef.configs);
                    
                    JsonRecipe recipe = new JsonRecipe(
                        "recipe",
                        recipeDef.itemName,
                        recipeDef.itemTextName,
                        recipeDef.itemTextNameTranslated,
                        recipeDef.recipeDesc,
                        recipeDef.recipeDescTranslated,
                        ingredients,
                        recipeDef.technologyConstraint
                        // configs
                    );
                    recipes.add(recipe);
                }
                case DeconstructRecipeDef deconstructRecipeDef -> {
                    // Convert ingredients to flat map (type -> amount)
                    java.util.Map<String, Integer> ingredients = new HashMap<>();
                    for (IngredientDef ing : deconstructRecipeDef.ingredients) {
                        ingredients.put(ing.ingredientType, ing.amount);
                    }
                    
                    // // Convert configs
                    // List<JsonConfig> configs = convertConfigs(deconstructRecipeDef.configs);
                    
                    JsonRecipe recipe = new JsonRecipe(
                        "deconstruct",
                        deconstructRecipeDef.itemName,
                        deconstructRecipeDef.itemTextName,
                        deconstructRecipeDef.itemTextNameTranslated,
                        deconstructRecipeDef.recipeDesc,
                        deconstructRecipeDef.recipeDescTranslated,
                        ingredients, 
                        null
                        // configs
                    );
                    recipes.add(recipe);
                }
                default -> {
                    // Skip unknown node types
                }
            }
        }
        
        return gson.toJson(recipes);
    }

    /**
     * Convert config ListItems to JsonConfig list
     */
    private static List<JsonConfig> convertConfigs(List<ListItem> configs) {
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        
        List<JsonConfig> result = new ArrayList<>();
        for (ListItem item : configs) {
            Object value = item.value;
            if (value instanceof ConfigEntry configEntry) {
                result.add(new JsonConfig(configEntry.key, convertConfigValue(configEntry.value)));
            } else if (value instanceof Term term) {
                // Handle Term as a config entry with numeric index
                result.add(new JsonConfig(String.valueOf(result.size()), convertConfigValue(term.value)));
            }
        }
        
        return result.isEmpty() ? null : result;
    }

    /**
     * Convert a config value to JSON-compatible object
     */
    private static Object convertConfigValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number) {
            return value;
        }
        if (value instanceof FunctionConfig funcConfig) {
            return funcConfig.content;
        }
        if (value instanceof ListValue listValue) {
            return convertListValue(listValue);
        }
        // Default to string representation
        return value.toString();
    }

    /**
     * Convert ListValue to a list of JSON-compatible objects
     */
    private static List<Object> convertListValue(ListValue listValue) {
        List<Object> result = new ArrayList<>();
        for (ListItem item : listValue.items) {
            if (item.value instanceof ListValue nested) {
                result.add(convertListValue(nested));
            } else if (item.value instanceof Term term) {
                result.add(convertConfigValue(term.value));
            } else if (item.value instanceof ConfigEntry configEntry) {
                // For config entries in a list, create a mini map
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put(configEntry.key, convertConfigValue(configEntry.value));
                result.add(map);
            }
        }
        return result;
    }

    /**
     * Resolve a path to absolute path. If the path is relative, it will be resolved
     * relative to the current working directory (project root).
     * 
     * @param path The path to resolve
     * @return The resolved absolute path
     */
    private static Path resolvePath(String path) {
        Path p = java.nio.file.Paths.get(path);
        if (p.isAbsolute()) {
            return p;
        }
        // Resolve relative to current working directory
        return java.nio.file.Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
    }

    /**
     * Write JSON output to a file
     * 
     * @param json The JSON string to write
     * @param outputPath The path to the output file
     */
    private static void writeToFile(String json, String outputPath) throws IOException {
        Path path = resolvePath(outputPath);
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Example usage and testing
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java OriginalRecipeParser <input_recipe_file> [input_po_file] <output_file>");
            System.err.println("  <input_recipe_file> - Path to the recipes.lua file to parse");
            System.err.println("  [input_po_file>]    - Path to the .po file containing translations");
            System.err.println("  <output_file>       - Path to the output JSON file");
            return;
        }
        
        String inputPath = args[0];
        String poFilePath = args.length > 2 ? args[1] : null;
        String outputPath = args.length > 2 ? args[2] : args[1];
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            // Read file content from the given path (supports relative paths)
            Path path = resolvePath(inputPath);
            Path poPath = poFilePath != null ? resolvePath(poFilePath) : null;

            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            HashMap<String, POEntry> poEntries = poPath != null ? POFileReader.parseFile(poPath) : new HashMap<>();
            
            List<ASTNode> nodes = parse(content, poEntries);
            
            // Output as JSON to file using Gson
            String json = nodesToJson(nodes, gson);
            writeToFile(json, outputPath);
            
            System.out.println("Successfully wrote parsed recipes to: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("Failed to read/write file: " + e.getMessage());
        } catch (ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
        }
    }
}
