package br.com.rodrigo.monitores.dto;

import br.com.rodrigo.monitores.model.*;

import java.util.*;
import java.util.stream.*;

public final class AlocacaoMapper {

    private AlocacaoMapper() {
    }

    public static List<AlocacaoDTO> toDtoList(List<AlocacaoVO> inputList) {
        return inputList.stream().map(c -> toDto(c)).collect(Collectors.toList());
    }

    private static AlocacaoDTO toDto(AlocacaoVO input) {
        return AlocacaoDTO.builder()
                .sala(input.getSala().getNome())
                .turno(toDto(input.getTurno()))
                .eventos(input.getEventos().stream().map(e -> toDto(e)).collect(Collectors.toList()))
                .monitores(input.getMonitores().stream().map(m -> toDto(m)).collect(Collectors.toList()))
                .build();
    }

    private static TurnoDTO toDto(TurnoVO input) {
        return TurnoDTO.builder()
                .inicio(input.getInicio())
                .fim(input.getFim())
                .dia(input.getDia())
                .build();
    }

    private static EventoDTO toDto(Evento input) {
        return EventoDTO.builder()
                .codigo(input.getNome())
                .sala(input.getSala().getNome())
                .grupo(input.getGrupo().getNome())
                .totalMonitores(input.getTotalMonitores())
                .build();
    }

    private static MonitorDTO toDto(CandidatoVO input) {
        return MonitorDTO.builder()
                .email(input.getEmail1())
                .nome(input.getNome())
                .grupo(input.getGrupo().getNome())
                .build();
    }
}
