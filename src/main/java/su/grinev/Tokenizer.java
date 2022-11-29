package su.grinev;

import su.grinev.tokenizer.Parser;
import su.grinev.tokenizer.NumericParser;
import su.grinev.tokenizer.StringParser;

import java.util.*;
import java.util.stream.Collectors;

public class Tokenizer {

    private static final int CURLY_BRACKET_OPEN = 0;
    private static final int KEY = 1;
    private static final int COLON = 2;
    private static final int VALUE = 3;
    private static final int COMMA = 4;
    private static final int SQUARE_BRACKET_OPEN = 5;

    private final Map<Character, Parser> tokenizerDelegationMap;
    private final SeekableList<Character> seekableList;
    private final List<Token> tokens;
    private final List<ParserContext> stack;

    private int position = 0;

    public Tokenizer(String json) {
        this.tokenizerDelegationMap = new HashMap<>();
        this.stack = new LinkedList<>();
        this.tokens = new ArrayList<>();
        this.seekableList = new SeekableList<>(json.chars()
                .filter(c -> c != '\n' && c != '\r' && c != '\t')
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList()));
        tokenizerDelegationMap.put(' ', () -> new Token(TokenType.WHITESPACE, " "));
        tokenizerDelegationMap.put(',', () -> new Token(TokenType.COMMA, ","));
        tokenizerDelegationMap.put(':', () -> new Token(TokenType.COLON, ":"));
        tokenizerDelegationMap.put('{', () -> new Token(TokenType.CURLY_BRACKET_OPEN, "{"));
        tokenizerDelegationMap.put('}', () -> new Token(TokenType.CURLY_BRACKET_CLOSE, "}"));
        tokenizerDelegationMap.put('[', () -> new Token(TokenType.SQUARE_BRACKET_OPEN, "["));
        tokenizerDelegationMap.put(']', () -> new Token(TokenType.SQUARE_BRACKET_CLOSE, "]"));
        tokenizerDelegationMap.put('n', () -> { seekableList.seek(3); return new Token(TokenType.NULL, "null"); });
        tokenizerDelegationMap.put('t', () -> { seekableList.seek(3); return new Token(TokenType.TRUE, Boolean.TRUE); });
        tokenizerDelegationMap.put('f', () -> { seekableList.seek(4); return new Token(TokenType.FALSE, Boolean.FALSE); });
        tokenizerDelegationMap.put('"', new StringParser(this.seekableList));
        Parser numericTokenizer = new NumericParser(this.seekableList);
        tokenizerDelegationMap.put('-', numericTokenizer);
        tokenizerDelegationMap.put('+', numericTokenizer);
        for (int i = 0; i != 10; i++) { tokenizerDelegationMap.put((char) ((char)i + 0x30), numericTokenizer); }
    }

    public void tokenize() {
        Character c;
        while (!seekableList.isEnd()) {
            c = seekableList.next();
            Token token = tokenizerDelegationMap.computeIfAbsent(c, k -> { throw new IllegalArgumentException("Can't parse \"" + k + "\" at pos " + seekableList.atPos());})
                    .tokenize();
            if (token.getType() == TokenType.WHITESPACE) continue;
            tokens.add(token);
        }
        System.out.println("Token list size: " + tokens.size());
    }
    
    public Map<String, Object> parseJsonObject() {
        Map<String, Object> rootJsonObject = new LinkedHashMap<>();
        List<Object> rootJsonArray = new ArrayList<>();
        stack.clear();
        stack.add(new ParserContext(0, rootJsonObject, rootJsonArray));
        while (!stack.isEmpty()) {
            ParserContext parserContext = stack.remove(0);
            position = parserContext.getPosition();
            Map<String, Object> jsonObject = parserContext.getJsonObject();
            List<Object> jsonArray = parserContext.getJsonArray();

            Token token = tokens.get(position);
            int state;
            if (token.getType() == TokenType.CURLY_BRACKET_OPEN) {
                String key = null;
                Object value = null;
                state = CURLY_BRACKET_OPEN;
                do {
                    token = tokens.get(position++);
                    if (token.getType() == TokenType.CURLY_BRACKET_OPEN && state == CURLY_BRACKET_OPEN) {
                        state = KEY;
                        continue;
                    }
                    if (token.getType() == TokenType.STRING && state == KEY) {
                        key = (String) token.getValue();
                        state = COLON;
                        continue;
                    }
                    if (token.getType() == TokenType.COLON && state == COLON) {
                        state = VALUE;
                        continue;
                    }
                    if (state == VALUE) {
                        if (token.getType() == TokenType.CURLY_BRACKET_OPEN) {
                            ParserContext nestedParserContext = new ParserContext(--position, new LinkedHashMap<>(), null);
                            stack.add(nestedParserContext);
                            value = nestedParserContext.getJsonObject();
                            fastForward(TokenType.CURLY_BRACKET_CLOSE);
                        } else {
                            if (token.getType() == TokenType.SQUARE_BRACKET_OPEN) {
                                ParserContext nestedParserContext = new ParserContext(--position, null, new ArrayList<>());
                                stack.add(nestedParserContext);
                                value = nestedParserContext.getJsonArray();
                                fastForward(TokenType.SQUARE_BRACKET_CLOSE);
                            } else {
                                value = token.getValue();
                            }
                        }
                        state = COMMA;
                        continue;
                    }
                    if ((token.getType() == TokenType.COMMA || token.getType() == TokenType.CURLY_BRACKET_CLOSE) && state == COMMA) {
                        if  (key != null && value != null) {
                            jsonObject.put(key, value);
                            key = null;
                            value = null;
                        }
                        state = KEY;
                    }
                } while (token.getType() != TokenType.CURLY_BRACKET_CLOSE);
            }

            if (token.getType() == TokenType.SQUARE_BRACKET_OPEN) {
                state = SQUARE_BRACKET_OPEN;
                Object value;
                do {
                    token = tokens.get(position++);
                    if (token.getType() == TokenType.SQUARE_BRACKET_OPEN && state == SQUARE_BRACKET_OPEN) {
                        state = VALUE;
                        continue;
                    }
                    if (state == VALUE) {
                        if (token.getType() == TokenType.CURLY_BRACKET_CLOSE) {
                            break;
                        }
                        if (token.getType() == TokenType.CURLY_BRACKET_OPEN) {
                            ParserContext nestedParserContext = new ParserContext(--position, new LinkedHashMap<>(), null);
                            stack.add(nestedParserContext);
                            value = nestedParserContext.getJsonObject();
                            fastForward(TokenType.CURLY_BRACKET_CLOSE);
                        } else
                        if (token.getType() == TokenType.SQUARE_BRACKET_OPEN) {
                            ParserContext nestedParserContext = new ParserContext(--position, null, new ArrayList<>());
                            stack.add(nestedParserContext);
                            value = nestedParserContext.getJsonArray();
                            fastForward(TokenType.SQUARE_BRACKET_CLOSE);
                        } else {
                            value = token.getValue();
                        }
                        jsonArray.add(value);
                        state = COMMA;
                        continue;
                    }
                    if (token.getType() == TokenType.COMMA && state == COMMA) {
                        state = VALUE;
                    }
                } while (token.getType() != TokenType.SQUARE_BRACKET_CLOSE);
            }
        }
        if (!rootJsonArray.isEmpty()) {
            rootJsonObject.put("array", rootJsonArray);
        }
        return rootJsonObject;
    }

    private void fastForward(TokenType type) {
        int levelObject = 0;
        int levelArray = 0;
        while (true) {
            Token token = tokens.get(position++);
            if (token.getType() == TokenType.CURLY_BRACKET_OPEN) { levelObject++; }
            if (token.getType() == TokenType.CURLY_BRACKET_CLOSE) { levelObject--; }
            if (token.getType() == TokenType.SQUARE_BRACKET_OPEN) { levelArray++; }
            if (token.getType() == TokenType.SQUARE_BRACKET_CLOSE) { levelArray--; }
            if ((token.getType() == TokenType.CURLY_BRACKET_CLOSE && type == TokenType.CURLY_BRACKET_CLOSE && levelObject == 0)
                    || (token.getType() == TokenType.SQUARE_BRACKET_CLOSE && type == TokenType.SQUARE_BRACKET_CLOSE && levelArray == 0)) {
                return;
            }
        }
    }
}