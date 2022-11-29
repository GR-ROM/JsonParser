package su.grinev.tokenizer;

import su.grinev.Token;

@FunctionalInterface
public interface Parser {

    Token tokenize();
}
