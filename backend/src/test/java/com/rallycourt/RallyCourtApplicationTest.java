package com.rallycourt;

import org.junit.jupiter.api.Test;

class RallyCourtApplicationTest {

    @Test
    void mainStartsApplication() {
        String originalPort = System.getProperty("server.port");
        try {
            System.setProperty("server.port", "0");
            RallyCourtApplication.main(new String[0]);
        } finally {
            if (originalPort == null) {
                System.clearProperty("server.port");
            } else {
                System.setProperty("server.port", originalPort);
            }
        }
    }
}
