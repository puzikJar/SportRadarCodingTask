package com.sportradar.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Team")
class TeamTest {

    @Test
    @DisplayName("trims surrounding whitespace and keeps the caller's casing")
    void trimsWhitespaceKeepingCasing() {
        assertThat(new Team("  Spain ").name()).isEqualTo("Spain");
    }

    @Test
    @DisplayName("is equal to another name that differs only in case or surrounding whitespace")
    void equalsIgnoringCaseAndWhitespace() {
        var spain = new Team("Spain");
        var shouted = new Team(" SPAIN ");

        assertThat(spain).isEqualTo(shouted).hasSameHashCodeAs(shouted);
        assertThat(spain).isNotEqualTo(new Team("Brazil"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("rejects a null, empty or blank name")
    void rejectsMissingOrBlankName(String invalidName) {
        assertThatThrownBy(() -> new Team(invalidName))
                .isInstanceOf(IllegalArgumentException.class);
    }
}