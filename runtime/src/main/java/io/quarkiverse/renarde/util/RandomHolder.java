package io.quarkiverse.renarde.util;

import java.security.SecureRandom;

/**
 * This class exists solely to get rid of a runtime-init issue when compiling natively
 */
public class RandomHolder {
    static final SecureRandom SECURE_RANDOM = new SecureRandom();
}
