package br.com.rodrigo.monitores.dto;

import lombok.*;

import java.io.*;
import java.time.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TurnoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate dia;
    private LocalTime inicio;
    private LocalTime fim;
}
