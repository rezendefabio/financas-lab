package com.laboratorio.financas.lancamentorecorrente.domain;

import java.time.LocalDate;

public enum Periodicidade {
    SEMANAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) {
            return atual.plusWeeks(1);
        }
    },
    QUINZENAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) {
            return atual.plusWeeks(2);
        }
    },
    MENSAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) {
            return atual.plusMonths(1);
        }
    },
    BIMESTRAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) {
            return atual.plusMonths(2);
        }
    },
    TRIMESTRAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) {
            return atual.plusMonths(3);
        }
    },
    SEMESTRAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) {
            return atual.plusMonths(6);
        }
    },
    ANUAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) {
            return atual.plusYears(1);
        }
    };

    public abstract LocalDate calcularProxima(LocalDate atual);
}
