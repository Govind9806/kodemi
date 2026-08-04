package com.example.ai_service.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotTypeTest {

    @Test
    void valuesShouldContainAllEnums() {

        BotType[] values = BotType.values();

        assertEquals(2, values.length);
        assertEquals(BotType.LEARNER, values[0]);
        assertEquals(BotType.TRAINER, values[1]);
    }

    @Test
    void valueOfShouldWork() {

        assertEquals(
                BotType.LEARNER,
                BotType.valueOf("LEARNER")
        );

        assertEquals(
                BotType.TRAINER,
                BotType.valueOf("TRAINER")
        );
    }

    @Test
    void ordinalShouldBeCorrect() {

        assertEquals(0, BotType.LEARNER.ordinal());
        assertEquals(1, BotType.TRAINER.ordinal());
    }

    @Test
    void toStringShouldMatchName() {

        assertEquals(
                "LEARNER",
                BotType.LEARNER.toString()
        );

        assertEquals(
                "TRAINER",
                BotType.TRAINER.toString()
        );
    }
}