package br.com.rodrigo.monitores.dto;

import lombok.*;

import java.io.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codigo;
    private String grupo;
    private String sala;
    private Integer totalMonitores;
}
