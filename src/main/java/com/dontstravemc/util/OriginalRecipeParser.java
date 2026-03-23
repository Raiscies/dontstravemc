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
import java.util.function.Consumer;



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
     * Escape a string for JSON
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Convert a Term to JSON string
     */
    private static String termToJson(Term term) {
        Object value = term.value;
        if (value instanceof String string) {
            return "\"" + escapeJson(string) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof ConfigEntry configEntry) {
            return configEntryToJson(configEntry);
        }
        return "\"unknown\"";
    }

    /**
     * Convert a ListItem to JSON string
     */
    private static String listItemToJson(ListItem item) {
        Object value = item.value;
        if (value instanceof ListValue listValue) {
            return listValueToJson(listValue);
        } else if (value instanceof Term term) {
            return termToJson(term);
        } else if (value instanceof ConfigEntry configEntry) {
            return configEntryToJson(configEntry);
        }
        return "\"unknown\"";
    }

    /**
     * Convert a ListValue to JSON string
     */
    private static String listValueToJson(ListValue listValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < listValue.items.size(); i++) {
            sb.append("  ").append(listItemToJson(listValue.items.get(i)));
            if (i < listValue.items.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Convert a ConfigEntry to JSON string (key: value)
     */
    private static String configEntryToJson(ConfigEntry config) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(escapeJson(config.key)).append("\": ");
        if (config.value instanceof String string) {
            sb.append("\"").append(escapeJson(string)).append("\"");
        } else if (config.value instanceof Number) {
            sb.append(config.value);
        } else if (config.value instanceof FunctionConfig functionConfig) {
            sb.append("\"").append(escapeJson(functionConfig.content)).append("\"");
        } else if (config.value instanceof ListValue listValue) {
            sb.append(listValueToJson(listValue));
        }
        return sb.toString();
    }

    /**
     * Convert ingredients list to JSON
     */
    private static void appendIngredients(StringBuilder sb, List<IngredientDef> ingredients, boolean includeExtraParams) {
        sb.append("    \"ingredients\": [\n");
        for (int j = 0; j < ingredients.size(); j++) {
            IngredientDef ing = ingredients.get(j);
            sb.append("      {\"type\": \"").append(escapeJson(ing.ingredientType)).append("\", \"amount\": ").append(ing.amount);
            if (includeExtraParams && !ing.extraParams.isEmpty()) {
                sb.append(", \"extra\": [");
                for (int k = 0; k < ing.extraParams.size(); k++) {
                    sb.append("\"").append(escapeJson(ing.extraParams.get(k))).append("\"");
                    if (k < ing.extraParams.size() - 1) sb.append(", ");
                }
                sb.append("]");
            }
            sb.append("}");
            if (j < ingredients.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ]");
    }

    /**
     * Convert AbstractRecipeDef common fields to JSON (includes ingredients)
     */
    private static void appendAbstractRecipeFields(StringBuilder sb, AbstractRecipeDef node, boolean includeExtraParams) {
        sb.append("    \"name\": \"").append(escapeJson(node.itemName)).append("\",\n");
        
        if (node.itemTextName != null) {
            sb.append("    \"text\": \"").append(escapeJson(node.itemTextName)).append("\",\n");
        }
        if (node.itemTextNameTranslated != null) {
            sb.append("    \"text_trans\": \"").append(escapeJson(node.itemTextNameTranslated)).append("\",\n");
        }
        if (node.recipeDesc != null) {
            sb.append("    \"recipe_desc\": \"").append(escapeJson(node.recipeDesc)).append("\",\n");
        }
        if (node.recipeDescTranslated != null) {
            sb.append("    \"recipe_desc_trans\": \"").append(escapeJson(node.recipeDescTranslated)).append("\",\n");
        }
        
        appendIngredients(sb, node.ingredients, includeExtraParams);
    }

    /**
     * Convert configs list to JSON
     */
    private static void appendConfigs(StringBuilder sb, List<ListItem> configs) {
        sb.append("    \"configs\": [\n");
        for (int j = 0; j < configs.size(); j++) {
            sb.append("      {").append(listItemToJson(configs.get(j))).append("}");
            if (j < configs.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ]");
    }

    /**
     * Convert a RecipeDef node to JSON
     */
    private static String recipeDefToJson(RecipeDef node) {
        StringBuilder sb = new StringBuilder();
        sb.append("  {\n");
        sb.append("    \"type\": \"recipe\",\n");
        
        appendAbstractRecipeFields(sb, node, false);
        sb.append(",\n");
        
        sb.append("    \"tech\": \"").append(escapeJson(node.technologyConstraint)).append("\"\n");
        
        sb.append("  }");
        return sb.toString();
    }

    /**
     * Convert a DeconstructRecipeDef node to JSON
     */
    private static String deconstructRecipeDefToJson(DeconstructRecipeDef node) {
        StringBuilder sb = new StringBuilder();
        sb.append("  {\n");
        sb.append("    \"type\": \"deconstruct\",\n");
        
        appendAbstractRecipeFields(sb, node, false);
        sb.append("\n");
        
        sb.append("  }");
        return sb.toString();
    }

    /**
     * Convert AST nodes to JSON
     */
    private static String nodesToJson(List<ASTNode> nodes) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        for (int i = 0; i < nodes.size(); i++) {
            ASTNode node = nodes.get(i);
            String nodeJson;
            
            switch (node) {
                case RecipeDef recipeDef 
                    -> nodeJson = recipeDefToJson(recipeDef);
                case DeconstructRecipeDef deconstructRecipeDef 
                    -> nodeJson = deconstructRecipeDefToJson(deconstructRecipeDef);
                default 
                    -> {
                    continue;
                }
            }
            
            json.append(nodeJson);
            if (i < nodes.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("]");
        return json.toString();
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
            System.err.println("  [input_po_file>]     - Path to the .po file containing translations");
            System.err.println("  <output_file>       - Path to the output JSON file");
            return;
        }

        String inputPath = args[0];
        String poFilePath = args.length > 2 ? args[1] : null;
        String outputPath = args.length > 2 ? args[2] : args[1];

        try {
            // Read file content from the given path (supports relative paths)
            Path path = resolvePath(inputPath);
            Path poPath = poFilePath != null ? resolvePath(poFilePath) : null;

            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            HashMap<String, POEntry> poEntries = poPath != null ? POFileParser.parseFile(poPath) : new HashMap<>();
            
            List<ASTNode> nodes = parse(content, poEntries);
            
            // Output as JSON to file
            String json = nodesToJson(nodes);
            writeToFile(json, outputPath);
            
            System.out.println("Successfully wrote parsed recipes to: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("Failed to read/write file: " + e.getMessage());
        } catch (ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
        }
    }
}
