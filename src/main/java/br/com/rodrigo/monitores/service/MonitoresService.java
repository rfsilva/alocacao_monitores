package br.com.rodrigo.monitores.service;

import br.com.rodrigo.monitores.dto.*;
import br.com.rodrigo.monitores.model.*;

import java.util.*;

public interface MonitoresService {
    List<AlocacaoDTO> distribuir(Boolean considerarGrupo);
}
