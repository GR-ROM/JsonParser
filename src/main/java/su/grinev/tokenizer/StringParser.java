package su.grinev.tokenizer;

import su.grinev.SeekableList;
import su.grinev.Token;
import su.grinev.TokenType;

import java.util.HashMap;
import java.util.Map;

public class StringParser implements Parser {

    private final SeekableList<Character> seekableList;
    private final Map<Character, Character> escapeCharMap;
    private char c;

    public StringParser(SeekableList<Character> seekableList) {
        this.seekableList = seekableList;
        this.escapeCharMap = new HashMap<>();
        this.escapeCharMap.put('"', '"');
        this.escapeCharMap.put('\\', '\\');
        this.escapeCharMap.put('b', '\b');
        this.escapeCharMap.put('f', '\f');
        this.escapeCharMap.put('n', '\n');
        this.escapeCharMap.put('r', '\r');
        this.escapeCharMap.put('t', '\t');
    }

    @Override
    public Token tokenize() {
        StringBuilder stringBuilder = new StringBuilder();
        while (next() != '\"') {
            if (c == '\\') {
                if (next() == '/') {
                    if (next() == 'u') {
                        stringBuilder.append((char)Integer.parseInt(next() + next() + next() + next() + "", 16));
                    } else {
                        stringBuilder.append(escapeCharMap.computeIfAbsent(c, k -> { throw new IllegalArgumentException("Can't parse unknown escape sequence"); }));
                    }
                }
            }
            stringBuilder.append(c);
        }
        return new Token(TokenType.STRING, stringBuilder.toString());
    }

    private Character next() {
        c = seekableList.next();
        return c;
    }
}
