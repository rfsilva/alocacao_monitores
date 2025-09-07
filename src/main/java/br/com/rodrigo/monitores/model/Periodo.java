package br.com.rodrigo.monitores.model;

public enum Periodo {
        MANHA(0, 12, "Manhã"),
        TARDE(12, 18, "Tarde"),
        NOITE(18, 24, "Noite");

        private int inicio, fim;
        private String nome;

        Periodo(int inicio, int fim, String nome) {
            this.inicio = inicio;
            this.fim = fim;
            this.nome = nome;
        }

        public int getInicio() {
            return inicio;
        }

        public int getFim() {
            return fim;
        }

        @Override
        public String toString() {
            return nome;
        }
    }