package br.com.rodrigo.monitores.util;

import br.com.rodrigo.monitores.model.*;
import org.apache.poi.ss.usermodel.*;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

public final class ContentUtil {

    private ContentUtil() {
    }

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Formatos possíveis:
     *  XX:XX - XX:XX
     *  XX:XX - XX:XX / XX:XX - XX:XX
     *  XX:XX as XX:XX
     * @param data
     * @param periodos
     * @return
     */
    public static List<TurnoVO> obterTurnos(LocalDate data, String periodos) {

        // Regex: captura pares de horários
        Pattern pattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*(?:-|as|às|até)\\s*(\\d{1,2}:\\d{2})");

        Matcher matcher = pattern.matcher(periodos);
        List<TurnoVO> turnos = new ArrayList<>();

        while (matcher.find()) {
            LocalTime inicio = LocalTime.parse(matcher.group(1), formatter);
            LocalTime fim = LocalTime.parse(matcher.group(2), formatter);
            TurnoVO turno = TurnoVO.builder().dia(data).inicio(inicio).fim(fim).build();
            turno.setPeriodo(ContentUtil.classificarPeriodo(turno));
            turnos.add(turno);
        }

        return turnos;
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static List<TurnoVO> obterTurnos(String periodos) {
        List<TurnoVO> turnos = new ArrayList<>();

        // Regex para capturar "dd/MM HH:mm-HH:mm"
        Pattern pattern = Pattern.compile("(\\d{2}/\\d{2})\\s+(\\d{2}:\\d{2})-(\\d{2}:\\d{2})");
        Matcher matcher = pattern.matcher(periodos);

        while (matcher.find()) {
            String dataStr = matcher.group(1);
            String inicioStr = matcher.group(2);
            String fimStr = matcher.group(3);

            // Parse dia/mês como MonthDay e adiciona o ano fixo
            MonthDay md = MonthDay.parse(dataStr, DATE_FORMAT);
            LocalDate data = md.atYear(2025);

            LocalTime inicio = LocalTime.parse(inicioStr, TIME_FORMAT);
            LocalTime fim = LocalTime.parse(fimStr, TIME_FORMAT);

            TurnoVO turno = TurnoVO.builder()
                    .dia(data)
                    .inicio(inicio)
                    .fim(fim)
                    .build();
            turno.setPeriodo(ContentUtil.classificarPeriodo(turno));
            turnos.add(turno);
        }

        return turnos;
    }

    public static SalaVO obterSala(List<SalaVO> salas, String nomeSala) {
        List<SalaVO> found = salas.stream().filter(s -> s.getNome().equalsIgnoreCase(nomeSala)).toList();
        if (found != null && found.size() > 0) {
            return found.get(0);
        }
        SalaVO sala = SalaVO.builder()
                .id(UUID.randomUUID())
                .nome(nomeSala)
                .build();
        salas.add(sala);
        return sala;
    }

    public static GrupoVO obterGrupo(List<GrupoVO> grupos, String nomeGrupo) {
        if (nomeGrupo == null || "".equals(nomeGrupo)) {
            return null;
        }
        List<GrupoVO> found = grupos.stream().filter(s -> s.getNome().equalsIgnoreCase(nomeGrupo)).toList();
        if (found != null && found.size() > 0) {
            return found.get(0);
        }
        GrupoVO grupo = GrupoVO.builder()
                .id(UUID.randomUUID())
                .nome(nomeGrupo)
                .build();
        grupos.add(grupo);
        return grupo;
    }

    public static String extrairCodigo(String nomeAtividade) {
        if (nomeAtividade != null && !"".equals(nomeAtividade)) {
            int idx = nomeAtividade.indexOf('-');
            if (idx < 0) {
                return "";
            }
            return nomeAtividade.substring(0, idx - 1);
        }
        return "";
    }

    public static boolean isLinhaEmBranco(Row row) {
        for (int i = 0; i < 5; i++) {
            if (!estaVazio(getStringValue(row, i))) {
                return false;
            }
        }
        return true;
    }

    public static Boolean estaVazio(String content) {
        return content == null || "".equals(content);
    }

    public static String getStringValue(Row row, Integer index) {
        try {
            String content = row.getCell(index).getStringCellValue();
            if (content != null) {
                content = content.trim();
            }
            return content;
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDateTime getDateTimeValue(Row row, Integer index) {
        try {
            return row.getCell(index).getLocalDateTimeCellValue();
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDate getLocalDateValue(Row row, Integer index) {
        try {
            Date data = row.getCell(index).getDateCellValue();
            return data == null ? null : data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer getIntegerValue(Row row, Integer index) {
        try {
            Double d = row.getCell(index).getNumericCellValue();
            return d.intValue();
        } catch (Exception e) {
            return null;
        }
    }
    public static List<String> extrairEventos(String texto) {
        List<String> eventos = new ArrayList<>();
        Pattern pattern = Pattern.compile("(Minicurso|Oficina)\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(texto);

        while (matcher.find()) {
            String tipo = matcher.group(1);
            String numero = matcher.group(2);

            String abreviado;
            if (tipo.equalsIgnoreCase("Minicurso")) {
                abreviado = "MC " + Integer.parseInt(numero); // tira zero à esquerda
            } else {
                abreviado = "OF " + Integer.parseInt(numero);
            }
            eventos.add(abreviado);
        }

        return eventos;
    }

    public static AtividadeVO obterAtividade(List<AtividadeVO> atividades, String codigoAtividade) {
        List<AtividadeVO> found = atividades.stream().filter(s -> s.getCodigo().equalsIgnoreCase(codigoAtividade)).toList();
        if (found != null && found.size() > 0) {
            return found.get(0);
        }
        return null;
    }

    public static RodaConversaVO obterRodaConversa(List<RodaConversaVO> rodasConversa, String codigoRodaConversa) {
        List<RodaConversaVO> found = rodasConversa.stream().filter(s -> s.getCodigo().equalsIgnoreCase(codigoRodaConversa)).toList();
        if (found != null && found.size() > 0) {
            return found.get(0);
        }
        return null;
    }

    public static List<String> extrairRC(String texto) {
        List<String> rcs = new ArrayList<>();
        Pattern pattern = Pattern.compile("RC\\s*0*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(texto);

        while (matcher.find()) {
            String numero = matcher.group(1);
            String abreviado = "RC " + Integer.parseInt(numero); // normaliza (RC 03 -> RC 3)
            rcs.add(abreviado);
        }

        return rcs;
    }

    public static AlocacaoVO obterAlocacao(List<AlocacaoVO> alocacoes, TurnoVO turno) {
        List<AlocacaoVO> alocacoesPeriodoDia = alocacoes.stream().filter(a ->
                (a.getTurno().getDia().getDayOfMonth() == turno.getDia().getDayOfMonth()
                && a.getTurno().getPeriodo() == turno.getPeriodo())).toList();
        if (alocacoesPeriodoDia != null && alocacoesPeriodoDia.size() > 0) {
            return alocacoesPeriodoDia.get(0);
        }
        return null;
    }

    public static Periodo classificarPeriodo(TurnoVO turno) {
        if ((turno.getInicio().getHour() >= Periodo.MANHA.getInicio())
            && (turno.getInicio().getHour() < Periodo.MANHA.getFim())) {
            return Periodo.MANHA;
        }
        if ((turno.getInicio().getHour() >= Periodo.TARDE.getInicio())
                && (turno.getInicio().getHour() < Periodo.TARDE.getFim())) {
            return Periodo.TARDE;
        }
        return Periodo.NOITE;
    }

    public static Boolean isConcorrente(List<TurnoVO> turnos, TurnoVO turno) {
        for (TurnoVO existente : turnos) {
            boolean colide = (turno.getDia().getDayOfMonth() == existente.getDia().getDayOfMonth()
                    && turno.getInicio().isBefore(existente.getFim())
                    && turno.getFim().isAfter(existente.getInicio()));
            if (colide) {
                return true;
            }
        }
        return false;
    }

    private static final int CARGA_HORARIA_MINIMA_MINUTOS = 8 * 60;

    public static boolean estaComCargaHorariaCompleta(CandidatoVO candidato) {
        int total = candidato.getAlocacoes().stream().mapToInt(a -> a.getTurno().obterTempoMinutos().intValue()).sum();
        return total >= CARGA_HORARIA_MINIMA_MINUTOS;
    }

    public static MonitorAprovadoVO encontrarMonitor(List<MonitorAprovadoVO> monitorList, CandidatoVO candidato) {
        Optional<MonitorAprovadoVO> opt1 = monitorList.stream().filter(m -> m.getEmail().equalsIgnoreCase(candidato.getEmail1())).findFirst();
        if (opt1.isPresent()) {
            return opt1.get();
        }
        Optional<MonitorAprovadoVO> opt2 = monitorList.stream().filter(m -> m.getEmail().equalsIgnoreCase(candidato.getEmail2())).findFirst();
        if (opt2.isPresent()) {
            return opt2.get();
        }
        Optional<MonitorAprovadoVO> opt3 = monitorList.stream().filter(m -> m.getNome().equalsIgnoreCase(candidato.getNome())).findFirst();
        if (opt3.isPresent()) {
            return opt3.get();
        }
        return null;
    }

    public static String toStringList(List<String> content) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (String s : content) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(s);
            i++;
        }
        return builder.toString();
    }
}
