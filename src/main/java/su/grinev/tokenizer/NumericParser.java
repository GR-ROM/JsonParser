package su.grinev.tokenizer;

import su.grinev.SeekableList;
import su.grinev.Token;
import su.grinev.TokenType;

public class NumericParser implements Parser {

    private StringBuilder stringBuilder;
    private final SeekableList<Character> seekableList;
    private char c;

    public NumericParser(SeekableList<Character> seekableList) {
        this.seekableList = seekableList;
    }

    @Override
    public Token tokenize() {
        stringBuilder = new StringBuilder();
        boolean hasFraction = false;
        boolean hasNatural = false;
        prev();
        while (true) {
            get();
            if (Character.isDigit(c) || c == '-' || c == '+') {
                stringBuilder.append(next());
                parseDigits();
                hasNatural = true;
                continue;
            }
            if (c == '.' && !hasFraction) {
                hasFraction = true;
                stringBuilder.append(next());
                parseDigits();
                continue;
            }
            if (c == 'e' || c == 'E') {
                stringBuilder.append(next());
                if (c == '-' || c == '+') {
                    stringBuilder.append(c);
                }
                parseDigits();
                continue;
            }
            break;
        }
        if (hasFraction) {
            return new Token(TokenType.NUMBER, Float.parseFloat(stringBuilder.toString()));
        }
        if (hasNatural) {
            return new Token(TokenType.NUMBER, Long.parseLong(stringBuilder.toString()));
        }
        throw new IllegalArgumentException("Can't parse number at " + seekableList.atPos());
    }

    private void parseDigits() {
        while (true) {
            if (Character.isDigit(next())) {
                stringBuilder.append(c);
            } else {
                prev();
                return;
            }
        }
    }

    private Character next() {
        c = seekableList.next();
        return c;
    }

    private Character prev() {
        c = seekableList.prev();
        return c;
    }

    private Character get() {
        c = seekableList.get();
        return c;
    }
}
