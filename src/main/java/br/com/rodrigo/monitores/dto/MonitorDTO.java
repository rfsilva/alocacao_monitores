package br.com.rodrigo.monitores.dto;

import lombok.*;

import java.io.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MonitorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nome;
    private String email;
    private String grupo;
}
