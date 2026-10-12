package com.futmanager.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsServiceTest {

    private final StatisticsService statisticsService = new StatisticsService(null, null, null, null, null, null, null);

    @Test
    @DisplayName("Deve calcular percentual de frequência corretamente com arredondamento seguro")
    void testCalculatePercentageSuccess() {
        // 8 presenças em 10 aulas = 80.0%
        Double result = statisticsService.calculatePercentage(8, 10);
        assertNotNull(result);
        assertEquals(80.0, result);

        // 2 presenças em 3 aulas = 66.7%
        Double result2 = statisticsService.calculatePercentage(2, 3);
        assertNotNull(result2);
        assertEquals(66.7, result2);
    }

    @Test
    @DisplayName("Deve retornar null quando houver 0 aulas contabilizadas, evitando divisão por zero")
    void testCalculatePercentageZeroDivision() {
        Double result = statisticsService.calculatePercentage(0, 0);
        assertNull(result, "Deve retornar null para representar 'Sem dados' em vez de gerar Infinity ou NaN");
    }

    @Test
    @DisplayName("Regra consolidada não pode ser média simples de percentuais de turmas com quantidades diferentes de aulas")
    void testConsolidatedRuleVsSimpleAverage() {
        // Turma A: 1 aula, 1 presença -> 100%
        // Turma B: 9 aulas, 0 presenças -> 0%
        // Média simples das % seria: (100 + 0) / 2 = 50% (ERRADO)
        // Regra correta consolidada: (1 + 0) / (1 + 9) = 1/10 = 10.0% (CORRETO)
        int presencesA = 1, totalA = 1;
        int presencesB = 0, totalB = 9;

        int sumPresences = presencesA + presencesB;
        int sumTotal = totalA + totalB;

        Double consolidated = statisticsService.calculatePercentage(sumPresences, sumTotal);
        assertEquals(10.0, consolidated);
        assertNotEquals(50.0, consolidated);
    }
}
