package br.com.rodrigo.monitores.dto;

import lombok.*;

import java.io.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlocacaoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sala;
    private TurnoDTO turno;
    @Builder.Default
    private List<EventoDTO> eventos = new ArrayList<>();
    @Builder.Default
    private List<MonitorDTO> monitores = new ArrayList<>();
}
