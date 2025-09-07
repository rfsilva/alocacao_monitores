package br.com.rodrigo.monitores.model.comparator;

import br.com.rodrigo.monitores.model.*;

import java.util.*;

public class AlocacaoComparator implements Comparator<AlocacaoVO> {
    @Override
    public int compare(AlocacaoVO o1, AlocacaoVO o2) {
        return o1.getTurno().compareTo(o2.getTurno());
    }
}
