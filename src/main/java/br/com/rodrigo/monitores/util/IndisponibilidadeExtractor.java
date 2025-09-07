package br.com.rodrigo.monitores.util;

import java.util.*;
import java.util.regex.*;

public class IndisponibilidadeExtractor {

    private static final Map<String, String> turnos = Map.of(
            "manhã", "08:00-12:00",
            "tarde", "14:00-18:00",
            "noite", "19:00-22:00"
    );

    private static final Map<String, String> diasSemana = Map.of(
            "quinta", "11/09",
            "sexta", "12/09",
            "sábado", "13/09",
            "sabado", "13/09",
            "domingo", "14/09"
    );

    public static List<String> extrairIndisponibilidades(String texto) {
        List<String> indisponibilidades = new ArrayList<>();

        // Normalizar
        String norm = texto.toLowerCase()
                .replace("às", "")
                .replace("as", "")
                .replace("–", "-");

        // 1. Detectar datas numéricas (dia 11, 12/09, etc.)
        Pattern pDia = Pattern.compile("(\\d{1,2})(?:/\\d{1,2})?");
        Matcher mDia = pDia.matcher(norm);

        List<String> dias = new ArrayList<>();
        while (mDia.find()) {
            dias.add(mDia.group(1) + "/09"); // força setembro
        }

        // 2. Detectar dias da semana
        for (Map.Entry<String, String> entry : diasSemana.entrySet()) {
            if (norm.contains(entry.getKey())) {
                dias.add(entry.getValue());
            }
        }

        // 3. Detectar turnos normais
        boolean achouTurno = false;
        for (Map.Entry<String, String> entry : turnos.entrySet()) {
            if (norm.contains(entry.getKey())) {
                achouTurno = true;
                for (String d : dias) {
                    indisponibilidades.add("Dia " + d + " - " + entry.getKey());
                }
            }
        }

        // 3.1 Detectar "qualquer turno" e expandir para manhã/tarde/noite
        if (norm.contains("qualquer turno")) {
            achouTurno = true;
            for (String d : dias) {
                for (String t : turnos.keySet()) {
                    indisponibilidades.add("Dia " + d + " - " + t);
                }
            }
        }

        // 4. Detectar "até Xh" ou "a partir de Xh"
        Pattern pHora = Pattern.compile("(até|a partir de)\\s*(\\d{1,2})h");
        Matcher mHora = pHora.matcher(norm);
        while (mHora.find()) {
            achouTurno = true;
            String tipo = mHora.group(1);
            String hora = mHora.group(2);
            for (String d : dias) {
                if (tipo.equals("até")) {
                    indisponibilidades.add("Dia " + d + " - 08:00-" + hora + ":00");
                } else {
                    indisponibilidades.add("Dia " + d + " - " + hora + ":00-22:00");
                }
            }
        }

        // 5. Se tiver dia mas não turno explícito → marcar como "turno indefinido"
        if (!dias.isEmpty() && !achouTurno) {
            for (String d : dias) {
                indisponibilidades.add("Dia " + d + " - turno não especificado");
            }
        }

        return indisponibilidades;
    }

    public static void main(String[] args) {
        String[] exemplos = {
                "Indisponível no dia 11/09 pela manhã e no dia 12/09 pela tarde.",
                "Não tenho disponibilidade quinta pela manhã - aula online à tarde e início da noite - Aula online do Doutorado.",
                "11 e 12 dispneia até a 16h pq tenho que buscar meu bb na creche. 13 e 14 disponível até as 13h pq não tendo com quem deixar meu bb.",
                "Disponibilidade para quinta, sexta e sábado (qualquer turno) - indisponível no domingo, pois a passagem de volta já foi comprada.",
                "Dia 14/09 eu não estarei porque eu tenho um casamento no qual eu vou ser madrinha no Rio e ele é de manhã, por isso sairei de ônibus no sábado dia 13/09 às 19",
                "Estarei disponível dias 12, 13 e 14 no período da tarde. Motivo: trabalho na parte da manhã"
        };

        for (String exemplo : exemplos) {
            System.out.println("Entrada: " + exemplo);
            List<String> res = extrairIndisponibilidades(exemplo);
            res.forEach(System.out::println);
            System.out.println("-----");
        }
    }
}
